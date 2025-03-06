package com.wallet.walletdigital.service.impl;

import com.wallet.walletdigital.event.producer.WalletProducer;
import com.wallet.walletdigital.exceptions.ResourceNotFoundException;
import com.wallet.walletdigital.model.MovementWallet;
import com.wallet.walletdigital.model.WalletYanki;
import com.wallet.walletdigital.repository.MovementWalletRepository;
import com.wallet.walletdigital.repository.WalletYankiRepository;
import com.wallet.walletdigital.service.MovementWalletService;
import com.wallet.walletdigital.utils.Numbers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static com.wallet.walletdigital.model.WalletYanki.StateWallet.*;
import static com.wallet.walletdigital.model.MovementWallet.TypeMovementWallet.*;

@Service
@Slf4j
public class MovementWalletServiceImp implements MovementWalletService {
    private final MovementWalletRepository movementWalletRepository;
    private final WalletYankiRepository walletYankiRepository;
    private final WalletProducer walletProducer;

    public MovementWalletServiceImp(MovementWalletRepository movementWalletRepository,
                                    WalletYankiRepository walletYankiRepository,
                                    WalletProducer walletProducer) {
        this.movementWalletRepository = movementWalletRepository;
        this.walletYankiRepository = walletYankiRepository;
        this.walletProducer = walletProducer;
    }

