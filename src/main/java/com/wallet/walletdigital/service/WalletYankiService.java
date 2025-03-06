package com.wallet.walletdigital.service;

import com.wallet.walletdigital.dto.ResponseAssociationWalletDto;
import com.wallet.walletdigital.model.WalletYanki;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

import java.util.List;

public interface WalletYankiService {
    public Single<List<WalletYanki>> getAll();
    public Single<WalletYanki> findById(String id);
    public Single<WalletYanki> save(WalletYanki walletYanki);
    public Single<WalletYanki> update(String id, WalletYanki walletYanki);
    public Single<Boolean> deleteById(String id);
    public Single<WalletYanki> assignDebitCard(WalletYanki walletYanki);
    Single<ResponseAssociationWalletDto> processResponseAssociation(ResponseAssociationWalletDto response);
    Completable updateBalanceWalletFromDebitCard(String idDebitCard, Double balance);
}
