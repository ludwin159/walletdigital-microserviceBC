package com.wallet.walletdigital.repository;

import com.wallet.walletdigital.model.WalletYanki;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface WalletYankiRepository extends MongoRepository<WalletYanki, String> {

    Optional<WalletYanki> findByIdDebitCard(String idDebitCard);
    Optional<WalletYanki> findByNumberPhone(String numberPhone);
}
