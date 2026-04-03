package com.example.rental.controller;

import com.example.rental.dto.request.MoMoPaymentRequest;
import com.example.rental.dto.request.PaymentRequest;
import com.example.rental.dto.response.ApiResponse;
import com.example.rental.dto.response.MoMoPaymentResponse;
import com.example.rental.dto.response.PaymentResponse;
import com.example.rental.service.MoMoPaymentService;
import com.example.rental.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller xu ly thanh toan.
 * Base path: /rental/payments  (context-path /rental trong application.yaml)
 *
 * --- MOCK CARD ---
 *  POST /payments              -> thanh toan gia lap
 *  GET  /payments/booking/{id} -> xem thong tin payment
 *
 * --- MOMO ---
 *  POST /payments/momo/create        -> tao don MoMo, nhan payUrl
 *  POST /payments/momo/ipn           -> IPN tu MoMo server (khong can JWT, can URL public)
 *  POST /payments/momo/verify-return -> FE goi sau redirect (KHONG can ngrok, hoat dong localhost)
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final MoMoPaymentService moMoPaymentService;

    // =====================================================================
    // MOCK CARD
    // =====================================================================

    /**
     * @deprecated Không còn sử dụng từ phía Frontend (Phase 2 Payment Streamlining).
     * Luồng Escrow chỉ thực hiện qua Wallet: POST /wallets/pay/{bookingId}.
     * Giữ lại để backward compatibility, sẽ xoá trong phiên bản tiếp theo.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    @PostMapping
    public ApiResponse<PaymentResponse> processPayment(@RequestBody @Valid PaymentRequest request) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.processPayment(request))
                .build();
    }

    @GetMapping("/booking/{bookingId}")
    public ApiResponse<PaymentResponse> getPaymentByBooking(@PathVariable Long bookingId) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.getPaymentByBookingId(bookingId))
                .build();
    }

    // =====================================================================
    // MOMO PAYMENT
    // =====================================================================

    /** Tao don MoMo, tra ve payUrl de FE redirect */
    @PostMapping("/momo/create")
    public ApiResponse<MoMoPaymentResponse> createMoMoPayment(
            @RequestBody @Valid MoMoPaymentRequest request) {
        return ApiResponse.<MoMoPaymentResponse>builder()
                .result(moMoPaymentService.createPayment(request))
                .build();
    }

    /**
     * IPN callback tu MoMo server (server-to-server).
     * Dung tren production. Khi local: can ssh tunnel.
     * MoMo yeu cau response HTTP 200.
     */
    @PostMapping("/momo/ipn")
    public Map<String, Object> handleMoMoIpn(@RequestBody Map<String, String> params) {
        moMoPaymentService.handleIpnCallback(params);
        return Map.of("resultCode", 0, "message", "ok");
    }

    /**
     * Verify ket qua tu return URL parameters.
     * Frontend goi endpoint nay sau khi MoMo redirect ve /payment/result.
     * HOAT DONG TREN LOCALHOST -- KHONG CAN NGROK/TUNNEL.
     *
     * Body: { orderId, resultCode, transId, signature, amount, requestId, orderInfo }
     */
    @PostMapping("/momo/verify-return")
    public ApiResponse<Map<String, Object>> verifyMoMoReturn(
            @RequestBody Map<String, String> params) {
        boolean success = moMoPaymentService.verifyReturn(
                params.getOrDefault("orderId", ""),
                Integer.parseInt(params.getOrDefault("resultCode", "-1")),
                params.getOrDefault("transId", ""),
                params.getOrDefault("signature", ""),
                params.getOrDefault("amount", ""),
                params.getOrDefault("requestId", ""),
                params.getOrDefault("orderInfo", ""),
                params.getOrDefault("extraData", "")
        );
        return ApiResponse.<Map<String, Object>>builder()
                .result(Map.of("success", success))
                .build();
    }
}