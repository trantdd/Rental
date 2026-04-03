package com.example.rental.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Tiá»‡n Ã­ch tÃ­nh giÃ¡ thuÃª sáº£n pháº©m.
 *
 * CÃ´ng thá»©c: tá»•ng tiá»n = sá»‘ ngÃ y Ã— giÃ¡ má»—i ngÃ y Ã— sá»‘ lÆ°á»£ng
 * Tá»‘i thiá»ƒu 1 ngÃ y, tá»‘i thiá»ƒu 1 Ä‘Æ¡n vá»‹.
 */
public class PriceCalculator {

    private PriceCalculator() {}

    /**
     * Káº¿t quáº£ phÃ¢n tÃ­ch giÃ¡ thuÃª.
     */
    public record PriceBreakdown(int rentalDays, double pricePerDay, int quantity, double rentalFee) {}

    /**
     * TÃ­nh chi tiáº¿t giÃ¡ thuÃª.
     *
     * @param startDate   ngÃ y báº¯t Ä‘áº§u thuÃª
     * @param endDate     ngÃ y káº¿t thÃºc thuÃª (pháº£i sau startDate)
     * @param pricePerDay giÃ¡ thuÃª má»—i ngÃ y (VNÄ)
     * @param quantity    sá»‘ lÆ°á»£ng thuÃª
     * @return PriceBreakdown chá»©a Ä‘áº§y Ä‘á»§ thÃ´ng tin
     */
    public static PriceBreakdown calculate(LocalDate startDate, LocalDate endDate,
                                           double pricePerDay, int quantity) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        int rentalDays = Math.max(1, (int) days);
        int qty = Math.max(1, quantity);
        double rentalFee = rentalDays * pricePerDay * qty;
        return new PriceBreakdown(rentalDays, pricePerDay, qty, rentalFee);
    }
}
