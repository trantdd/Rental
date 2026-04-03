package com.example.rental.service.impl;

import com.example.rental.config.JwtUtils;
import com.example.rental.dto.request.MoMoDepositRequest;
import com.example.rental.dto.request.MoMoPaymentRequest;
import com.example.rental.dto.response.MoMoPaymentResponse;
import com.example.rental.entity.*;
import com.example.rental.exception.AppException;
import com.example.rental.exception.ErrorCode;
import com.example.rental.repository.BookingRepository;
import com.example.rental.repository.PaymentRepository;
import com.example.rental.repository.WalletRepository;
import com.example.rental.repository.WalletTransactionRepository;
import com.example.rental.service.MoMoPaymentService;
import com.example.rental.util.MoMoSignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoMoPaymentServiceImpl implements MoMoPaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final JwtUtils jwtUtils;
    private final RestTemplate restTemplate;

    @Value("${app.momo.partner-code}")
    private String partnerCode;

    @Value("${app.momo.access-key}")
    private String accessKey;

    @Value("${app.momo.secret-key}")
    private String secretKey;

    @Value("${app.momo.api-endpoint}")
    private String apiEndpoint;

    @Value("${app.momo.notify-url}")
    private String notifyUrl;

    @Value("${app.momo.return-url}")
    private String returnUrl;

    // =========================================================
    // 1. THANH TOAN DON THUE (BOOKING)
    // =========================================================

    @Override
    @Transactional
    public MoMoPaymentResponse createPayment(MoMoPaymentRequest request) {
        String currentUser = jwtUtils.getCurrentUsername();

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getTenantId().equals(currentUser))
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT)
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        if (paymentRepository.existsByBookingIdAndStatus(booking.getId(), "SUCCESS"))
            throw new AppException(ErrorCode.BOOKING_ALREADY_PAID);

        String requestId   = partnerCode + "_" + booking.getId() + "_" + System.currentTimeMillis();
        String orderId     = "BOOKING_" + booking.getId() + "_" + System.currentTimeMillis();
        double totalToPay  = booking.getRentalFee() + (booking.getDepositFee() != null ? booking.getDepositFee() : 0.0);
        String amount      = String.valueOf((long) totalToPay);
        String orderInfo   = "Thanh toan don thue #" + booking.getId() + " (Tien thue + Doc coc)";
        
        return executeMoMoApiRequest(booking.getId(), totalToPay, requestId, orderId, orderInfo, amount, this.returnUrl, "");
    }

    // =========================================================
    // 2. NAP TIEN VAO VI (DEPOSIT)
    // =========================================================

    @Override
    @Transactional
    public MoMoPaymentResponse createDepositPayment(MoMoDepositRequest request) {
        String currentUser = jwtUtils.getCurrentUsername();
        double totalToPay = request.getAmount();

        String timestampSuffix = String.valueOf(System.currentTimeMillis());
        String randomSuffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        String requestId   = partnerCode + "_DEPOSIT_" + timestampSuffix + "_" + randomSuffix;
        String orderId     = "DEPOSIT_" + timestampSuffix + "_" + randomSuffix;
        String amount      = String.valueOf((long) totalToPay);
        String orderInfo   = "Nap tien vao vi RentMarket";
        
        String callbackUrl = this.returnUrl + "?type=wallet_topup";
        String extraData = java.util.Base64.getEncoder().encodeToString(currentUser.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        return executeMoMoApiRequest(null, totalToPay, requestId, orderId, orderInfo, amount, callbackUrl, extraData);
    }

    private MoMoPaymentResponse executeMoMoApiRequest(Long bookingId, double totalToPay, String requestId, String orderId, String orderInfo, String amount, String callbackUrl, String extraData) {
        String requestType = "captureWallet";

        String rawSignature = MoMoSignatureUtil.buildRawSignature(
                accessKey, amount, extraData, notifyUrl,
                orderId, orderInfo, partnerCode, callbackUrl,
                requestId, requestType);
        String signature = MoMoSignatureUtil.signHmacSHA256(rawSignature, secretKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("partnerName", "RentMarket");
        requestBody.put("storeId", "RentMarketStore");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", Long.parseLong(amount));
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", callbackUrl);
        requestBody.put("ipnUrl", notifyUrl);
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", requestType);
        requestBody.put("signature", signature);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> momoRes = restTemplate.postForObject(apiEndpoint, entity, Map.class);
            if (momoRes == null) throw new AppException(ErrorCode.MOMO_PAYMENT_FAILED);

            int    rc      = momoRes.containsKey("resultCode") ? ((Number) momoRes.get("resultCode")).intValue() : -1;
            String payUrl  = (String) momoRes.getOrDefault("payUrl", "");
            String qrUrl   = (String) momoRes.getOrDefault("qrCodeUrl", "");
            String message = (String) momoRes.getOrDefault("message", "");

            if (rc != 0 || payUrl.isBlank()) throw new AppException(ErrorCode.MOMO_PAYMENT_FAILED);

            return MoMoPaymentResponse.builder()
                    .bookingId(bookingId) // Co the null neu la deposit
                    .amount(totalToPay)
                    .orderId(orderId).payUrl(payUrl).qrCodeUrl(qrUrl)
                    .resultCode(rc).message(message).build();

        } catch (AppException e) { throw e; }
        catch (Exception e) {
            throw new AppException(ErrorCode.MOMO_PAYMENT_FAILED);
        }
    }

    // =========================================================
    // 3. IPN & VERIFY
    // =========================================================

    @Override
    @Transactional
    public void handleIpnCallback(Map<String, String> p) {
        String orderId  = p.getOrDefault("orderId", "");
        String rcStr    = p.getOrDefault("resultCode", "-1");
        String amount   = p.getOrDefault("amount", "0");
        String transId  = p.getOrDefault("transId", "");

        int rc = Integer.parseInt(rcStr);
        if (orderId.startsWith("BOOKING_")) {
            processBookingCallback(orderId, rc, transId, amount);
        } else if (orderId.startsWith("DEPOSIT_")) {
            String extraData = p.getOrDefault("extraData", "");
            processDepositCallback(orderId, rc, transId, amount, extraData);
        }
    }

    @Override
    @Transactional
    public boolean verifyReturn(String orderId, int resultCode, String transId,
                                String signature, String amount,
                                String requestId, String orderInfo, String extraData) {
        if (orderId.startsWith("BOOKING_")) {
            return processBookingCallback(orderId, resultCode, transId, amount);
        } else if (orderId.startsWith("DEPOSIT_")) {
            return processDepositCallback(orderId, resultCode, transId, amount, extraData);
        }
        return false;
    }

    // --- Booking logic ---
    private boolean processBookingCallback(String orderId, int resultCode, String transId, String amount) {
        Long bookingId = parseBookingId(orderId);
        if (bookingId == null) return false;

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return false;

        if (resultCode == 0) {
            if (!paymentRepository.existsByBookingIdAndStatus(bookingId, "SUCCESS")) {
                double paidAmount = Double.parseDouble(amount);
                
                paymentRepository.save(Payment.builder()
                        .bookingId(booking.getId()).tenantId(booking.getTenantId())
                        .amount(paidAmount).status("SUCCESS")
                        .paymentMethod("MOMO").transactionId("MOMO_" + transId).build());

                Wallet tenantWallet = walletRepository.findByUserId(booking.getTenantId())
                        .orElseGet(() -> walletRepository.save(Wallet.builder().userId(booking.getTenantId()).availableBalance(0.0).frozenBalance(0.0).build()));

                // 1. NAP TIEN TU MOMO VAO VI (DEPOSIT)
                tenantWallet.setAvailableBalance(tenantWallet.getAvailableBalance() + paidAmount);
                walletTransactionRepository.save(WalletTransaction.builder()
                        .wallet(tenantWallet).amount(paidAmount).type(TransactionType.DEPOSIT)
                        .bookingId(booking.getId()).description("Nap tien tu MoMo (Ma GD " + transId + ")").build());

                // 2. TRU TIEN VI, DUA VAO DONG BANG (RENT_PAYMENT)
                tenantWallet.setAvailableBalance(tenantWallet.getAvailableBalance() - paidAmount);
                tenantWallet.setFrozenBalance(tenantWallet.getFrozenBalance() + paidAmount);
                walletTransactionRepository.save(WalletTransaction.builder()
                        .wallet(tenantWallet).amount(paidAmount).type(TransactionType.RENT_PAYMENT)
                        .bookingId(booking.getId()).description("Dong bang tien thue & coc (MoMo)").build());

                walletRepository.save(tenantWallet);
                booking.setStatus(BookingStatus.PAID_WAITING_APPROVAL);
                bookingRepository.save(booking);
            }
            return true;
        } else {
            paymentRepository.save(Payment.builder()
                    .bookingId(bookingId).tenantId(booking.getTenantId())
                    .amount(Double.parseDouble(amount)).status("FAILED")
                    .paymentMethod("MOMO").transactionId("MOMO_FAIL_" + transId).build());
            return false;
        }
    }

    // --- Deposit logic ---
    private boolean processDepositCallback(String orderId, int resultCode, String transId, String amount, String extraData) {
        String userId = null;
        if (extraData != null && !extraData.isEmpty()) {
            try {
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(extraData);
                userId = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("Failed to decode extraData: {}", extraData);
            }
        }
        // Fallback ngược về orderId nếu không có extraData
        if (userId == null || userId.isEmpty()) {
            userId = parseUserIdFromDeposit(orderId);
        }

        if (userId == null || userId.isEmpty()) return false;

        if (resultCode == 0) {
            // Vi IPN va Return co the cung chay, kiem tra trung transId
            String desc = "Nap tien vao vi qua MoMo (Ma GD: " + transId + ")";
            boolean exists = walletTransactionRepository.findAll().stream()
                    .anyMatch(t -> t.getDescription() != null && t.getDescription().equals(desc));
            
            if (!exists) {
                double paidAmount = Double.parseDouble(amount);
                final String finalUserId = userId;
                Wallet tenantWallet = walletRepository.findByUserId(finalUserId)
                    .orElseGet(() -> walletRepository.save(Wallet.builder().userId(finalUserId).availableBalance(0.0).frozenBalance(0.0).build()));

                tenantWallet.setAvailableBalance(tenantWallet.getAvailableBalance() + paidAmount);
                walletTransactionRepository.save(WalletTransaction.builder()
                        .wallet(tenantWallet).amount(paidAmount).type(TransactionType.DEPOSIT)
                        .bookingId(null).description(desc).build());

                walletRepository.save(tenantWallet);
            }
            return true;
        }
        return false;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Long parseBookingId(String orderId) {
        try {
            String[] parts = orderId.split("_");
            if (parts.length >= 2 && "BOOKING".equals(parts[0])) return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {}
        return null;
    }

    private String parseUserIdFromDeposit(String orderId) {
        try {
            String[] parts = orderId.split("_");
            if (parts.length >= 2 && "DEPOSIT".equals(parts[0])) return parts[1];
        } catch (Exception e) {}
        return null;
    }
}