package com.wallet.walletdigital.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.walletdigital.service.WalletYankiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class WalletConsumerTest {

    @InjectMocks
    private WalletConsumer walletConsumer;
    @Mock
    private WalletYankiService walletYankiService;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    /*@Test
    @DisplayName("Listener response associate")
    void listenerResponseAssociateTest() throws JsonProcessingException {
        // Given
        ResponseAssociationWalletDto response = ResponseAssociationWalletDto.builder()
                .idDebitCard("DEBITCARD1")
                .idWallet("WALLET_ID001")
                .observation("Test observation")
                .state("APPROVED")
                .balance(100.0)
                .idMessage("message123")
                .build();
        String message = objectMapper.writeValueAsString(response);

        when(walletYankiService.processResponseAssociation(response)).thenReturn(Single.just(response));
        walletConsumer.listenerResponseAssociate(message);
        verify(walletYankiService).processResponseAssociation(response);
    }

    @Test
    void listenerUpdateBalanceBankAccount() {
    }*/
}