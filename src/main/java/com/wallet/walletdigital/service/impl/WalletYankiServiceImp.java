package com.wallet.walletdigital.service.impl;

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoWriteException;
import com.wallet.walletdigital.dto.ResponseAssociationWalletDto;
import com.wallet.walletdigital.event.producer.WalletProducer;
import com.wallet.walletdigital.exceptions.ResourceNotFoundException;
import com.wallet.walletdigital.model.WalletYanki;
import com.wallet.walletdigital.repository.WalletYankiRepository;
import com.wallet.walletdigital.service.WalletYankiService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.Signature;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class WalletYankiServiceImp implements WalletYankiService {
    private final WalletYankiRepository walletYankiRepository;
    private final WalletProducer walletProducer;

    public WalletYankiServiceImp(WalletYankiRepository walletYankiRepository,
                                 WalletProducer walletProducer) {
        this.walletYankiRepository = walletYankiRepository;
        this.walletProducer = walletProducer;
    }

    @Override
    public Single<List<WalletYanki>> getAll() {
        return Single.fromCallable(walletYankiRepository::findAll)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<WalletYanki> findById(String id) {
        return Single.fromCallable(() -> walletYankiRepository.findById(id))
                .subscribeOn(Schedulers.io())
                .flatMap(walletOptional -> walletOptional
                        .map(Single::just)
                        .orElseGet(() -> Single.error(new ResourceNotFoundException("The wallet not found."))));
    }

    public Single<WalletYanki> save(WalletYanki walletYanki) {
        walletYanki.setBalance(0.0);
        walletYanki.setCreatedAt(LocalDateTime.now());
        walletYanki.setStateWallet(WalletYanki.StateWallet.APPROVE);
        walletYanki.setHasDebitCard(false);

        return Single.fromCallable(() -> walletYankiRepository.save(walletYanki))
                .onErrorResumeNext(error -> {
                    log.info(error.getClass().getSimpleName());
                    if (error.getClass().getSimpleName().equals("DuplicateKeyException")) {
                        return Single.error(
                                new IllegalArgumentException("The ID number, phone number, or IMEI already exists."));
                    }
                    return Single.error(error);
                })
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<WalletYanki> update(String id, WalletYanki walletYanki) {
        return Single.fromCallable(() -> walletYankiRepository.findById(id).orElseThrow(
                        () -> new ResourceNotFoundException("Wallet not found.")
                ))
                .map(walletFound -> {
                            walletFound.setEmail(walletYanki.getEmail());
                            return walletYankiRepository.save(walletFound);
                        })
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<Boolean> deleteById(String id) {
        return Single.fromCallable(() -> walletYankiRepository.findById(id))
                .flatMap(optionalWallet -> optionalWallet
                        .map(walletYanki -> {
                            walletYankiRepository.deleteById(walletYanki.getId());
                            return Single.just(true);
                        })
                        .orElseGet(() -> Single.just(false))
                );
    }

    @Override
    public Single<WalletYanki> assignDebitCard(WalletYanki walletYanki) {
        return Single.fromCallable(() -> walletYankiRepository.findById(walletYanki.getId()))
                .subscribeOn(Schedulers.io())
                .doOnError(error -> log.error(error.getMessage()))
                .flatMap(optionalWallet -> optionalWallet
                        .map(Single::just)
                        .orElseGet(() -> Single.error(new
                                ResourceNotFoundException("The wallet Yanki does not exist.")))
                )
                .flatMap(walletFound -> validateAssignDebitCard(walletFound)
                        .andThen(Single.just(walletFound))
                )
                .flatMap(walletFound -> {
                    walletFound.setStateWallet(WalletYanki.StateWallet.PENDING);
                    walletFound.setIdDebitCard(walletYanki.getIdDebitCard());
                    walletFound.setHasDebitCard(true);

                    return Completable.fromAction(() ->
                                walletProducer.sendAssociationWallet(walletFound.getId(), walletYanki.getIdDebitCard()))
                            .andThen(Single.fromCallable(() -> walletYankiRepository.save(walletFound))
                                    .subscribeOn(Schedulers.io()));
                })
                .observeOn(Schedulers.io())
                .doOnSuccess(wallet -> log.info("Wallet assigned successfully: {}", wallet.getId()))
                .doOnError(error -> log.error("Error assigning debit card: {}", error.getMessage()));
    }

    private Completable validateAssignDebitCard(WalletYanki walletYanki) {
        if (walletYanki.getHasDebitCard() && walletYanki.getStateWallet() == WalletYanki.StateWallet.APPROVE) {
            String errorMessage = "The wallet already has a debit card assigned: " + walletYanki.getId();
            log.error(errorMessage);
            return Completable.error(new IllegalArgumentException(errorMessage));
        }
        if (walletYanki.getBalance() != 0) {
            String errorMessage = "The client must have balance 0 for this operation: " + walletYanki.getId();
            log.error(errorMessage);
            return Completable.error(new IllegalArgumentException(errorMessage));
        }
        return Completable.complete();
    }

    @Override
    public Single<ResponseAssociationWalletDto> processResponseAssociation(ResponseAssociationWalletDto response) {
        return Single.defer(() -> {
            WalletYanki walletYanki = walletYankiRepository.findById(response.getIdWallet())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found after the response"));

            if (Objects.equals(response.getState(), "APPROVE")) {
                walletYanki.setBalance(response.getBalance());
                walletYanki.setStateWallet(WalletYanki.StateWallet.APPROVE);
                walletYanki.setObservation(response.getObservation());
                log.info("Approve association: " + response);
            } else if (Objects.equals(response.getState(), "REJECTED")) {
                log.info("Rejected association: " + response);
                walletYanki.setStateWallet(WalletYanki.StateWallet.REJECTED);
                walletYanki.setObservation(response.getObservation());
            } else {
                log.error("Unknown association: " + response);
                return Single.error(new IllegalArgumentException("The request could not be processed"));
            }

            log.info("Update wallet: " + walletYanki);
            return Single.fromCallable(() -> walletYankiRepository.save(walletYanki))
                    .map(savedWallet -> response);
        });
    }

    @Override
    public Completable updateBalanceWalletFromDebitCard(String idDebitCard, Double balance) {
        return Single.fromCallable(() -> walletYankiRepository.findByIdDebitCard(idDebitCard))
                .subscribeOn(Schedulers.io())
                .doOnError(error -> log.error(error.getMessage()))
                .flatMap(walletOptional -> walletOptional
                        .map(Single::just)
                        .orElseGet(() -> Single.error(new ResourceNotFoundException("Debit card not found"))))
                .flatMapCompletable(wallet -> {
                    wallet.setBalance(balance);
                    return Completable.fromAction(() -> walletYankiRepository.save(wallet))
                            .subscribeOn(Schedulers.io());
                });
    }

}
