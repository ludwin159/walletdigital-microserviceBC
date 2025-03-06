package com.wallet.walletdigital.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@Data
@Document(collection = "wallet_yanki")
public class WalletYanki {
    @Id
    private String id;
    private LocalDateTime createdAt;
    @Indexed(unique = true)
    @Pattern(regexp = "^\\d{8}$|^\\d{11}$|^\\d{15}$")
    private String numberIdentity;

    @Indexed(unique = true)
    @Pattern(regexp = "^9\\d{8}$")
    private String numberPhone;

    @Indexed(unique = true)
    @Pattern(regexp = "^\\d{15}$")
    private String phoneImei;
    @Pattern(regexp = "^[^@]+@[^@]+\\.[a-zA-Z]{2,}$", message = "The email is not valid.")
    private String email;
    @Min(0)
    private Double balance;
    private Boolean hasDebitCard;

    @Indexed(unique = true)
    private String idDebitCard;
    private StateWallet stateWallet;
    private String observation;
    @Transient
    private List<MovementWallet> movementsWallet;

    public enum StateWallet {APPROVE, PENDING, REJECTED}

    public WalletYanki() {
        this.balance = 0.0;
        this.hasDebitCard = false;
        this.idDebitCard = "";
        this.stateWallet = StateWallet.APPROVE;
        this.movementsWallet = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }
}
