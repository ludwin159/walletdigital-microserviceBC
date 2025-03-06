package com.wallet.walletdigital.controller;

import com.wallet.walletdigital.model.MovementWallet;
import com.wallet.walletdigital.service.MovementWalletService;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.wallet.walletdigital.model.MovementWallet.TypeMovementWallet.MAKE_PAYMENT;
import static com.wallet.walletdigital.model.MovementWallet.TypeMovementWallet.RECEIPT_PAYMENT;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovementWalletControllerTest {
    @InjectMocks
    private MovementWalletController movementWalletController;

    @Mock
    private MovementWalletService movementWalletService;

    private WebTestClient webTestClient;
    private MovementWallet movementWallet1;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(movementWalletController).build();
        movementWallet1 = MovementWallet.builder()
                .id("MOVEMENT1")
                .idWallet("IDWALLET")
                .type(RECEIPT_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.APPROVE)
                .amount(20.0)
                .numberDestin("")
                .createdAt(new Date())
                .idDebitCard("")
                .description("Description")
                .build();
    }

    @Test
    @DisplayName("Get all movements controller")
    void getAllTest() {
        // Given
        List<MovementWallet> movementsList = Arrays.asList(new MovementWallet(), new MovementWallet());
        when(movementWalletService.getAll()).thenReturn(Single.just(movementsList));
        // When// Then
        webTestClient.get()
                .uri("/movement-wallet")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MovementWallet.class)
                .hasSize(2);
        verify(movementWalletService, times(1)).getAll();
    }

    @Test
    @DisplayName("Find movements by id")
    void findById() {
        // Given
        when(movementWalletService.findById(movementWallet1.getId())).thenReturn(Single.just(movementWallet1));
        // When// Then
        webTestClient.get()
                .uri("/movement-wallet/{id}", movementWallet1.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(MovementWallet.class);
        verify(movementWalletService, times(1)).findById(movementWallet1.getId());
    }

    @Test
    @DisplayName("Save movement wallet")
    void saveTest() {

        movementWallet1 = MovementWallet.builder()
                .id("MOVEMENT1")
                .idWallet("IDWALLET")
                .type(MAKE_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.APPROVE)
                .amount(20.0)
                .numberDestin("929549040")
                .createdAt(Date.from(Instant.now()))
                .idDebitCard("")
                .description("Description")
                .build();

        when(movementWalletService.save(any(MovementWallet.class))).thenReturn(Single.just(movementWallet1));

        webTestClient.post()
                .uri("/movement-wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(movementWallet1)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(MovementWallet.class);
        verify(movementWalletService, times(1)).save(movementWallet1);
    }

    @Test
    @DisplayName("Delete movement by id")
    void deleteTest() {
        String idDelete = movementWallet1.getId();
        // Given
        when(movementWalletService.deleteById(idDelete)).thenReturn(Single.just(true));
        // When
        webTestClient.delete().uri("/movement-wallet/{id}", idDelete)
                .exchange()
                .expectStatus().isNoContent();
        verify(movementWalletService).deleteById(idDelete);
    }
}