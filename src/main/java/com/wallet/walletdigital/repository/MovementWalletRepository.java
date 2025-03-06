package com.wallet.walletdigital.repository;

import com.wallet.walletdigital.model.MovementWallet;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MovementWalletRepository extends MongoRepository<MovementWallet, String> {

}
