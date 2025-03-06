package com.wallet.walletdigital.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class SendAssociation {
    private String idWallet;
    private String idDebitCard;
}

