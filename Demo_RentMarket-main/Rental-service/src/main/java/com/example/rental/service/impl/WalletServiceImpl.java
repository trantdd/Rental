package com.example.rental.service.impl;

import com.example.rental.config.JwtUtils;
import com.example.rental.dto.response.WalletResponse;
import com.example.rental.dto.response.WalletTransactionResponse;
import com.example.rental.entity.Booking;
import com.example.rental.entity.BookingStatus;
import com.example.rental.entity.TransactionType;
import com.example.rental.entity.Wallet;
import com.example.rental.entity.WalletTransaction;
import com.example.rental.exception.AppException;
import com.example.rental.exception.ErrorCode;
import com.example.rental.repository.BookingRepository;
import com.example.rental.repository.WalletRepository;
import com.example.rental.repository.WalletTransactionRepository;
import com.example.rental.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final BookingRepository bookingRepository;
    private final JwtUtils jwtUtils;

    // =========================================================================
    // Lấy / tạo ví
    // =========================================================================

    @Override
    @Transactional
    public WalletResponse getMyWallet() {
        String userId = jwtUtils.getCurrentUsername();
        Wallet wallet = getOrCreateWallet(userId);
        return toWalletResponse(wallet);
    }

    // =========================================================================
    // Lịch sử giao dịch
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getMyTransactions(Pageable pageable) {
        String userId = jwtUtils.getCurrentUsername();
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        return transactionRepository.findByWalletOrderByCreatedAtDesc(wallet, pageable)
                .map(this::toTransactionResponse);
    }

    // =========================================================================
    // Thanh toán đơn thuê bằng ví (Escrow)
    // =========================================================================

    @Override
    @Transactional
    public WalletResponse payBookingFromWallet(Long bookingId) {
        String userId = jwtUtils.getCurrentUsername();
        log.info("User {} thanh toan booking {} bang vi", userId, bookingId);

        // 1. Lay booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // 2. Kiem tra quyen — chi nguoi thue moi duoc thanh toan
        if (!booking.getTenantId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // 3. Kiem tra trang thai — chi duoc thanh toan khi PENDING_PAYMENT
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }

        // 4. Tinh tong so tien can thanh toan
        double totalAmount = (booking.getRentalFee() != null ? booking.getRentalFee() : 0.0)
                           + (booking.getDepositFee() != null ? booking.getDepositFee() : 0.0);

        // 5. Lay vi nguoi thue
        Wallet renterWallet = getOrCreateWallet(userId);

        // 6. Kiem tra so du kha dung
        if (renterWallet.getAvailableBalance() < totalAmount) {
            log.warn("So du vi cua {} khong du: can {} nhung chi co {}",
                    userId, totalAmount, renterWallet.getAvailableBalance());
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 7. Escrow: available -> frozen
        renterWallet.setAvailableBalance(renterWallet.getAvailableBalance() - totalAmount);
        renterWallet.setFrozenBalance(renterWallet.getFrozenBalance() + totalAmount);
        walletRepository.save(renterWallet);

        // 8. Ghi lich su giao dich
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(renterWallet)
                .amount(totalAmount)
                .type(TransactionType.RENT_PAYMENT)
                .bookingId(bookingId)
                .description("Thanh toan don thue #" + bookingId + " (Escrow: khoa tam)")
                .build();
        transactionRepository.save(tx);

        // 9. Cap nhat trang thai booking -> PAID_WAITING_APPROVAL
        booking.setStatus(BookingStatus.PAID_WAITING_APPROVAL);
        bookingRepository.save(booking);

        log.info("Thanh toan booking {} thanh cong. So du vi: available={}, frozen={}",
                bookingId, renterWallet.getAvailableBalance(), renterWallet.getFrozenBalance());

        return toWalletResponse(renterWallet);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /** Lấy ví theo userId, nếu chưa có thì tạo mới với số dư 0. */
    private Wallet getOrCreateWallet(String userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            log.info("Tao vi moi cho user: {}", userId);
            Wallet newWallet = Wallet.builder()
                    .userId(userId)
                    .availableBalance(0.0)
                    .frozenBalance(0.0)
                    .build();
            return walletRepository.save(newWallet);
        });
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        double available = wallet.getAvailableBalance() != null ? wallet.getAvailableBalance() : 0.0;
        double frozen    = wallet.getFrozenBalance() != null ? wallet.getFrozenBalance() : 0.0;
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .availableBalance(available)
                .frozenBalance(frozen)
                .totalBalance(available + frozen)
                .build();
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction tx) {
        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .amount(tx.getAmount())
                .type(tx.getType())
                .bookingId(tx.getBookingId())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
