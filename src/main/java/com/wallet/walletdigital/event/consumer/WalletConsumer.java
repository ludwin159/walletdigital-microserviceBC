package com.wallet.walletdigital.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.walletdigital.dto.ResponseAssociationWalletDto;
import com.wallet.walletdigital.model.MovementWallet;
import com.wallet.walletdigital.service.WalletYankiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class WalletConsumer {

    private final WalletYankiService walletYankiService;
    private final ObjectMapper objectMapper;
    public WalletConsumer(WalletYankiService walletYankiService,
                          ObjectMapper objectMapper) {
        this.walletYankiService = walletYankiService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "wallet-debit-card-response", groupId = "wallet-group")
    public void listenerResponseAssociate(String response) {
        try {
            log.info("Mensaje recibido: " + response);
            ResponseAssociationWalletDto responseObject = objectMapper.readValue(response, ResponseAssociationWalletDto.class);
            log.info("Mensaje recibido: " + responseObject);
            walletYankiService.processResponseAssociation(responseObject)
                    .subscribe();
        } catch (Exception e) {
            log.error("Error deserializing Kafka message in response association debit card: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "bank-account-balance-updated", groupId = "wallet-group")
    public void listenerUpdateBalanceBankAccount(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String idDebitCard = json.get("idDebitCard").asText();
            double newBalance = json.get("newBalance").asDouble();

            log.info("Received balance for update for account: {}, {}", idDebitCard, newBalance);
            walletYankiService.updateBalanceWalletFromDebitCard(idDebitCard, newBalance)
                    .doOnError(error -> log.error("Error updating balance: " + error.getMessage()))
                    .subscribe();

        } catch (Exception e){
            log.error("Error deserializing data in update balance bank account");
        }
    }

}
