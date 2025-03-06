package com.wallet.walletdigital.service.impl;

import com.wallet.walletdigital.dto.ResponseAssociationWalletDto;
import com.wallet.walletdigital.event.producer.WalletProducer;
import com.wallet.walletdigital.exceptions.ResourceNotFoundException;
import com.wallet.walletdigital.model.WalletYanki;
import com.wallet.walletdigital.repository.WalletYankiRepository;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class WalletYankiServiceImpTest {

    @InjectMocks
    private WalletYankiServiceImp walletYankiService;
    @Mock
    private WalletYankiRepository walletYankiRepository;
    @Mock
    private WalletProducer walletProducer;

    private WalletYanki walletYanki, walletDestin;

    @BeforeEach
    void setUp() {
        walletYanki = WalletYanki.builder()
                .id("WALLET_01")
                .balance(200.0)
                .createdAt(LocalDateTime.now())
                .stateWallet(WalletYanki.StateWallet.APPROVE)
                .email("prueba@prueba.com")
                .hasDebitCard(false)
                .numberIdentity("7690210")
                .numberPhone("929549040")
                .idDebitCard("DEBITCARD01")
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
    @DisplayName("Find all wallets")
    void getAll() {
        // Given
        when(walletYankiRepository.findAll()).thenReturn(List.of(walletYanki, walletDestin));
        // When
        TestObserver<List<WalletYanki>> testObserver = walletYankiService.getAll().test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertComplete();
        testObserver.assertValue(response -> response.size() == 2);
    }

    @Test
    @DisplayName("Find Wallet by id not exits")
    void findByIdNotExist() {
        String notExist = "NOTEXIST";
        // Given
        when(walletYankiRepository.findById(notExist)).thenReturn(Optional.empty());
        // When
        TestObserver<WalletYanki> testObserver = walletYankiService.findById(notExist).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertError(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Find Wallet by id")
    void findByIdTest() {
        String idWallet = walletYanki.getId();
        // Given
        when(walletYankiRepository.findById(idWallet)).thenReturn(Optional.of(walletYanki));
        // When
        TestObserver<WalletYanki> testObserver = walletYankiService.findById(idWallet).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertValue(response -> response.getId().equals(idWallet));
    }

    @Test
    @DisplayName("Save wallet duplicate")
    void saveDuplicateTest() {
        // Given

        DuplicateKeyException duplicateKeyException = new DuplicateKeyException("Duplicate key error");
        when(walletYankiRepository.save(any(WalletYanki.class))).thenThrow(duplicateKeyException);

        // When
        TestObserver<WalletYanki> testObserver = walletYankiService.save(walletYanki).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertError(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Save wallet")
    void saveTest() {
        // Given
        when(walletYankiRepository.save(walletYanki)).thenReturn(walletYanki);
        // When
        TestObserver<WalletYanki> testObserver = walletYankiService.save(walletYanki).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertComplete();
        testObserver.assertValue(response -> response.getId().equals(walletYanki.getId()));
    }

    @Test
    @DisplayName("Update wallet yanki not found")
    void updateNotFoundTest() {
        String notExist = "NOTEXIST";
        // Given
        when(walletYankiRepository.findById(notExist)).thenReturn(Optional.empty());
        // When
        TestObserver<WalletYanki> testObserver = walletYankiService.update(notExist, walletYanki).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertError(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Update wallet yanki")
    void updateTest() {
        String idWallet = walletYanki.getId();
        // Given
        walletYanki.setEmail("modify@modify.com");
        when(walletYankiRepository.findById(idWallet)).thenReturn(Optional.of(walletYanki));
        when(walletYankiRepository.save(walletYanki)).thenReturn(walletYanki);
        // When
        TestObserver<WalletYanki> testObserver = walletYankiService.update(idWallet, walletYanki).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertValue(value -> value.getId().equals(walletYanki.getId()));
    }

    @Test
    @DisplayName("Delete by id when that not found")
    void deleteByIdNotFoundTest() {
        String notExist = "NOTEXIST";
        // Given
        when(walletYankiRepository.findById(notExist)).thenReturn(Optional.empty());
        // When
        TestObserver<Boolean> testObserver = walletYankiService.deleteById(notExist).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertValue(value -> !value);
    }

    @Test
    @DisplayName("Delete found wallet")
    void deleteById() {
        String idWallet = walletYanki.getId();
        // Given
        when(walletYankiRepository.findById(idWallet)).thenReturn(Optional.of(walletYanki));
        doNothing().when(walletYankiRepository).deleteById(walletYanki.getId());
        // When
        TestObserver<Boolean> testObserver = walletYankiService.deleteById(idWallet).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertValue(value -> value);
    }

    @Test
    @DisplayName("Assign debit card with wallet not exists")
    void assignDebitCardNotExistTest() {
        String notExist = "NOTEXIST";
        walletYanki.setId(notExist);
        // Given
        when(walletYankiRepository.findById(walletYanki.getId())).thenReturn(Optional.empty());
        // When
        TestObserver<WalletYanki> testObserver = walletYankiService.assignDebitCard(walletYanki).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertError(ResourceNotFoundException.class);

    }

    @Test
    @DisplayName("Assign debit card with wallet not approved")
    void assignDebitCardWalletNotApproved() {
        walletYanki.setStateWallet(WalletYanki.StateWallet.REJECTED);
        // Given
        when(walletYankiRepository.findById(walletYanki.getId())).thenReturn(Optional.of(walletYanki));
        // When
        TestObserver<WalletYanki> testObserver = walletYankiService.assignDebitCard(walletYanki).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertError(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Assign debit card and wallet with balance > 0")
    void assignDebitCardWithBalanceMoreZero() {
        // Given
        when(walletYankiRepository.findById(walletYanki.getId())).thenReturn(Optional.of(walletYanki));
        // When
        TestObserver<WalletYanki> testObserver = walletYankiService.assignDebitCard(walletYanki).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertError(IllegalArgumentException.class);
        testObserver.assertError(error ->
                error.getMessage().contains("The client must have balance 0 for this operation:"));
    }


    @Test
    @DisplayName("Assign debit card with wallet")
    void assignDebitCard() {
        walletYanki.setBalance(0.0);
        // Given
        when(walletYankiRepository.findById(walletYanki.getId())).thenReturn(Optional.of(walletYanki));
        doNothing().when(walletProducer).sendAssociationWallet(walletYanki.getId(), walletYanki.getIdDebitCard());
        when(walletYankiRepository.save(walletYanki)).thenReturn(walletYanki);
        // When
        TestObserver<WalletYanki> testObserver = walletYankiService.assignDebitCard(walletYanki).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertValue(value -> value.getStateWallet() == WalletYanki.StateWallet.PENDING);

    }

    @Test
    @DisplayName("Process response association from app bank")
    void processResponseAssociation() {
        ResponseAssociationWalletDto association = ResponseAssociationWalletDto.builder()
                .idWallet(walletYanki.getId())
                .state("APPROVE")
                .observation("Association is correct")
                .idDebitCard(walletYanki.getIdDebitCard())
                .balance(250.0).build();
        // Given
        when(walletYankiRepository.findById(walletYanki.getId())).thenReturn(Optional.of(walletYanki));
        when(walletYankiRepository.save(walletYanki)).thenReturn(walletYanki);
        // When
        TestObserver<ResponseAssociationWalletDto> testObserver =
                walletYankiService.processResponseAssociation(association).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertValue(response -> response.getState().equals("APPROVE"));
    }

    @Test
    @DisplayName("Process response association from app bank rejected")
    void processResponseAssociationRejected() {
        ResponseAssociationWalletDto association = ResponseAssociationWalletDto.builder()
                .idWallet(walletYanki.getId())
                .state("REJECTED")
                .observation("Debit card not exist")
                .idDebitCard(walletYanki.getIdDebitCard())
                .balance(0.0).build();
        // Given
        when(walletYankiRepository.findById(walletYanki.getId())).thenReturn(Optional.of(walletYanki));
        when(walletYankiRepository.save(walletYanki)).thenReturn(walletYanki);
        // When
        TestObserver<ResponseAssociationWalletDto> testObserver =
                walletYankiService.processResponseAssociation(association).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertValue(response -> response.getState().equals("REJECTED"));
    }

    @Test
    @DisplayName("Process response association from app bank Unknown")
    void processResponseAssociationUnknown() {
        ResponseAssociationWalletDto association = ResponseAssociationWalletDto.builder()
                .idWallet(walletYanki.getId())
                .state("ERROR")
                .observation("Debit card not exist")
                .idDebitCard(walletYanki.getIdDebitCard())
                .balance(0.0).build();
        // Given
        when(walletYankiRepository.findById(walletYanki.getId())).thenReturn(Optional.of(walletYanki));
        // When
        TestObserver<ResponseAssociationWalletDto> testObserver =
                walletYankiService.processResponseAssociation(association).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertError(IllegalArgumentException.class);
    }


    @Test
    @DisplayName("update balance in wallet from debit card")
    void updateBalanceWalletFromDebitCard() {
        // Given
        when(walletYankiRepository.findByIdDebitCard(walletYanki.getIdDebitCard()))
                .thenReturn(Optional.of(walletYanki));
        when(walletYankiRepository.save(walletYanki)).thenReturn(walletYanki);
        // When
        TestObserver<Void> testObserver =
                walletYankiService.updateBalanceWalletFromDebitCard(walletYanki.getIdDebitCard(), 300.0).test();
        // Then
        testObserver.awaitDone(1, TimeUnit.SECONDS);
        testObserver.assertComplete();

    }
}