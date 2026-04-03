package com.example.rental.service;

import com.example.rental.dto.response.WalletResponse;
import com.example.rental.dto.response.WalletTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service quản lý ví Escrow của người dùng.
 *
 * Luồng Escrow được định nghĩa:
 *   1. Thanh toán đơn thuê: available -> frozen (RENT_PAYMENT)
 *   2. Từ chối đơn       : frozen -> available (REFUND 100%)
 *   3. Hoàn tất đơn      : frozen: cọc -> renter available (REFUND)
 *                          frozen: 70% rentalFee -> owner available (EARNING)
 *                          frozen: 30% rentalFee -> admin/platform (PLATFORM_FEE)
 */
public interface WalletService {

    /** Lấy thông tin ví của user hiện tại (tạo mới nếu chưa có). */
    WalletResponse getMyWallet();

    /** Lấy lịch sử giao dịch ví của user hiện tại — có phân trang. */
    Page<WalletTransactionResponse> getMyTransactions(Pageable pageable);

    /**
     * Thanh toán đơn thuê từ số dư ví (Escrow flow).
     * Trừ totalAmount từ availableBalance → frozenBalance.
     * Cập nhật booking.status = PAID_WAITING_APPROVAL.
     */
    WalletResponse payBookingFromWallet(Long bookingId);
}
