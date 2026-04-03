package com.example.rental.service;

import com.example.rental.dto.request.MoMoPaymentRequest;
import com.example.rental.dto.response.MoMoPaymentResponse;
import java.util.Map;

/**
 * Service xu ly thanh toan qua MoMo Payment Gateway.
 *
 * Co 2 cach nhan ket qua tu MoMo:
 *   1. IPN callback (server-to-server)  can URL public (dung tren server that)
 *   2. verifyReturn (frontend goi sau redirect)  hoat dong ngay tren localhost, KHONG can ngrok
 */
public interface MoMoPaymentService {

    /**
     * Khoi tao don thanh toan MoMo, tra ve payUrl de FE redirect.
     */
    MoMoPaymentResponse createPayment(MoMoPaymentRequest request);

    /**
     * Khởi tạo yêu cầu nạp tiền vào ví qua MoMo.
     */
    MoMoPaymentResponse createDepositPayment(com.example.rental.dto.request.MoMoDepositRequest request);

    /**
     * Xu ly IPN callback tu MoMo server (server-to-server).
     * Dung tren production khi co URL public.
     * Khi dev local: MoMo se co gang goi nhung se timeout (khong sao, verifyReturn lo).
     */
    void handleIpnCallback(Map<String, String> params);

    /**
     * Xac thuc ket qua tu tham so tren return URL cua MoMo.
     * Frontend goi endpoint nay NGAY SAU KHI MoMo redirect ve /payment/result.
     * Day la cach khong can ngrok hay bat ky tunnel nao:
     *   1. MoMo redirect sang: /payment/result?orderId=...&resultCode=0&transId=...&signature=...
     *   2. PaymentResult.jsx doc params va goi POST /payments/momo/verify-return
     *   3. Backend xac thuc chu ky, cap nhat Booking -> PAID neu thanh cong
     *
     * @return true neu thanh toan thanh cong va da cap nhat booking
     */
    boolean verifyReturn(String orderId, int resultCode, String transId,
                         String signature, String amount,
                         String requestId, String orderInfo, String extraData);
}
