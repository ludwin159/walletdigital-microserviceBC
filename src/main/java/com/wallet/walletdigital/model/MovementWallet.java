package com.wallet.walletdigital.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Date;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "movement_wallet")
public class MovementWallet {
    @Id
    private String id;

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS",timezone = "GMT")
    private Date createdAt;
    @NotNull
    @NotBlank
    private String idWallet;
    private TypeMovementWallet type;
    @NotNull
    private String description;
    @Min(0)
    @NotNull
    private Double amount;
    private String idDebitCard;

    @NotNull
    @NotBlank
    @Pattern(regexp = "^9\\d{8}$")
    private String numberDestin;
    private StateMovement stateMovement;

    public enum TypeMovementWallet {MAKE_PAYMENT, RECEIPT_PAYMENT}
    public enum StateMovement {APPROVE, PENDING, REJECTED}
}
