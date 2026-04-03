package com.example.rental.controller;

import com.example.rental.dto.request.CancelBookingRequest;
import com.example.rental.dto.request.CreateBookingRequest;
import com.example.rental.dto.request.RejectBookingRequest;
import com.example.rental.dto.response.ApiResponse;
import com.example.rental.dto.response.AvailabilityResponse;
import com.example.rental.dto.response.BookingResponse;
import com.example.rental.entity.BookingStatus;
import com.example.rental.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller xá»­ lÃ½ toÃ n bá»™ tÃ¡c vá»¥ Ä‘áº·t thuÃª.
 *
 * Base path: /rental/bookings  (context-path /rental Ä‘Æ°á»£c cáº¥u hÃ¬nh trong application.yaml)
 * Táº¥t cáº£ endpoint Ä‘á»u yÃªu cáº§u JWT (báº£o vá»‡ bá»Ÿi SecurityConfig).
 *
 * â”€â”€â”€ NGÆ¯á»œI THUÃŠ (tenant) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 *  POST   /bookings                â†’ táº¡o yÃªu cáº§u Ä‘áº·t thuÃª
 *  PUT    /bookings/{id}/cancel    â†’ huá»· yÃªu cáº§u (kÃ¨m lÃ½ do tuá»³ chá»n)
 *  GET    /bookings/my-rentals     â†’ danh sÃ¡ch booking cá»§a tÃ´i (cÃ³ lá»c)
 *
 * â”€â”€â”€ CHá»¦ Äá»’ (owner) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 *  PUT    /bookings/{id}/accept    â†’ cháº¥p nháº­n yÃªu cáº§u (PENDING â†’ APPROVED)
 *  PUT    /bookings/{id}/reject    â†’ tá»« chá»‘i yÃªu cáº§u kÃ¨m lÃ½ do (PENDING â†’ REJECTED)
 *  PUT    /bookings/{id}/complete  â†’ hoÃ n táº¥t (APPROVED â†’ COMPLETED)
 *  GET    /bookings/my-items       â†’ danh sÃ¡ch booking cho sáº£n pháº©m cá»§a tÃ´i (cÃ³ lá»c)
 *
 * â”€â”€â”€ CHUNG â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 *  GET    /bookings/{id}           â†’ chi tiáº¿t booking (chá»‰ 2 bÃªn liÃªn quan)
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // =========================================================================
    // NGÆ¯á»œI THUÃŠ
    // =========================================================================

    /**
     * Táº¡o yÃªu cáº§u Ä‘áº·t thuÃª má»›i.
     * tenantId Ä‘Æ°á»£c láº¥y tá»± Ä‘á»™ng tá»« JWT â€” khÃ´ng nháº­n tá»« client.
     */
    @PostMapping
    public ApiResponse<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.createBooking(request))
                .build();
    }

    /**
     * NgÆ°á»i thuÃª huá»· yÃªu cáº§u Ä‘áº·t thuÃª (chá»‰ khi PENDING).
     * LÃ½ do huá»· lÃ  tuá»³ chá»n â€” body cÃ³ thá»ƒ bá» qua hoÃ n toÃ n.
     *
     * JSON body (tuá»³ chá»n): { "reason": "TÃ´i thay Ä‘á»•i káº¿ hoáº¡ch" }
     */
    @PutMapping("/{id}/cancel")
    public ApiResponse<BookingResponse> cancelBooking(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelBookingRequest request
    ) {
        String reason = (request != null) ? request.getReason() : null;
        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.cancelBooking(id, reason))
                .build();
    }

    /**
     * NgÆ°á»i thuÃª xem danh sÃ¡ch booking cá»§a mÃ¬nh vá»›i bá»™ lá»c vÃ  phÃ¢n trang.
     * Response kÃ¨m thÃ´ng tin sáº£n pháº©m tá»« Product-service (best-effort).
     *
     * Query params (táº¥t cáº£ tuá»³ chá»n):
     *  - status   : PENDING / APPROVED / REJECTED / CANCELLED / COMPLETED
     *  - fromDate : bookings cÃ³ startDate >= fromDate (yyyy-MM-dd)
     *  - toDate   : bookings cÃ³ endDate   <= toDate   (yyyy-MM-dd)
     *  - page, size (máº·c Ä‘á»‹nh 0, 10)
     */
    @GetMapping("/my-rentals")
    public ApiResponse<Page<BookingResponse>> getMyRentals(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<Page<BookingResponse>>builder()
                .result(bookingService.getMyRentals(status, fromDate, toDate, page, size))
                .build();
    }

    // =========================================================================
    // CHá»¦ Äá»’
    // =========================================================================

    /**
     * Chá»§ Ä‘á»“ cháº¥p nháº­n yÃªu cáº§u Ä‘áº·t thuÃª (PENDING â†’ APPROVED).
     * Chá»‰ productOwnerId == JWT username má»›i Ä‘Æ°á»£c phÃ©p.
     */
    @PutMapping("/{id}/accept")
    public ApiResponse<BookingResponse> acceptBooking(@PathVariable Long id) {
        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.acceptBooking(id))
                .build();
    }

    /**
     * Chá»§ Ä‘á»“ tá»« chá»‘i yÃªu cáº§u Ä‘áº·t thuÃª kÃ¨m lÃ½ do báº¯t buá»™c (PENDING â†’ REJECTED).
     * Chá»‰ productOwnerId == JWT username má»›i Ä‘Æ°á»£c phÃ©p.
     *
     * JSON body: { "reason": "Sáº£n pháº©m Ä‘ang báº£o trÃ¬" }
     */
    @PutMapping("/{id}/reject")
    public ApiResponse<BookingResponse> rejectBooking(
            @PathVariable Long id,
            @Valid @RequestBody RejectBookingRequest request
    ) {
        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.rejectBooking(id, request.getReason()))
                .build();
    }

    /**
     * Chá»§ Ä‘á»“ Ä‘Ã¡nh dáº¥u booking hoÃ n táº¥t sau khi sáº£n pháº©m Ä‘Æ°á»£c tráº£ láº¡i (APPROVED â†’ COMPLETED).
     * Tá»± Ä‘á»™ng gá»i Product-service Ä‘á»ƒ Ä‘áº·t sáº£n pháº©m vá» AVAILABLE (best-effort).
     */
    @PutMapping("/{id}/handover")
    public ApiResponse<BookingResponse> confirmHandover(@PathVariable Long id) {
        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.confirmHandover(id))
                .build();
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<BookingResponse> completeBooking(@PathVariable Long id) {
        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.completeBooking(id))
                .build();
    }

    /**
     * Chá»§ Ä‘á»“ xem danh sÃ¡ch booking cho sáº£n pháº©m cá»§a mÃ¬nh vá»›i bá»™ lá»c vÃ  phÃ¢n trang.
     * Response kÃ¨m thÃ´ng tin sáº£n pháº©m (Product-service) vÃ  ngÆ°á»i thuÃª (Identity-service), best-effort.
     *
     * Query params (táº¥t cáº£ tuá»³ chá»n):
     *  - status    : PENDING / APPROVED / REJECTED / CANCELLED / COMPLETED
     *  - productId : lá»c theo sáº£n pháº©m cá»¥ thá»ƒ
     *  - fromDate  : bookings cÃ³ startDate >= fromDate (yyyy-MM-dd)
     *  - toDate    : bookings cÃ³ endDate   <= toDate   (yyyy-MM-dd)
     *  - page, size (máº·c Ä‘á»‹nh 0, 10)
     */
    @GetMapping("/my-items")
    public ApiResponse<Page<BookingResponse>> getOwnerBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<Page<BookingResponse>>builder()
                .result(bookingService.getOwnerBookings(status, productId, fromDate, toDate, page, size))
                .build();
    }

    // =========================================================================
    // CHUNG
    // =========================================================================

    /**
     * Kiá»ƒm tra sá»‘ lÆ°á»£ng sáº£n pháº©m cÃ²n trá»‘ng cho khoáº£ng thá»i gian cá»¥ thá»ƒ.
     *
     * Query params:
     *  - productId : ID sáº£n pháº©m (báº¯t buá»™c)
     *  - startDate : ngÃ y báº¯t Ä‘áº§u (yyyy-MM-dd, báº¯t buá»™c)
     *  - endDate   : ngÃ y káº¿t thÃºc (yyyy-MM-dd, báº¯t buá»™c)
     *
     * Response: { totalQuantity, bookedQuantity, availableQuantity, nextAvailableDate }
     */
    @GetMapping("/availability")
    public ApiResponse<AvailabilityResponse> checkAvailability(
            @RequestParam Long productId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.<AvailabilityResponse>builder()
                .result(bookingService.checkAvailability(productId, startDate, endDate))
                .build();
    }

    /**
     * Xem chi tiáº¿t má»™t booking theo ID.
     * Chá»‰ tenantId hoáº·c productOwnerId cá»§a booking Ä‘Ã³ má»›i Ä‘Æ°á»£c xem (403 náº¿u vi pháº¡m).
     */
    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> getBookingById(@PathVariable Long id) {
        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.getBookingById(id))
                .build();
    }
}
