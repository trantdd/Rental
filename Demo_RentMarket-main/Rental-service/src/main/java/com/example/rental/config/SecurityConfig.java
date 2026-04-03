package com.example.rental.config;

import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

/**
 * Cấu hình bảo mật cho Rental-service.
 * Xác thực JWT dùng cùng signerKey (HS512) với Identity-service.
 * Mọi endpoint đặt thuê đều yêu cầu đăng nhập.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @NonFinal
    @Value("${jwt.signerKey}")
    String signerKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(request -> request
                // MoMo IPN callback — MoMo server goi, khong co JWT
                .requestMatchers("/payments/momo/ipn").permitAll()
                // verify-return — FE goi voi JWT, nhung MoMo sandbox redirect co the khong co JWT
                // -> permit de FE goi duoc du khong co token trong mot so truong hop
                .requestMatchers("/payments/momo/verify-return").permitAll()
                // Kiem tra kha dung san pham — public
                .requestMatchers(HttpMethod.GET, "/bookings/availability").permitAll()
                // Tat ca cac endpoint con lai yeu cau dang nhap
                .anyRequest().authenticated()
        );

        // Xác thực JWT dùng NimbusJwtDecoder với thuật toán HS512
        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwtConfigurer -> jwtConfigurer
                        .decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
        );

        // Vô hiệu hóa CSRF (REST API stateless)
        http.csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
        return NimbusJwtDecoder
                .withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }
}
