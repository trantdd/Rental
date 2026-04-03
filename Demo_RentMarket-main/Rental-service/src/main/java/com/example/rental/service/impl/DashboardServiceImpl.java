package com.example.rental.service.impl;

import com.example.rental.config.JwtUtils;
import com.example.rental.dto.response.AdminDashboardResponse;
import com.example.rental.dto.response.ApiResponse;
import com.example.rental.dto.response.DashboardResponse;
import com.example.rental.entity.BookingStatus;
import com.example.rental.repository.BookingRepository;
import com.example.rental.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private final JwtUtils jwtUtils;
    private final RestTemplate restTemplate;

    @Value("${app.product-service.url}")
    private String productServiceUrl;

    @Value("${app.review-service.url:http://localhost:8083/reviews}")
    private String reviewServiceUrl;

    @Value("${app.identity-service.url:http://localhost:8080/identity/users}")
    private String identityServiceUrl;

    // =========================================================================
    // OWNER DASHBOARD — Thong ke ca nhan theo JWT
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getStats() {
        String ownerId = jwtUtils.getCurrentUsername();
        log.info("Lay thong ke kinh doanh cho chu do: {}", ownerId);

        // 1. Du lieu tu Rental DB
        Double totalRev    = bookingRepository.sumCompletedRevenue(ownerId);
        Double pendingRev  = bookingRepository.sumPendingRevenue(ownerId);
        Long totalBookings = bookingRepository.countTotalActiveBookingsForOwner(ownerId);
        Long pendingCount  = bookingRepository.countByProductOwnerIdAndStatus(ownerId, BookingStatus.PENDING_PAYMENT);

        long approvedCount  = bookingRepository.countByProductOwnerIdAndStatus(ownerId, BookingStatus.APPROVED);
        long paidCount      = bookingRepository.countByProductOwnerIdAndStatus(ownerId, BookingStatus.PAID_WAITING_APPROVAL);
        Long activeBookings = approvedCount + paidCount;

        Long completedCount = bookingRepository.countByProductOwnerIdAndStatus(ownerId, BookingStatus.COMPLETED);

        // 1.1 Doanh thu theo thang
        Map<Integer, Double> monthlyRevMap = initMonthMap();
        for (Object[] row : bookingRepository.getMonthlyRevenue(ownerId)) {
            if (row[0] != null && row[1] != null) {
                monthlyRevMap.put(((Number) row[0]).intValue(), ((Number) row[1]).doubleValue());
            }
        }

        // 2. Tong do tu Product-service
        Long totalItems = fetchTotalItems();

        // 3. Rating tu Review-service
        Double avgRating   = 0.0;
        Long totalReviews  = 0L;
        try {
            String url = reviewServiceUrl + "/owner/" + ownerId + "/rating";
            @SuppressWarnings("unchecked")
            ApiResponse<LinkedHashMap<String, Object>> response = restTemplate.getForObject(url, ApiResponse.class);
            if (response != null && response.getResult() != null) {
                Object avg = response.getResult().get("avgRating");
                Object tot = response.getResult().get("totalReviews");
                if (avg instanceof Number) avgRating    = ((Number) avg).doubleValue();
                if (tot instanceof Number) totalReviews = ((Number) tot).longValue();
            }
        } catch (Exception e) {
            log.warn("Loi khong goi duoc Review-service thong ke: {}", e.getMessage());
        }

        return DashboardResponse.builder()
                .totalRevenue(totalRev)
                .pendingRevenue(pendingRev)
                .totalBookings(totalBookings)
                .pendingBookings(pendingCount)
                .activeBookings(activeBookings)
                .completedBookings(completedCount)
                .totalItems(totalItems)
                .averageRating(avgRating)
                .totalReviews(totalReviews)
                .monthlyRevenue(monthlyRevMap)
                .build();
    }

    // =========================================================================
    // ADMIN DASHBOARD — Thong ke toan san
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminStats() {
        log.info("Lay thong ke toan san (Admin Dashboard)");

        // 1. Doanh thu nen tang (30% * rentalFee cua toan bo don COMPLETED)
        Double platformRevenue = bookingRepository.sumPlatformRevenue();

        // 1.1 Doanh thu nen tang theo thang
        Map<Integer, Double> monthlyPlatformRev = initMonthMap();
        for (Object[] row : bookingRepository.getMonthlyPlatformRevenue()) {
            if (row[0] != null && row[1] != null) {
                monthlyPlatformRev.put(((Number) row[0]).intValue(), ((Number) row[1]).doubleValue());
            }
        }

        // 2. Tong giao dich toan san
        long totalTransactions = bookingRepository.count();
        long completedCount    = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        long pendingApproval   = bookingRepository.countByStatus(BookingStatus.PAID_WAITING_APPROVAL);
        long cancelled         = bookingRepository.countByStatus(BookingStatus.CANCELLED)
                               + bookingRepository.countByStatus(BookingStatus.REJECTED);

        // 3. Tong nguoi dung tu Identity-service
        Long totalUsers = fetchTotalUsers();

        return AdminDashboardResponse.builder()
                .totalPlatformRevenue(platformRevenue)
                .monthlyPlatformRevenue(monthlyPlatformRev)
                .totalUsers(totalUsers)
                .totalTransactions(totalTransactions)
                .pendingApprovalCount(pendingApproval)
                .completedTransactions(completedCount)
                .cancelledTransactions(cancelled)
                .build();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Map<Integer, Double> initMonthMap() {
        Map<Integer, Double> map = new HashMap<>();
        for (int i = 1; i <= 12; i++) map.put(i, 0.0);
        return map;
    }

    private Long fetchTotalItems() {
        try {
            String url = productServiceUrl + "/me";
            @SuppressWarnings("unchecked")
            ApiResponse<LinkedHashMap<String, Object>> response = restTemplate.getForObject(url, ApiResponse.class);
            if (response != null && response.getResult() != null) {
                Object totalObj = response.getResult().get("totalElements");
                if (totalObj instanceof Number) return ((Number) totalObj).longValue();
            }
        } catch (Exception e) {
            log.warn("Loi khong goi duoc Product-service thong ke: {}", e.getMessage());
        }
        return 0L;
    }

    private Long fetchTotalUsers() {
        try {
            @SuppressWarnings("unchecked")
            ApiResponse<LinkedHashMap<String, Object>> response =
                    restTemplate.getForObject(identityServiceUrl, ApiResponse.class);
            if (response != null && response.getResult() != null) {
                Object total = response.getResult().get("totalElements");
                if (total instanceof Number) return ((Number) total).longValue();
            }
        } catch (Exception e) {
            log.warn("Loi khong goi duoc Identity-service users: {}", e.getMessage());
        }
        return 0L;
    }
}
