package com.example.rental.entity;

public enum TransactionType {
    DEPOSIT,        // Nạp tiền vào ví
    RENT_PAYMENT,   // Chuyển tiền vào khoản đóng băng (Khách thanh toán)
    REFUND,         // Hoàn tiền cọc (Từ đóng băng sang available)
    PLATFORM_FEE,   // Tiền 30% phí sàn (Chủ gửi cho Admin)
    EARNING         // Doanh thu 70% chủ đồ nhận được
}
