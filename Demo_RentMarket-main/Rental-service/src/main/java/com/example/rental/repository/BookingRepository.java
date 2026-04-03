package com.example.rental.repository;

import com.example.rental.entity.Booking;
import com.example.rental.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * Repository cho Booking entity Ã¢â‚¬â€ PERSON-175.
 * Cung cÃ¡ÂºÂ¥p cÃƒÂ¡c query phÃ¡Â»Â¥c vÃ¡Â»Â¥:
 *   - NgÃ†Â°Ã¡Â»Âi thuÃƒÂª xem booking cÃ¡Â»Â§a mÃƒÂ¬nh
 *   - ChÃ¡Â»Â§ Ã„â€˜Ã¡Â»â€œ xem booking cho sÃ¡ÂºÂ£n phÃ¡ÂºÂ©m cÃ¡Â»Â§a mÃƒÂ¬nh
 *   - KiÃ¡Â»Æ’m tra xung Ã„â€˜Ã¡Â»â„¢t lÃ¡Â»â€¹ch Ã„â€˜Ã¡ÂºÂ·t thuÃƒÂª
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * LÃ¡ÂºÂ¥y danh sÃƒÂ¡ch booking mÃƒÂ  ngÃ†Â°Ã¡Â»Âi dÃƒÂ¹ng lÃƒÂ  NGÃ†Â¯Ã¡Â»Å“I THUÃƒÅ , cÃƒÂ³ phÃƒÂ¢n trang.
     *
     * @param tenantId username cÃ¡Â»Â§a ngÃ†Â°Ã¡Â»Âi thuÃƒÂª (tÃ¡Â»Â« JWT)
     * @param pageable thÃƒÂ´ng tin phÃƒÂ¢n trang
     */
    Page<Booking> findByTenantId(String tenantId, Pageable pageable);

    /**
     * LÃ¡ÂºÂ¥y danh sÃƒÂ¡ch booking cho cÃƒÂ¡c sÃ¡ÂºÂ£n phÃ¡ÂºÂ©m mÃƒÂ  ngÃ†Â°Ã¡Â»Âi dÃƒÂ¹ng lÃƒÂ  CHÃ¡Â»Â¦ SÃ¡Â»Å¾ HÃ¡Â»Â®U, cÃƒÂ³ phÃƒÂ¢n trang.
     *
     * @param productOwnerId username cÃ¡Â»Â§a chÃ¡Â»Â§ sÃ¡Â»Å¸ hÃ¡Â»Â¯u sÃ¡ÂºÂ£n phÃ¡ÂºÂ©m
     * @param pageable thÃƒÂ´ng tin phÃƒÂ¢n trang
     */
    Page<Booking> findByProductOwnerId(String productOwnerId, Pageable pageable);

    /**
     * KiÃ¡Â»Æ’m tra xem sÃ¡ÂºÂ£n phÃ¡ÂºÂ©m Ã„â€˜ÃƒÂ£ cÃƒÂ³ booking PENDING hoÃ¡ÂºÂ·c APPROVED nÃƒÂ o
     * trÃƒÂ¹ng lÃ¡ÂºÂ·p vÃ¡Â»â€ºi khoÃ¡ÂºÂ£ng thÃ¡Â»Âi gian [startDate, endDate] chÃ†Â°a.
     *
     * Ã„ÂiÃ¡Â»Âu kiÃ¡Â»â€¡n trÃƒÂ¹ng lÃ¡Â»â€¹ch: startDate1 <= endDate2 VÃƒâ‚¬ endDate1 >= startDate2
     *
     * @param productId ID sÃ¡ÂºÂ£n phÃ¡ÂºÂ©m
     * @param startDate ngÃƒÂ y bÃ¡ÂºÂ¯t Ã„â€˜Ã¡ÂºÂ§u muÃ¡Â»â€˜n Ã„â€˜Ã¡ÂºÂ·t
     * @param endDate ngÃƒÂ y kÃ¡ÂºÂ¿t thÃƒÂºc muÃ¡Â»â€˜n Ã„â€˜Ã¡ÂºÂ·t
     * @return true nÃ¡ÂºÂ¿u cÃƒÂ³ xung Ã„â€˜Ã¡Â»â„¢t lÃ¡Â»â€¹ch
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.productId = :productId
              AND b.status IN ('PENDING_PAYMENT', 'PAID_WAITING_APPROVAL', 'APPROVED', 'IN_PROGRESS')
              AND b.startDate <= :endDate
              AND b.endDate >= :startDate
            """)
    boolean existsOverlappingBooking(
            @Param("productId") Long productId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * LÃ¡ÂºÂ¥y danh sÃƒÂ¡ch booking cÃ¡Â»Â§a ngÃ†Â°Ã¡Â»Âi thuÃƒÂª vÃ¡Â»â€ºi bÃ¡Â»â„¢ lÃ¡Â»Âc tuÃ¡Â»Â³ chÃ¡Â»Ân Ã¢â‚¬â€ PERSON-189.
     *
     * LÃ¡Â»Âc theo:
     *   - tenantId    : bÃ¡ÂºÂ¯t buÃ¡Â»â„¢c (tÃ¡Â»Â« JWT)
     *   - status      : tuÃ¡Â»Â³ chÃ¡Â»Ân (null = lÃ¡ÂºÂ¥y tÃ¡ÂºÂ¥t cÃ¡ÂºÂ£ trÃ¡ÂºÂ¡ng thÃƒÂ¡i)
     *   - fromDate    : tuÃ¡Â»Â³ chÃ¡Â»Ân Ã¢â‚¬â€ chÃ¡Â»â€° lÃ¡ÂºÂ¥y booking cÃƒÂ³ startDate >= fromDate
     *   - toDate      : tuÃ¡Â»Â³ chÃ¡Â»Ân Ã¢â‚¬â€ chÃ¡Â»â€° lÃ¡ÂºÂ¥y booking cÃƒÂ³ endDate <= toDate
     *
     * @param tenantId  username ngÃ†Â°Ã¡Â»Âi thuÃƒÂª
     * @param status    trÃ¡ÂºÂ¡ng thÃƒÂ¡i booking (null = khÃƒÂ´ng lÃ¡Â»Âc)
     * @param fromDate  ngÃƒÂ y bÃ¡ÂºÂ¯t Ã„â€˜Ã¡ÂºÂ§u tÃ¡Â»â€˜i thiÃ¡Â»Æ’u (null = khÃƒÂ´ng lÃ¡Â»Âc)
     * @param toDate    ngÃƒÂ y kÃ¡ÂºÂ¿t thÃƒÂºc tÃ¡Â»â€˜i Ã„â€˜a (null = khÃƒÂ´ng lÃ¡Â»Âc)
     * @param pageable  thÃƒÂ´ng tin phÃƒÂ¢n trang
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.tenantId = :tenantId
              AND (:status IS NULL OR b.status = :status)
              AND (:fromDate IS NULL OR b.startDate >= :fromDate)
              AND (:toDate IS NULL OR b.endDate <= :toDate)
            ORDER BY b.createdAt DESC
            """)
    Page<Booking> findMyRentals(
            @Param("tenantId") String tenantId,
            @Param("status") BookingStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    /**
     * LÃ¡ÂºÂ¥y danh sÃƒÂ¡ch booking mÃƒÂ  ngÃ†Â°Ã¡Â»Âi dÃƒÂ¹ng lÃƒÂ  CHÃ¡Â»Â¦ Ã„ÂÃ¡Â»â€™ vÃ¡Â»â€ºi bÃ¡Â»â„¢ lÃ¡Â»Âc tuÃ¡Â»Â³ chÃ¡Â»Ân Ã¢â‚¬â€ PERSON-196.
     *
     * LÃ¡Â»Âc theo:
     *   - productOwnerId : bÃ¡ÂºÂ¯t buÃ¡Â»â„¢c (tÃ¡Â»Â« JWT)
     *   - status         : tuÃ¡Â»Â³ chÃ¡Â»Ân (null = tÃ¡ÂºÂ¥t cÃ¡ÂºÂ£ trÃ¡ÂºÂ¡ng thÃƒÂ¡i)
     *   - productId      : tuÃ¡Â»Â³ chÃ¡Â»Ân Ã¢â‚¬â€ lÃ¡Â»Âc theo sÃ¡ÂºÂ£n phÃ¡ÂºÂ©m cÃ¡Â»Â¥ thÃ¡Â»Æ’
     *   - fromDate       : tuÃ¡Â»Â³ chÃ¡Â»Ân Ã¢â‚¬â€ booking cÃƒÂ³ startDate >= fromDate
     *   - toDate         : tuÃ¡Â»Â³ chÃ¡Â»Ân Ã¢â‚¬â€ booking cÃƒÂ³ endDate <= toDate
     *
     * @param productOwnerId username chÃ¡Â»Â§ sÃ¡Â»Å¸ hÃ¡Â»Â¯u sÃ¡ÂºÂ£n phÃ¡ÂºÂ©m
     * @param status         trÃ¡ÂºÂ¡ng thÃƒÂ¡i booking (null = khÃƒÂ´ng lÃ¡Â»Âc)
     * @param productId      ID sÃ¡ÂºÂ£n phÃ¡ÂºÂ©m cÃ¡Â»Â¥ thÃ¡Â»Æ’ (null = khÃƒÂ´ng lÃ¡Â»Âc)
     * @param fromDate       ngÃƒÂ y bÃ¡ÂºÂ¯t Ã„â€˜Ã¡ÂºÂ§u tÃ¡Â»â€˜i thiÃ¡Â»Æ’u (null = khÃƒÂ´ng lÃ¡Â»Âc)
     * @param toDate         ngÃƒÂ y kÃ¡ÂºÂ¿t thÃƒÂºc tÃ¡Â»â€˜i Ã„â€˜a (null = khÃƒÂ´ng lÃ¡Â»Âc)
     * @param pageable       thÃƒÂ´ng tin phÃƒÂ¢n trang
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.productOwnerId = :productOwnerId
              AND (:status IS NULL OR b.status = :status)
              AND (:productId IS NULL OR b.productId = :productId)
              AND (:fromDate IS NULL OR b.startDate >= :fromDate)
              AND (:toDate IS NULL OR b.endDate <= :toDate)
            ORDER BY b.createdAt DESC
            """)
    Page<Booking> findOwnerBookings(
            @Param("productOwnerId") String productOwnerId,
            @Param("status") BookingStatus status,
            @Param("productId") Long productId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    /**
     * TÃƒÂ¬m cÃƒÂ¡c booking PENDING khÃƒÂ¡c cÃ¡Â»Â§a cÃƒÂ¹ng sÃ¡ÂºÂ£n phÃ¡ÂºÂ©m mÃƒÂ  trÃƒÂ¹ng lÃ¡Â»â€¹ch vÃ¡Â»â€ºi booking vÃ¡Â»Â«a Ã„â€˜Ã†Â°Ã¡Â»Â£c APPROVED.
     * DÃƒÂ¹ng Ã„â€˜Ã¡Â»Æ’ tÃ¡Â»Â± Ã„â€˜Ã¡Â»â„¢ng REJECT khi chÃ¡Â»Â§ Ã„â€˜Ã¡Â»â€œ approve 1 Ã„â€˜Ã†Â¡n.
     *
     * LoÃ¡ÂºÂ¡i trÃ¡Â»Â« booking Ã„â€˜ÃƒÂ£ Ã„â€˜Ã†Â°Ã¡Â»Â£c approve (excludeBookingId).
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.productId = :productId
              AND b.status = 'PENDING'
              AND b.id <> :excludeBookingId
              AND b.startDate <= :endDate
              AND b.endDate >= :startDate
            """)
    java.util.List<Booking> findOverlappingPendingBookings(
            @Param("productId") Long productId,
            @Param("excludeBookingId") Long excludeBookingId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * TÃƒÂ­nh tÃ¡Â»â€¢ng sÃ¡Â»â€˜ lÃ†Â°Ã¡Â»Â£ng Ã„â€˜ÃƒÂ£ APPROVED trong khoÃ¡ÂºÂ£ng thÃ¡Â»Âi gian trÃƒÂ¹ng lÃ¡ÂºÂ·p.
     * DÃƒÂ¹ng Ã„â€˜Ã¡Â»Æ’ kiÃ¡Â»Æ’m tra cÃƒÂ²n bao nhiÃƒÂªu quantity khÃ¡ÂºÂ£ dÃ¡Â»Â¥ng.
     */
    @Query("""
            SELECT COALESCE(SUM(b.quantity), 0) FROM Booking b
            WHERE b.productId = :productId
              AND b.status IN ('APPROVED', 'IN_PROGRESS')
              AND b.startDate <= :endDate
              AND b.endDate >= :startDate
            """)
    int sumApprovedQuantity(
            @Param("productId") Long productId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * TÃƒÂ¬m ngÃƒÂ y kÃ¡ÂºÂ¿t thÃƒÂºc sÃ¡Â»â€ºm nhÃ¡ÂºÂ¥t trong cÃƒÂ¡c booking APPROVED cÃ¡Â»Â§a sÃ¡ÂºÂ£n phÃ¡ÂºÂ©m.
     * DÃƒÂ¹ng Ã„â€˜Ã¡Â»Æ’ hiÃ¡Â»Æ’n thÃ¡Â»â€¹ "DÃ¡Â»Â± kiÃ¡ÂºÂ¿n cÃƒÂ³ hÃƒÂ ng lÃ¡ÂºÂ¡i vÃƒÂ o ngÃƒÂ y..."
     */
    @Query("""
            SELECT MIN(b.endDate) FROM Booking b
            WHERE b.productId = :productId
              AND b.status IN ('APPROVED', 'IN_PROGRESS')
              AND b.endDate >= :today
            """)
    LocalDate findEarliestAvailableDate(
            @Param("productId") Long productId,
            @Param("today") LocalDate today
    );

    // =========================================================================
    // THÃ¡Â»ÂNG KÃƒÅ  (DASHBOARD) Ã¢â‚¬â€ PERSON-277
    // =========================================================================

    @Query("SELECT COALESCE(SUM(b.rentalFee), 0.0) FROM Booking b WHERE b.productOwnerId = :ownerId AND b.status = 'COMPLETED'")
    Double sumCompletedRevenue(@Param("ownerId") String ownerId);

    @Query("SELECT COALESCE(SUM(b.rentalFee), 0.0) FROM Booking b WHERE b.productOwnerId = :ownerId AND b.status IN ('PENDING_PAYMENT', 'PAID_WAITING_APPROVAL', 'APPROVED', 'IN_PROGRESS')")
    Double sumPendingRevenue(@Param("ownerId") String ownerId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.productOwnerId = :ownerId AND b.status NOT IN ('CANCELLED', 'REJECTED')")
    Long countTotalActiveBookingsForOwner(@Param("ownerId") String ownerId);

    long countByProductOwnerIdAndStatus(String productOwnerId, BookingStatus status);

    /**
     * Lay doanh thu hoan tat (COMPLETED) theo tung thang trong nam hien tai.
     * Tra ve Object[] gom: [0] = tnt_month (Int), [1] = sum_revenue (Double)
     */
    @Query("SELECT MONTH(b.endDate) as thang, SUM(b.rentalFee) as doanhThu " +
           "FROM Booking b WHERE b.productOwnerId = :ownerId AND b.status = 'COMPLETED' " +
           "AND YEAR(b.endDate) = YEAR(CURRENT_DATE) " +
           "GROUP BY MONTH(b.endDate) ORDER BY MONTH(b.endDate)")
    java.util.List<Object[]> getMonthlyRevenue(@Param("ownerId") String ownerId);

    // =========================================================================
    // ADMIN — Thống kê toàn sàn
    // =========================================================================

    /** Tổng số booking toàn sàn */
    long count();

    /** Đếm booking theo trạng thái toàn sàn (không giới hạn owner) */
    long countByStatus(BookingStatus status);

    /**
     * Tổng phí nền tảng đã thu (30% rentalFee của tất cả đơn COMPLETED).
     * Business rule: Platform = 30%, Owner = 70%
     */
    @Query("SELECT COALESCE(SUM(b.rentalFee * 0.30), 0.0) FROM Booking b WHERE b.status = 'COMPLETED'")
    Double sumPlatformRevenue();

    /**
     * Doanh thu nền tảng theo tháng (30% rentalFee, đơn COMPLETED, năm hiện tại).
     */
    @Query("SELECT MONTH(b.endDate) as thang, SUM(b.rentalFee * 0.30) as doanhThu " +
           "FROM Booking b WHERE b.status = 'COMPLETED' " +
           "AND YEAR(b.endDate) = YEAR(CURRENT_DATE) " +
           "GROUP BY MONTH(b.endDate) ORDER BY MONTH(b.endDate)")
    java.util.List<Object[]> getMonthlyPlatformRevenue();
}