    @Override
    public Single<List<MovementWallet>> getAll() {
        return Single.fromCallable(movementWalletRepository::findAll)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<MovementWallet> findById(String id) {
        return Single.fromCallable(() -> movementWalletRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Movement not found"))
                )
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<MovementWallet> save(MovementWallet movementWallet) {
        movementWallet.setType(MAKE_PAYMENT);
        return Single.fromCallable(() -> walletYankiRepository.findById(movementWallet.getIdWallet()))
                .flatMap(walletOptional -> {
                    if (walletOptional.isEmpty()) {
                        String messageError = "Wallet not exists: " + movementWallet.getIdWallet();
                        log.error(messageError);
                        return Single.error(
                                new ResourceNotFoundException(messageError));
                    }
                    return Single.just(walletOptional.get());
                })
                .flatMap(walletYanki -> walletYankiRepository.findByNumberPhone(movementWallet.getNumberDestin())
                        .map(Single::just)
                        .orElseGet(() -> Single.error(new ResourceNotFoundException("Wallet destin not found.")))
                        .flatMap(walletDestin -> isPayValid(walletYanki, walletDestin, movementWallet)
                                .flatMap(isValid -> {
                                    if (bothWithoutDebitCard(walletYanki, walletDestin)) {
                                        return makeImmediatePayment(walletYanki, walletDestin, movementWallet);
                                    }
                                    return generateTransactionsAndSaveMovements(
                                            walletYanki, walletDestin, movementWallet);
                                }))
                )

                .subscribeOn(Schedulers.io());
    }

    private Single<Boolean> isPayValid(WalletYanki wallet, WalletYanki walletDestin, MovementWallet movementWallet) {
        String messageError = "";
        if (movementWallet.getAmount() < 0.1) {
            messageError = "The amount cannot be less than 0.1";
        }
        if (wallet.getBalance() < movementWallet.getAmount()) {
            messageError = "The wallet balance must be greater than the payment amount";
        }
        if (wallet.getStateWallet() != APPROVE) {
            messageError = "The origin wallet has a " + wallet.getStateWallet() + " status";
        }
        if (walletDestin.getStateWallet() != APPROVE) {
            messageError = "The destination wallet has a " + walletDestin.getStateWallet() + " status";
        }
        if (!messageError.isEmpty()) {
            log.error(messageError);
            return Single.error(new IllegalArgumentException(messageError));
        }
        return Single.just(true);
    }
    private boolean bothWithoutDebitCard(WalletYanki wallet, WalletYanki walletDestin) {
        return !wallet.getHasDebitCard() && !walletDestin.getHasDebitCard();
    }
    private Single<MovementWallet> makeImmediatePayment(WalletYanki wallet,
                                                        WalletYanki walletDestin,
                                                        MovementWallet movement) {
        MovementWallet movementOrigin = MovementWallet.builder()
                .idWallet(wallet.getId())
                .type(MAKE_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.APPROVE)
                .amount(movement.getAmount())
                .numberDestin(walletDestin.getNumberPhone())
                .createdAt(new Date())
                .idDebitCard("")
                .description(movement.getDescription())
                .build();
        MovementWallet movementDestin = MovementWallet.builder()
                .idWallet(walletDestin.getId())
                .type(RECEIPT_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.APPROVE)
                .amount(movement.getAmount())
                .numberDestin(wallet.getNumberPhone())
                .createdAt(new Date())
                .idDebitCard("")
                .description(movement.getDescription())
                .build();

        wallet.setBalance(wallet.getBalance() - movement.getAmount());
        walletDestin.setBalance(walletDestin.getBalance() + movement.getAmount());

        Tuple2<MovementWallet, MovementWallet> movementsTuple = Tuple.of(movementOrigin, movementDestin);

        return Single.zip(
                Single.fromCallable(() -> movementWalletRepository.save(movementsTuple._1)),
                Single.fromCallable(() -> movementWalletRepository.save(movementsTuple._2)),
                (savedOrigin, savedDestin) -> savedOrigin
        ).flatMap(savedMovement -> Single.zip(
                Single.fromCallable(() -> walletYankiRepository.save(wallet)),
                Single.fromCallable(() -> walletYankiRepository.save(walletDestin)),
                (updatedWallet, updatedWalletDestin) -> savedMovement
        ));
    }

    private Single<MovementWallet> generateTransactionsAndSaveMovements(WalletYanki wallet,
                                                                        WalletYanki walletDestin,
                                                                        MovementWallet movement) {
        MovementWallet movementOrigin = MovementWallet.builder()
                .idWallet(wallet.getId())
                .type(MAKE_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.PENDING)
                .amount(movement.getAmount())
                .numberDestin(walletDestin.getNumberPhone())
                .createdAt(new Date())
                .idDebitCard(wallet.getIdDebitCard())
                .description(movement.getDescription())
                .build();
        MovementWallet movementDestin = MovementWallet.builder()
                .idWallet(walletDestin.getId())
                .type(RECEIPT_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.PENDING)
                .amount(movement.getAmount())
                .numberDestin(wallet.getNumberPhone())
                .createdAt(new Date())
                .idDebitCard(walletDestin.getIdDebitCard())
                .description(movement.getDescription())
                .build();

        Tuple2<MovementWallet, MovementWallet> movementsTuple = Tuple.of(movementOrigin, movementDestin);

        return Single.zip(
                Single.fromCallable(() -> movementWalletRepository.save(movementsTuple._1)),
                Single.fromCallable(() -> movementWalletRepository.save(movementsTuple._2)),
                Tuple::of
        )
                .flatMap(tuple ->
                        Single.fromCallable(() -> {
                            log.info("Send make payment with debit card.");
                            walletProducer.sendMakePaymentWithDebitCard(tuple._1, tuple._2);
                            return tuple._1;
                        })
                        .subscribeOn(Schedulers.io()));
    }
    @Override
    public Single<MovementWallet> update(String id, MovementWallet movementWallet) {
        return Single.fromCallable(() -> movementWalletRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Movement not found: " + id)))
                .flatMap(walletFound -> {
                    walletFound.setDescription(movementWallet.getDescription());
                    return Single.fromCallable(() -> movementWalletRepository.save(walletFound));
                })
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<Boolean> deleteById(String id) {
        return Single.fromCallable(() -> movementWalletRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Movement not found " + id))
                )
                .map(movementWallet -> {
                    movementWalletRepository.deleteById(movementWallet.getId());
                    return true;
                });
    }

    @Override
    public Completable processMovementsWithDebitCard(List<MovementWallet> movements) {
        MovementWallet movementOrigin = movements.get(0);
        MovementWallet movementDestin = movements.get(1);

        if (movementOrigin.getStateMovement() == MovementWallet.StateMovement.APPROVE &&
                movementDestin.getStateMovement() == MovementWallet.StateMovement.APPROVE) {

            double amount = movementOrigin.getAmount();

            Single<WalletYanki> walletOrigin =
                    Single.fromCallable(() -> walletYankiRepository.findById(movementOrigin.getIdWallet())
                            .orElseThrow(() -> new ResourceNotFoundException("Wallet origin not found")));

            Single<WalletYanki> walletDestin =
                    Single.fromCallable(() -> walletYankiRepository.findById(movementDestin.getIdWallet())
                            .orElseThrow(() -> new ResourceNotFoundException("Wallet destin not found")));

            return Single.zip(walletOrigin, walletDestin, (origin, destin) -> {
                origin.setBalance(Numbers.round(origin.getBalance() - amount));
                destin.setBalance(Numbers.round(destin.getBalance() + amount));

                Single<WalletYanki> saveOrigin = Single.fromCallable(() -> walletYankiRepository.save(origin));
                Single<WalletYanki> saveDestin = Single.fromCallable(() -> walletYankiRepository.save(destin));
                Single<MovementWallet> saveMovementOrigin = Single.fromCallable(() ->
                        movementWalletRepository.save(movementOrigin));
                Single<MovementWallet> saveMovementDestin = Single.fromCallable(() ->
                        movementWalletRepository.save(movementDestin));

                return Completable.mergeArray(
                        saveOrigin.ignoreElement(),
                        saveDestin.ignoreElement(),
                        saveMovementOrigin.ignoreElement(),
                        saveMovementDestin.ignoreElement()
                );
            }).flatMapCompletable(completable -> completable);
        }

        return Completable.complete();
    }
}


