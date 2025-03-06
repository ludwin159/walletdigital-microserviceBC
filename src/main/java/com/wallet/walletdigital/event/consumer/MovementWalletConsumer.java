package com.wallet.walletdigital.event.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.walletdigital.model.MovementWallet;
import com.wallet.walletdigital.service.MovementWalletService;
import com.wallet.walletdigital.service.WalletYankiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MovementWalletConsumer {

    private final MovementWalletService movementWalletService;
    private final ObjectMapper objectMapper;

    public MovementWalletConsumer(MovementWalletService movementWalletService,
                                  ObjectMapper objectMapper) {
        this.movementWalletService = movementWalletService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-with-debit-card-confirmation", groupId = "wallet-group")
    public void listenerConfirmationPaymentWithDebitCard(String message) {
        try {
            List<MovementWallet> movementsWallet = objectMapper.readValue(message, new TypeReference<List<MovementWallet>>() {});
            movementWalletService.processMovementsWithDebitCard(movementsWallet)
                    .subscribe();
        } catch (Exception e) {
            log.error("Error deserializing data in confirmation payment wallet with debit card");
        }
    }
}
