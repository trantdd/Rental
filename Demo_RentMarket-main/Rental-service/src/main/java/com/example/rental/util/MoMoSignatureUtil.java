package com.example.rental.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Tien ich tao chu ky (signature) HMAC-SHA256 cho MoMo Payment Gateway.
 *
 * Thu tu chuoi ky tu phai dung chinh xac theo tai lieu MoMo:
 * accessKey=...&amount=...&extraData=...&ipnUrl=...&orderId=...
 * &orderInfo=...&partnerCode=...&redirectUrl=...&requestId=...&requestType=...
 */
public class MoMoSignatureUtil {

    private MoMoSignatureUtil() {}

    /**
     * Tao chu ky HMAC-SHA256.
     *
     * @param rawData  Chuoi du lieu can ky (da duoc sap xep theo alphabet)
     * @param secretKey MoMo Secret Key
     * @return Chu ky dang HEX lowercase
     */
    public static String signHmacSHA256(String rawData, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(rawData.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Loi khi tao chu ky MoMo HMAC-SHA256", e);
        }
    }

    /**
     * Build chuoi raw signature theo dung thu tu alphabet MoMo yeu cau.
     * Thu tu: accessKey, amount, extraData, ipnUrl, orderId,
     *         orderInfo, partnerCode, redirectUrl, requestId, requestType
     */
    public static String buildRawSignature(
            String accessKey,
            String amount,
            String extraData,
            String ipnUrl,
            String orderId,
            String orderInfo,
            String partnerCode,
            String redirectUrl,
            String requestId,
            String requestType
    ) {
        return "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
    }
}
