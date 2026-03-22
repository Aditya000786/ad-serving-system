package com.adserving.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class TrackTokenService {

    private final byte[] secretKey;
    private final ObjectMapper objectMapper;

    public TrackTokenService(
            @Value("${ad-serving.track-token.secret}") String secret,
            ObjectMapper objectMapper) {
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    public String generateToken(String adId, String campaignId, String userId, String geo) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("ad_id", adId);
            payload.put("campaign_id", campaignId);
            payload.put("user_id", userId != null ? userId : "");
            payload.put("geo", geo != null ? geo : "");
            payload.put("timestamp", Instant.now().toEpochMilli());
            payload.put("nonce", UUID.randomUUID().toString());

            String payloadJson = objectMapper.writeValueAsString(payload);
            String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signature = hmacSha256(payloadBase64);

            return payloadBase64 + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate track token", e);
        }
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }
}
