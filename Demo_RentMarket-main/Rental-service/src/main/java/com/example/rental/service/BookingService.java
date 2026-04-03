package com.example.rental.service;

import com.example.rental.config.JwtUtils;
import com.example.rental.dto.request.CreateBookingRequest;
import com.example.rental.dto.response.*;
import com.example.rental.entity.*;
import com.example.rental.exception.AppException;
import com.example.rental.exception.ErrorCode;
import com.example.rental.mapper.BookingMapper;
import com.example.rental.repository.BookingRepository;
import com.example.rental.repository.WalletRepository;
import com.example.rental.repository.WalletTransactionRepository;
import com.example.rental.util.PriceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final BookingMapper bookingMapper;
    private final RestTemplate restTemplate;
    private final JwtUtils jwtUtils;

    @Value("${app.product-service.url}")
    private String productServiceUrl;

    @Value("${app.identity-service.url}")
    private String identityServiceUrl;

    // =========================================================================
    // TẠO BOOKING
    // =========================================================================

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        String tenantId = jwtUtils.getCurrentUsername();
        int requestedQty = request.getQuantity() != null ? request.getQuantity() : 1;

        validateDates(request.getStartDate(), request.getEndDate());

        ProductData product = fetchProductData(request.getProductId());

        if (tenantId.equals(product.ownerId())) {
            throw new AppException(ErrorCode.CANNOT_RENT_OWN_PRODUCT);
        }

        if (!"AVAILABLE".equalsIgnoreCase(product.status())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }

        int bookedQty = bookingRepository.sumApprovedQuantity(
                request.getProductId(), request.getStartDate(), request.getEndDate());
        int availableQty = product.quantity() - bookedQty;

        if (requestedQty > availableQty) {
            throw new AppException(ErrorCode.INSUFFICIENT_QUANTITY);
        }

        PriceCalculator.PriceBreakdown price = PriceCalculator.calculate(
                request.getStartDate(), request.getEndDate(),
                product.pricePerDay(), requestedQty);

        double depositFee = price.rentalFee() * 0.5;

        Booking booking = Booking.builder()
                .tenantId(tenantId)
                .productId(request.getProductId())
                .productOwnerId(product.ownerId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .pricePerDay(price.pricePerDay())
                .rentalDays(price.rentalDays())
                .quantity(requestedQty)
                .rentalFee(price.rentalFee())
                .depositFee(depositFee)
                .note(request.getNote())
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        return bookingMapper.toBookingResponse(bookingRepository.save(booking));
    }

    // =========================================================================
    // CHỦ ĐỒ — CHẤP NHẬN
    // =========================================================================

    @Transactional
    public BookingResponse acceptBooking(Long bookingId) {
        String currentUser = jwtUtils.getCurrentUsername();
        Booking booking = findBookingById(bookingId);

        requireOwner(booking, currentUser);
        requireStatus(booking, BookingStatus.PAID_WAITING_APPROVAL, ErrorCode.INVALID_BOOKING_STATUS);

        ProductData product = fetchProductData(booking.getProductId());
        int currentBooked = bookingRepository.sumApprovedQuantity(
                booking.getProductId(), booking.getStartDate(), booking.getEndDate());
        int availableQty = product.quantity() - currentBooked;

        if (booking.getQuantity() > availableQty) {
            throw new AppException(ErrorCode.INSUFFICIENT_QUANTITY);
        }

        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);

        int newBooked = bookingRepository.sumApprovedQuantity(
                booking.getProductId(), booking.getStartDate(), booking.getEndDate());
        if (newBooked >= product.quantity()) {
            autoRejectOverlappingBookings(booking);
        }

        return bookingMapper.toBookingResponse(booking);
    }

    // =========================================================================
    // NGƯỜI THUÊ — BẮT ĐẦU THUÊ (XÁC NHẬN GIAO NHẬN)
    // =========================================================================

    @Transactional
    public BookingResponse confirmHandover(Long bookingId) {
        String currentUser = jwtUtils.getCurrentUsername();
        Booking booking = findBookingById(bookingId);

        if (!currentUser.equals(booking.getTenantId()) && !currentUser.equals(booking.getProductOwnerId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        requireStatus(booking, BookingStatus.APPROVED, ErrorCode.INVALID_BOOKING_STATUS);

        booking.setStatus(BookingStatus.IN_PROGRESS);
        return bookingMapper.toBookingResponse(bookingRepository.save(booking));
    }

    // =========================================================================
    // CHỦ ĐỒ — TỪ CHỐI
    // =========================================================================

    @Transactional
    public BookingResponse rejectBooking(Long bookingId, String reason) {
        String currentUser = jwtUtils.getCurrentUsername();
        Booking booking = findBookingById(bookingId);

        requireOwner(booking, currentUser);

        boolean needRefund = booking.getStatus() == BookingStatus.PAID_WAITING_APPROVAL;
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT && !needRefund) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }

        booking.setRejectionReason(reason);
        booking.setStatus(BookingStatus.REJECTED);
        bookingRepository.save(booking);

        if (needRefund) {
            refundTenant(booking);
        }
        return bookingMapper.toBookingResponse(booking);
    }

    // =========================================================================
    // CHỦ ĐỒ — HOÀN TẤT & QUYẾT TOÁN
    // =========================================================================

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse completeBooking(Long bookingId) {
        String currentUser = jwtUtils.getCurrentUsername();
        Booking booking = findBookingById(bookingId);

        requireOwner(booking, currentUser);
        requireStatus(booking, BookingStatus.IN_PROGRESS, ErrorCode.INVALID_BOOKING_STATUS);

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        Wallet tenantWallet = walletRepository.findByUserId(booking.getTenantId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        Wallet ownerWallet = walletRepository.findByUserId(booking.getProductOwnerId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        Wallet adminWallet = walletRepository.findByUserId("ADMIN_SYSTEM")
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .userId("ADMIN_SYSTEM").availableBalance(0.0).frozenBalance(0.0).build()));

        double totalFrozen = booking.getRentalFee() + (booking.getDepositFee() != null ? booking.getDepositFee() : 0.0);

        if (tenantWallet.getFrozenBalance() < totalFrozen) {
            throw new AppException(ErrorCode.INSUFFICIENT_QUANTITY);
        }

        tenantWallet.setFrozenBalance(tenantWallet.getFrozenBalance() - totalFrozen);

        if (booking.getDepositFee() != null && booking.getDepositFee() > 0) {
            tenantWallet.setAvailableBalance(tenantWallet.getAvailableBalance() + booking.getDepositFee());
            logTransaction(tenantWallet, booking.getDepositFee(), TransactionType.REFUND, bookingId, "Hoàn trả tiền cọc");
        }

        double platformFee = booking.getRentalFee() * 0.30;
        double ownerEarning = booking.getRentalFee() - platformFee;

        ownerWallet.setAvailableBalance(ownerWallet.getAvailableBalance() + ownerEarning);
        logTransaction(ownerWallet, ownerEarning, TransactionType.EARNING, bookingId, "Doanh thu cho thuê 70%");

        adminWallet.setAvailableBalance(adminWallet.getAvailableBalance() + platformFee);
        logTransaction(adminWallet, platformFee, TransactionType.PLATFORM_FEE, bookingId, "Phí nền tảng 30%");

        walletRepository.saveAll(List.of(tenantWallet, ownerWallet, adminWallet));

        return bookingMapper.toBookingResponse(booking);
    }

    // =========================================================================
    // NGƯỜI THUÊ — HUỶ
    // =========================================================================

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, String reason) {
        String currentUser = jwtUtils.getCurrentUsername();
        Booking booking = findBookingById(bookingId);

        requireTenant(booking, currentUser);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                && booking.getStatus() != BookingStatus.PAID_WAITING_APPROVAL) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }

        boolean needRefund = booking.getStatus() == BookingStatus.PAID_WAITING_APPROVAL;
        booking.setStatus(BookingStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            booking.setCancellationReason(reason);
        }
        bookingRepository.save(booking);

        if (needRefund) {
            refundTenant(booking);
        }

        return bookingMapper.toBookingResponse(booking);
    }

    // =========================================================================
    // WALLET UTILS
    // =========================================================================

    private void refundTenant(Booking booking) {
        Wallet tenantWallet = walletRepository.findByUserId(booking.getTenantId()).orElseThrow();
        double totalFrozen = booking.getRentalFee() + (booking.getDepositFee() != null ? booking.getDepositFee() : 0.0);
        tenantWallet.setFrozenBalance(tenantWallet.getFrozenBalance() - totalFrozen);
        tenantWallet.setAvailableBalance(tenantWallet.getAvailableBalance() + totalFrozen);
        walletRepository.save(tenantWallet);
        logTransaction(tenantWallet, totalFrozen, TransactionType.REFUND, booking.getId(), "Hoàn tiền do đơn hàng bị huỷ");
    }

    private void logTransaction(Wallet wallet, Double amount, TransactionType type, Long bookingId, String desc) {
        WalletTransaction t = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(type)
                .bookingId(bookingId)
                .description(desc)
                .build();
        walletTransactionRepository.save(t);
    }

    // =========================================================================
    // KIỂM TRA KHẢ DỤNG
    // =========================================================================

    public AvailabilityResponse checkAvailability(Long productId, LocalDate startDate, LocalDate endDate) {
        ProductData product = fetchProductData(productId);
        int totalQty = product.quantity();

        int bookedQty = bookingRepository.sumApprovedQuantity(productId, startDate, endDate);
        int availableQty = Math.max(0, totalQty - bookedQty);

        LocalDate nextAvailable = null;
        if (availableQty == 0) {
            nextAvailable = bookingRepository.findEarliestAvailableDate(productId, LocalDate.now());
        }

        return AvailabilityResponse.builder()
                .totalQuantity(totalQty)
                .bookedQuantity(bookedQty)
                .availableQuantity(availableQty)
                .nextAvailableDate(nextAvailable)
                .build();
    }

    // =========================================================================
    // TRUY VẤN — Bất đồng bộ enrich
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyRentals(
            BookingStatus status, LocalDate fromDate, LocalDate toDate,
            int page, int size) {
        String tenantId = jwtUtils.getCurrentUsername();
        Pageable pageable = PageRequest.of(page, size);

        Page<BookingResponse> responsePage = bookingRepository
                .findMyRentals(tenantId, status, fromDate, toDate, pageable)
                .map(bookingMapper::toBookingResponse);

        List<CompletableFuture<Void>> futures = responsePage.getContent().stream()
                .map(response -> CompletableFuture.runAsync(() ->
                        response.setProductInfo(fetchProductInfo(response.getProductId()))))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return responsePage;
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getOwnerBookings(
            BookingStatus status, Long productId, LocalDate fromDate, LocalDate toDate,
            int page, int size) {
        String ownerId = jwtUtils.getCurrentUsername();
        Pageable pageable = PageRequest.of(page, size);

        Page<BookingResponse> responsePage = bookingRepository
                .findOwnerBookings(ownerId, status, productId, fromDate, toDate, pageable)
                .map(bookingMapper::toBookingResponse);

        List<CompletableFuture<Void>> futures = responsePage.getContent().stream()
                .map(response -> {
                    CompletableFuture<Void> productFuture = CompletableFuture.runAsync(() ->
                            response.setProductInfo(fetchProductInfo(response.getProductId())));
                    CompletableFuture<Void> tenantFuture = CompletableFuture.runAsync(() ->
                            response.setTenantInfo(fetchTenantInfo(response.getTenantId())));
                    return CompletableFuture.allOf(productFuture, tenantFuture);
                })
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return responsePage;
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        String currentUser = jwtUtils.getCurrentUsername();
        Booking booking = findBookingById(bookingId);

        if (!currentUser.equals(booking.getTenantId())
                && !currentUser.equals(booking.getProductOwnerId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        BookingResponse response = bookingMapper.toBookingResponse(booking);

        // Luôn fetch productInfo cho cả 2 phía
        CompletableFuture<Void> productFuture = CompletableFuture.runAsync(() ->
                response.setProductInfo(fetchProductInfo(booking.getProductId())));

        // Luôn fetch ownerInfo để người thuê có thể liên hệ chủ đồ
        CompletableFuture<Void> ownerFuture = CompletableFuture.runAsync(() ->
                response.setOwnerInfo(fetchOwnerInfo(booking.getProductOwnerId())));

        // Chỉ fetch tenantInfo khi chủ đồ xem (để biết người thuê là ai)
        CompletableFuture<Void> tenantFuture = currentUser.equals(booking.getProductOwnerId())
                ? CompletableFuture.runAsync(() -> response.setTenantInfo(fetchTenantInfo(booking.getTenantId())))
                : CompletableFuture.completedFuture(null);

        CompletableFuture.allOf(productFuture, ownerFuture, tenantFuture).join();

        return response;
    }

    // =========================================================================
    // PRIVATE — Guard checks
    // =========================================================================

    private Booking findBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new AppException(ErrorCode.INVALID_DATE);
        }
    }

    private void requireOwner(Booking booking, String currentUser) {
        if (!currentUser.equals(booking.getProductOwnerId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    private void requireTenant(Booking booking, String currentUser) {
        if (!currentUser.equals(booking.getTenantId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    private void requireStatus(Booking booking, BookingStatus required, ErrorCode errorCode) {
        if (booking.getStatus() != required) {
            throw new AppException(errorCode);
        }
    }

    private void autoRejectOverlappingBookings(Booking approvedBooking) {
        List<Booking> overlapping = bookingRepository.findOverlappingPendingBookings(
                approvedBooking.getProductId(),
                approvedBooking.getId(),
                approvedBooking.getStartDate(),
                approvedBooking.getEndDate()
        );

        if (!overlapping.isEmpty()) {
            for (Booking b : overlapping) {
                b.setStatus(BookingStatus.REJECTED);
                b.setRejectionReason("Sản phẩm đã hết hàng");

                if (b.getStatus() == BookingStatus.PAID_WAITING_APPROVAL) {
                    refundTenant(b);
                }
            }
            bookingRepository.saveAll(overlapping);
        }
    }

    // =========================================================================
    // PRIVATE — Fetch từ microservice khác
    // =========================================================================

    private ProductData fetchProductData(Long productId) {
        try {
            String url = productServiceUrl + "/" + productId;
            @SuppressWarnings("unchecked")
            ApiResponse<Object> response = restTemplate.getForObject(url, ApiResponse.class);
            if (response == null || response.getResult() == null) {
                throw new AppException(ErrorCode.PRODUCT_NOT_AVAILABLE);
            }
            LinkedHashMap<?, ?> data = (LinkedHashMap<?, ?>) response.getResult();

            int quantity = 1;
            if (data.get("quantity") instanceof Number num) {
                quantity = num.intValue();
            }

            return new ProductData(
                    productId,
                    ((Number) data.get("pricePerDay")).doubleValue(),
                    (String) data.get("status"),
                    (String) data.get("ownerId"),
                    quantity
            );
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Không thể lấy thông tin sản phẩm: {}", e.getMessage());
            throw new AppException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }
    }

    private ProductInfoDto fetchProductInfo(Long productId) {
        try {
            String url = productServiceUrl + "/" + productId;
            @SuppressWarnings("unchecked")
            ApiResponse<Object> response = restTemplate.getForObject(url, ApiResponse.class);
            if (response == null || response.getResult() == null) return null;

            LinkedHashMap<?, ?> data = (LinkedHashMap<?, ?>) response.getResult();
            String categoryName = null;
            if (data.get("category") instanceof LinkedHashMap<?, ?> cat) {
                categoryName = (String) cat.get("name");
            }

            String imageUrl = null;
            if (data.get("images") instanceof List<?> images && !images.isEmpty()) {
                Object firstImage = images.get(0);
                if (firstImage instanceof LinkedHashMap<?, ?> imgMap) {
                    imageUrl = (String) imgMap.get("imageUrl");
                }
            }

            return ProductInfoDto.builder()
                    .id(((Number) data.get("id")).longValue())
                    .name((String) data.get("name"))
                    .description((String) data.get("description"))
                    .pricePerDay(((Number) data.get("pricePerDay")).doubleValue())
                    .status((String) data.get("status"))
                    .categoryName(categoryName)
                    .imageUrl(imageUrl)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private TenantInfoDto fetchTenantInfo(String tenantId) {
        TenantInfoDto info = TenantInfoDto.builder().username(tenantId).build();
        try {
            String url = identityServiceUrl + "/by-username/" + tenantId;
            @SuppressWarnings("unchecked")
            ApiResponse<Object> response = restTemplate.getForObject(url, ApiResponse.class);
            if (response != null && response.getResult() instanceof LinkedHashMap<?, ?> data) {
                String first = (String) data.get("firstName");
                String last  = (String) data.get("lastName");
                if (first != null || last != null) {
                    info.setFullName(((first != null ? first : "").trim() + " " + (last != null ? last : "").trim()).trim());
                }
                info.setEmail((String) data.get("email"));
                info.setPhone((String) data.get("phone"));
                info.setAddress((String) data.get("address"));
            }
        } catch (Exception ignored) {}
        return info;
    }

    private OwnerInfoDto fetchOwnerInfo(String ownerId) {
        OwnerInfoDto info = OwnerInfoDto.builder().username(ownerId).build();
        try {
            String url = identityServiceUrl + "/by-username/" + ownerId;
            @SuppressWarnings("unchecked")
            ApiResponse<Object> response = restTemplate.getForObject(url, ApiResponse.class);
            if (response != null && response.getResult() instanceof LinkedHashMap<?, ?> data) {
                String first = (String) data.get("firstName");
                String last  = (String) data.get("lastName");
                if (first != null || last != null) {
                    info.setFullName(((first != null ? first : "").trim() + " " + (last != null ? last : "").trim()).trim());
                }
                info.setEmail((String) data.get("email"));
                info.setPhone((String) data.get("phone"));
                info.setAddress((String) data.get("address"));
            }
        } catch (Exception ignored) {}
        return info;
    }

    private record ProductData(Long id, double pricePerDay, String status, String ownerId, int quantity) {}
}