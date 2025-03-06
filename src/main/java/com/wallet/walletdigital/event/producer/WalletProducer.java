package com.wallet.walletdigital.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.walletdigital.dto.SendAssociation;
import com.wallet.walletdigital.model.MovementWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WalletProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final Logger log = LoggerFactory.getLogger(WalletProducer.class);
    private final ObjectMapper objectMapper;

    public WalletProducer(KafkaTemplate<String, String> kafkaTemplate,
                          ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendAssociationWallet(String idWallet, String idDebitCard) {
        try {
            SendAssociation sendAssociation = SendAssociation.builder()
                    .idWallet(idWallet)
                    .idDebitCard(idDebitCard)
                    .build();
            String message = objectMapper.writeValueAsString(sendAssociation);
            kafkaTemplate.send("wallet-debit-card-association", message);
            log.info("Request of association send: " + message);
        } catch (Exception e) {
            log.error("Error serializing message in association", e);
        }
    }

    public void sendMakePaymentWithDebitCard(MovementWallet movementOrigin, MovementWallet movementDestin) {
        try {
            List<MovementWallet> movements = List.of(movementOrigin, movementDestin);
            String message = objectMapper.writeValueAsString(movements);
            kafkaTemplate.send("payment-with-debit-card", message);
            log.info("Send payment with debit card: {}, {} ",
                    movementOrigin.getIdDebitCard(),
                    movementDestin.getIdDebitCard());
        } catch (Exception e) {
            log.error("Error serializing data in send Make payment with debit card {}", e);
        }
    }
}
