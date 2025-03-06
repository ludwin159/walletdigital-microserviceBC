package com.wallet.walletdigital.service;

import com.wallet.walletdigital.model.MovementWallet;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

import java.util.List;

public interface MovementWalletService {
    public Single<List<MovementWallet>> getAll();
    public Single<MovementWallet> findById(String id);
    public Single<MovementWallet> save(MovementWallet movementWallet);
    public Single<MovementWallet> update(String id, MovementWallet movementWallet);
    public Single<Boolean> deleteById(String id);
    public Completable processMovementsWithDebitCard(List<MovementWallet> movements);
}
