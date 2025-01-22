package com.coinbase.cb.controllers.services;

import com.coinbase.cb.error.CustomErrorResponse;
import com.coinbase.cb.models.DTOs.SendRequestDTO;
import com.coinbase.cb.models.DTOs.TransactionDTO;
import com.coinbase.cb.models.DbTables.User;
import com.coinbase.cb.models.DbTables.Wallet;
import com.coinbase.cb.repository.TransactionRepository;
import com.coinbase.cb.repository.UserRespository;
import com.coinbase.cb.repository.WalletRepository;
import com.coinbase.cb.util.BtcRpcInterface;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SendBitcoinServiceTest {

    @Mock
    private UserRespository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private SendBitcoinService sendBitcoinService;

    private User sender;
    private User recipient;
    private Wallet senderWallet;
    private Wallet recipientWallet;
    private SendRequestDTO validRequest;
    private static final String MOCK_TXID = "mock_blockchain_txid";

    @BeforeEach
    void setUp() {
        // Setup security context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("sender@test.com");

        // Setup test data
        sender = new User();
        sender.setUser_id(1L);
        sender.setEmail("sender@test.com");
        
        recipient = new User();
        recipient.setUser_id(2L);
        recipient.setEmail("recipient@test.com");

        senderWallet = new Wallet();
        senderWallet.setBalance(1.0);
        senderWallet.setAddress("sender_address");
        senderWallet.setWalletName("sender_wallet");
        sender.setWallet(senderWallet);
        senderWallet.setUser(sender);

        recipientWallet = new Wallet();
        recipientWallet.setBalance(0.0);
        recipientWallet.setAddress("recipient_address");
        recipientWallet.setWalletName("recipient_wallet");
        recipient.setWallet(recipientWallet);
        recipientWallet.setUser(recipient);

        validRequest = new SendRequestDTO();
        validRequest.setRecipient("2");
        validRequest.setAmount(0.1);
        validRequest.setMessage("Test transaction");
    }

    @Test
    void whenSendingValidTransaction_thenSuccess() {
        // Arrange
        when(userRepository.findByEmailWithWallet("sender@test.com")).thenReturn(Optional.of(sender));
        when(userRepository.findByIdWithWallet(2L)).thenReturn(Optional.of(recipient));
        when(walletRepository.save(any(Wallet.class))).thenReturn(null);
        
        // Mock BTC RPC response
        JSONObject mockResponse = new JSONObject();
        mockResponse.put("result", MOCK_TXID);
        mockResponse.putOpt("error", JSONObject.NULL);
        
        // Use mockStatic for BtcRpcInterface if it's a utility class
        when(BtcRpcInterface.callRpcMethod(any(), any())).thenReturn(mockResponse);

        // Act
        ResponseEntity<TransactionDTO> response = sendBitcoinService.execute(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        TransactionDTO transactionDTO = response.getBody();
        assertNotNull(transactionDTO);
        assertEquals(MOCK_TXID, transactionDTO.blockchainTxid());
        assertEquals(sender.getUser_id(), transactionDTO.from());
        assertEquals(recipient.getUser_id(), transactionDTO.to());
        assertEquals(validRequest.getAmount(), transactionDTO.amount());
        assertEquals(validRequest.getMessage(), transactionDTO.comment());

        // Verify wallet updates
        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void whenSenderHasInsufficientBalance_thenThrowException() {
        // Arrange
        senderWallet.setBalance(0.05); // Set balance lower than transaction amount
        when(userRepository.findByEmailWithWallet("sender@test.com")).thenReturn(Optional.of(sender));
        when(userRepository.findByIdWithWallet(2L)).thenReturn(Optional.of(recipient));

        // Act & Assert
        CustomErrorResponse exception = assertThrows(CustomErrorResponse.class, () -> {
            sendBitcoinService.execute(validRequest);
        });
        assertEquals("Insufficient balance", exception.getMessage());
    }

    @Test
    void whenRecipientNotFound_thenThrowException() {
        // Arrange
        when(userRepository.findByEmailWithWallet("sender@test.com")).thenReturn(Optional.of(sender));
        when(userRepository.findByIdWithWallet(2L)).thenReturn(Optional.empty());

        // Act & Assert
        CustomErrorResponse exception = assertThrows(CustomErrorResponse.class, () -> {
            sendBitcoinService.execute(validRequest);
        });
        assertEquals("Recipient not found", exception.getMessage());
    }

    @Test
    void whenBitcoinRPCFails_thenThrowException() {
        // Arrange
        when(userRepository.findByEmailWithWallet("sender@test.com")).thenReturn(Optional.of(sender));
        when(userRepository.findByIdWithWallet(2L)).thenReturn(Optional.of(recipient));
        
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("error", "RPC error");
        errorResponse.putOpt("result", JSONObject.NULL);
        when(BtcRpcInterface.callRpcMethod(any(), any())).thenReturn(errorResponse);

        // Act & Assert
        CustomErrorResponse exception = assertThrows(CustomErrorResponse.class, () -> {
            sendBitcoinService.execute(validRequest);
        });
        assertTrue(exception.getMessage().contains("Transaction failed"));
    }
} 