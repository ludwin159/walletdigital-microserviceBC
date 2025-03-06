package com.wallet.walletdigital.service.impl;

import com.wallet.walletdigital.event.producer.WalletProducer;
import com.wallet.walletdigital.exceptions.ResourceNotFoundException;
import com.wallet.walletdigital.model.MovementWallet;
import com.wallet.walletdigital.model.WalletYanki;
import com.wallet.walletdigital.repository.MovementWalletRepository;
import com.wallet.walletdigital.repository.WalletYankiRepository;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.wallet.walletdigital.model.MovementWallet.TypeMovementWallet.RECEIPT_PAYMENT;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovementWalletServiceImpTest {

    @InjectMocks
    private MovementWalletServiceImp movementWalletService;
    @Mock
    private MovementWalletRepository movementWalletRepository;
    @Mock
    private WalletYankiRepository walletYankiRepository;
    @Mock
    private WalletProducer walletProducer;
    @Mock
    private Clock clock;

    private MovementWallet movementWallet1;
    private WalletYanki walletYanki, walletDestin;
    private String idDebitCard;

    @BeforeEach
    void setUp() {
        idDebitCard = "DEBIT_CARD_001";
        movementWallet1 = MovementWallet.builder()
                .id("MOVEMENT1")
                .idWallet("WALLET_01")
                .type(MovementWallet.TypeMovementWallet.MAKE_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.PENDING)
                .amount(20.0)
                .idDebitCard(idDebitCard)
                .numberDestin("929549041")
                .createdAt(Date.from(Instant.now()))
                .build();

        walletYanki = WalletYanki.builder()
                .id("WALLET_01")
                .balance(200.0)
                .createdAt(LocalDateTime.now())
                .stateWallet(WalletYanki.StateWallet.APPROVE)
                .email("prueba@prueba.com")
                .hasDebitCard(false)
                .numberIdentity("7690210")
                .numberPhone("929549040")
                .idDebitCard("")
                .phoneImei("98989999AS986532")
                .build();

        walletDestin = WalletYanki.builder()
                .id("WALLET_02")
                .balance(0.0)
                .createdAt(LocalDateTime.now())
                .stateWallet(WalletYanki.StateWallet.APPROVE)
                .email("prueba1@prueba.com")
                .hasDebitCard(false)
                .numberIdentity("20750210")
                .numberPhone("929549041")
                .idDebitCard("")
                .phoneImei("98989999AS986533")
                .build();
    }

    @Test
    @DisplayName("Get All movements wallet")
    void getAllTest() {
        // Given
        when(movementWalletRepository.findAll()).thenReturn(List.of(movementWallet1));
        // When
        TestObserver<List<MovementWallet>> testObserver = movementWalletService.getAll()
                .subscribeOn(Schedulers.trampoline())
                .test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(list -> list.size() == 1);
    }

    @Test
    @DisplayName("Find movements by Id")
    void findByIdTest() {
        // Given
        when(movementWalletRepository.findById(movementWallet1.getId()))
                .thenReturn(Optional.of(movementWallet1));
        // When
        TestObserver<MovementWallet> testObserver = movementWalletService.findById(movementWallet1.getId())
                .test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(movementWallet -> movementWallet.getId().equals(movementWallet1.getId()));
    }

    @Test
    @DisplayName("Movement not found")
    void findByIdNotFoundTest() {
        // Given
        String idNotFound = "ABCD123";
        when(movementWalletRepository.findById(idNotFound))
                .thenReturn(Optional.empty());
        // When
        TestObserver<MovementWallet> testObserver = movementWalletService.findById(idNotFound).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertError(ResourceNotFoundException.class);
        testObserver.assertError(error -> error.getMessage().equals("Movement not found"));

    }

    @Test
    @DisplayName("Save a movement wallet when wallet not found")
    void saveMovementWalletWithWalletNotFound() {
        String idNotFound = "WALLET_00";
        // Given
        movementWallet1.setIdWallet(idNotFound);
        when(walletYankiRepository.findById(idNotFound)).thenReturn(Optional.empty());
        // When
        TestObserver<MovementWallet> testObserver = movementWalletService.save(movementWallet1)
                .test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertError(ResourceNotFoundException.class);
        testObserver.assertError(error -> error.getMessage().contains("Wallet not exists:"));
    }


    @Test
    @DisplayName("Save a movement wallet destin not found")
    void movementWithWalletDestinNotFoundCard() {
        // Given
        when(walletYankiRepository.findById("WALLET_01")).thenReturn(Optional.of(walletYanki));
        when(walletYankiRepository.findByNumberPhone(movementWallet1.getNumberDestin()))
                .thenReturn(Optional.empty());
        // When
        TestObserver<MovementWallet> testObserver = movementWalletService.save(movementWallet1)
                .test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertError(ResourceNotFoundException.class);
        testObserver.assertError(error -> error.getMessage().contains("Wallet destin not found."));
    }

    @Test
    @DisplayName("Save a movement wallet between accounts without debit card")
    void saveWithoutDebitCard() {
//        MovementWallet movementOrigin = MovementWallet.builder()
//                .idWallet(walletYanki.getId())
//                .type(MAKE_PAYMENT)
//                .stateMovement(MovementWallet.StateMovement.APPROVE)
//                .amount(movementWallet1.getAmount())
//                .numberDestin(walletDestin.getNumberPhone())
//                .createdAt(new Date())
//                .idDebitCard("")
//                .description(movementWallet1.getDescription())
//                .build();
        MovementWallet movementDestin = MovementWallet.builder()
                .id("MOVEMENT1")
                .idWallet(walletDestin.getId())
                .type(RECEIPT_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.APPROVE)
                .amount(movementWallet1.getAmount())
                .numberDestin(walletDestin.getNumberPhone())
                .createdAt(new Date())
                .idDebitCard("")
                .description(movementWallet1.getDescription())
                .build();
        // Given
        when(walletYankiRepository.findById("WALLET_01")).thenReturn(Optional.of(walletYanki));
        when(walletYankiRepository.findByNumberPhone(movementWallet1.getNumberDestin()))
                .thenReturn(Optional.of(walletDestin));
        when(movementWalletRepository.save(any(MovementWallet.class))).thenReturn(movementDestin);
        when(walletYankiRepository.save(walletYanki)).thenReturn(walletYanki);
        when(walletYankiRepository.save(walletDestin)).thenReturn(walletDestin);
        // When
        TestObserver<MovementWallet> testObserver = movementWalletService.save(movementWallet1)
                .test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertComplete();
        testObserver.assertValue(response -> response.getId().equals(movementWallet1.getId()));
    }

    @Test
    @DisplayName("Save a movement wallet with debit card")
    void saveWithDebitCard() {
        walletYanki.setHasDebitCard(true);
        walletYanki.setIdDebitCard("ID_DEBIT_CARD01");
        MovementWallet movementDestin = MovementWallet.builder()
                .id("MOVEMENT1")
                .idWallet(walletDestin.getId())
                .type(RECEIPT_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.APPROVE)
                .amount(movementWallet1.getAmount())
                .numberDestin(walletDestin.getNumberPhone())
                .createdAt(new Date())
                .idDebitCard("")
                .description(movementWallet1.getDescription())
                .build();
        // Given
        when(walletYankiRepository.findById("WALLET_01")).thenReturn(Optional.of(walletYanki));
        when(walletYankiRepository.findByNumberPhone(movementWallet1.getNumberDestin()))
                .thenReturn(Optional.of(walletDestin));
        when(movementWalletRepository.save(any(MovementWallet.class))).thenReturn(movementDestin);
        doAnswer(invocation -> null)
            .when(walletProducer).sendMakePaymentWithDebitCard(any(MovementWallet.class), any(MovementWallet.class));

        // When
        TestObserver<MovementWallet> testObserver = movementWalletService.save(movementWallet1)
                .test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertComplete();
        testObserver.assertValue(response -> response.getId().equals(movementWallet1.getId()));
    }

    @Test
    @DisplayName("Modify description in movement wallet")
    void updateNotExists() {
        String idMovement = "ASF20";
        // Given
        when(movementWalletRepository.findById(idMovement)).thenReturn(Optional.empty());
        // When
        TestObserver<MovementWallet> testObserver = movementWalletService.update(idMovement, movementWallet1)
                .test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertError(ResourceNotFoundException.class);
        testObserver.assertError(error -> error.getMessage().contains("Movement not found"));
    }

    @Test
    @DisplayName("Modify description in movement wallet")
    void update() {
        String idMovement = movementWallet1.getId();
        movementWallet1.setDescription("New Description test");
        // Given
        when(movementWalletRepository.findById(idMovement)).thenReturn(Optional.of(movementWallet1));
        when(movementWalletRepository.save(movementWallet1)).thenReturn(movementWallet1);
        // When
        TestObserver<MovementWallet> testObserver = movementWalletService.update(idMovement, movementWallet1)
                .test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertComplete();
        testObserver.assertValue(value -> value.getDescription().contains("New"));
    }


    @Test
    @DisplayName("Delete a movement by id not found")
    void deleteByIdNotFoundTest() {
        // Given
        when(movementWalletRepository.findById("NOTFOUND")).thenReturn(Optional.empty());
        // When
        TestObserver<MovementWallet> testObserver = movementWalletService.update("NOTFOUND", movementWallet1)
                .test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertError(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Delete a movement by id")
    void deleteById() {
        // Given
        when(movementWalletRepository.findById(movementWallet1.getId())).thenReturn(Optional.of(movementWallet1));
        doNothing().when(movementWalletRepository).deleteById(movementWallet1.getId());
        // When
        TestObserver<Boolean> testObserver = movementWalletService.deleteById(movementWallet1.getId())
                .test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertComplete();
    }

    @Test
    @DisplayName("Process response movement from debit card service")
    void processMovementsWithDebitCard() {
        MovementWallet movementWallet2 = MovementWallet.builder()
                .id("MOVEMENT2")
                .idWallet("WALLET_02")
                .type(RECEIPT_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.APPROVE)
                .amount(20.0)
                .idDebitCard(idDebitCard)
                .numberDestin("929549040")
                .createdAt(Date.from(Instant.now()))
                .build();
        movementWallet1.setStateMovement(MovementWallet.StateMovement.APPROVE);
        List<MovementWallet> movements = List.of(movementWallet1, movementWallet2);

        // Given
        when(walletYankiRepository.findById(movementWallet1.getIdWallet())).thenReturn(Optional.of(walletYanki));
        when(walletYankiRepository.findById(movementWallet2.getIdWallet())).thenReturn(Optional.of(walletDestin));
        when(walletYankiRepository.save(walletYanki)).thenReturn(walletYanki);
        when(walletYankiRepository.save(walletDestin)).thenReturn(walletDestin);
        when(movementWalletRepository.save(movementWallet1)).thenReturn(movementWallet1);
        when(movementWalletRepository.save(movementWallet2)).thenReturn(movementWallet2);
        // When
        TestObserver<Void> testObserver = movementWalletService.processMovementsWithDebitCard(movements)
                .test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertComplete();
    }
}