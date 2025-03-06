package com.wallet.walletdigital.controller;

import com.wallet.walletdigital.model.WalletYanki;
import com.wallet.walletdigital.service.WalletYankiService;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletYankiControllerTest {

    @InjectMocks
    private WalletYankiController walletYankiController;
    @Mock
    private WalletYankiService walletYankiService;
    private WebTestClient webTestClient;

    private WalletYanki walletYanki, walletDestin;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(walletYankiController).build();

        walletYanki = WalletYanki.builder()
                .id("WALLET_01")
                .balance(200.0)
                .stateWallet(WalletYanki.StateWallet.APPROVE)
                .email("prueba@prueba.com")
                .hasDebitCard(false)
                .numberIdentity("76902100")
                .numberPhone("929549040")
                .idDebitCard("")
                .phoneImei("989899998898653")
                .build();

        walletDestin = WalletYanki.builder()
                .id("WALLET_02")
                .balance(0.0)
                .stateWallet(WalletYanki.StateWallet.APPROVE)
                .email("prueba1@prueba.com")
                .hasDebitCard(false)
                .numberIdentity("20750210")
                .numberPhone("929549041")
                .idDebitCard("")
                .phoneImei("989899995598693")
                .build();
    }

    @Test
    @DisplayName("Get All Wallets")
    void getAll() {
        List<WalletYanki> wallets = List.of(walletYanki, walletDestin);
        // Given
        when(walletYankiService.getAll()).thenReturn(Single.just(wallets));
        // When
        webTestClient.get().uri("/wallet")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(WalletYanki.class);
        verify(walletYankiService).getAll();
    }

    @Test
    @DisplayName("Find By id Wallet yanki")
    void findById() {
        // Given
        String idFound = walletYanki.getId();
        when(walletYankiService.findById(idFound)).thenReturn(Single.just(walletYanki));
        // Then
        webTestClient.get().uri("/wallet/{id}", idFound)
                .exchange()
                .expectBody(WalletYanki.class);
        verify(walletYankiService).findById(idFound);
    }

    @Test
    @DisplayName("Save a wallet")
    void saveTest() {
        // Given
        when(walletYankiService.save(walletYanki)).thenReturn(Single.just(walletYanki));
        // Then
        webTestClient.post().uri("/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(walletYanki)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WalletYanki.class);
        verify(walletYankiService).save(walletYanki);
    }

    @Test
    @DisplayName("Delete by id")
    void delete() {
        String idWallet = walletYanki.getId();
        // Given
        when(walletYankiService.deleteById(idWallet)).thenReturn(Single.just(true));
        // Then
        webTestClient.delete().uri("/wallet/{id}", idWallet)
                .exchange()
                .expectStatus().isNoContent();

        verify(walletYankiService).deleteById(idWallet);
    }

    @Test
    @DisplayName("Associate to debit card")
    void associateToDebitCard() {
        // Given
        when(walletYankiService.assignDebitCard(walletYanki)).thenReturn(Single.just(walletYanki));
        // Then
        webTestClient.post().uri("/wallet/associate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(walletYanki)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WalletYanki.class);
        verify(walletYankiService).assignDebitCard(walletYanki);
    }
}