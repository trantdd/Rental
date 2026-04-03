package com.example.rental.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String userId;

    @Column(nullable = false)
    @Builder.Default
    Double availableBalance = 0.0;

    @Column(nullable = false)
    @Builder.Default
    Double frozenBalance = 0.0;
}