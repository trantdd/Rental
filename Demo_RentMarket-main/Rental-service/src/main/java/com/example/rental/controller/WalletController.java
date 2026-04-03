package com.example.rental.controller;

import com.example.rental.dto.request.MoMoDepositRequest;
import com.example.rental.dto.response.ApiResponse;
import com.example.rental.dto.response.MoMoPaymentResponse;
import com.example.rental.dto.response.WalletResponse;
import com.example.rental.dto.response.WalletTransactionResponse;
import com.example.rental.service.WalletService;
import com.example.rental.service.MoMoPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller cho Wallet API.
 * Base path: /rental/wallets (context-path /rental trong application.yaml)
 *
 * GET  /wallets/me                  → Lấy thông tin ví hiện tại (tạo mới nếu chưa có)
 * GET  /wallets/me/transactions     → Lịch sử giao dịch (phân trang)
 * POST /wallets/pay/{bookingId}     → Thanh toán đơn thuê bằng ví (Escrow flow)
 */
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final MoMoPaymentService moMoPaymentService;

    /**
     * Lấy thông tin ví của user hiện tại.
     * Nếu chưa có ví, tự động tạo mới với số dư 0.
     */
    @GetMapping("/me")
    public ApiResponse<WalletResponse> getMyWallet() {
        return ApiResponse.<WalletResponse>builder()
                .result(walletService.getMyWallet())
                .build();
    }

    /**
     * Lịch sử giao dịch ví của user hiện tại.
     * Query params: page (default 0), size (default 20)
     */
    @GetMapping("/me/transactions")
    public ApiResponse<Page<WalletTransactionResponse>> getMyTransactions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<WalletTransactionResponse>>builder()
                .result(walletService.getMyTransactions(pageable))
                .build();
    }

    /**
     * Thanh toán đơn thuê bằng số dư ví — Escrow flow:
     *   availableBalance -= totalAmount
     *   frozenBalance    += totalAmount
     *   booking.status    = PAID_WAITING_APPROVAL
     */
    @PostMapping("/pay/{bookingId}")
    public ApiResponse<WalletResponse> payBookingFromWallet(@PathVariable Long bookingId) {
        return ApiResponse.<WalletResponse>builder()
                .result(walletService.payBookingFromWallet(bookingId))
                .build();
    }

    /**
     * Nạp tiền vào ví qua MoMo.
     * Trả về URL để redirect tới cổng thanh toán MoMo.
     */
    @PostMapping("/deposit/momo")
    public ApiResponse<MoMoPaymentResponse> depositMomo(@RequestBody @Valid MoMoDepositRequest request) {
        return ApiResponse.<MoMoPaymentResponse>builder()
                .result(moMoPaymentService.createDepositPayment(request))
                .build();
    }
}
