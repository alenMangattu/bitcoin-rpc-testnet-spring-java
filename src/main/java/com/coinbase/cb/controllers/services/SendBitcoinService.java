package com.coinbase.cb.controllers.services;

import com.coinbase.cb.controllers.Query;
import com.coinbase.cb.error.CustomErrorResponse;
import com.coinbase.cb.error.ErrorMessages;
import com.coinbase.cb.models.DTOs.SendRequestDTO;
import com.coinbase.cb.models.DTOs.TransactionDTO;
import com.coinbase.cb.models.DbTables.Transaction;
import com.coinbase.cb.models.DbTables.User;
import com.coinbase.cb.models.DbTables.Wallet;
import com.coinbase.cb.repository.TransactionRepository;
import com.coinbase.cb.repository.UserRespository;
import com.coinbase.cb.repository.WalletRepository;
import com.coinbase.cb.util.BtcRpcInterface;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class SendBitcoinService implements Query<SendRequestDTO, TransactionDTO> {

    private final UserRespository userRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public SendBitcoinService(UserRespository userRepository, 
                             TransactionRepository transactionRepository,
                             WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public ResponseEntity<TransactionDTO> execute(SendRequestDTO request) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();

        User sender = userRepository.findByEmailWithWallet(currentUserEmail)
                .orElseThrow(() -> new CustomErrorResponse(ErrorMessages.SENDER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        // Find recipient by userId or wallet address
        User recipient;
        try {
            Long recipientId = Long.parseLong(request.getRecipient());
            recipient = userRepository.findByIdWithWallet(recipientId)
                    .orElseThrow(() -> new CustomErrorResponse(ErrorMessages.RECIPIENT_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        } catch (NumberFormatException e) {
            // If not a userId, treat as wallet address
            recipient = userRepository.findByWalletAddress(request.getRecipient())
                    .orElseThrow(() -> new CustomErrorResponse(ErrorMessages.RECIPIENT_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        }

        // Verify sender has sufficient balance
        if (sender.getWallet().getBalance() < request.getAmount()) {
            throw new CustomErrorResponse(ErrorMessages.INSUFFICIENT_BALANCE.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // Create Bitcoin transaction using RPC
        JSONObject sendRequest = BtcRpcInterface.generateReqJson(
                currentUserEmail,
                "sendtoaddress",
                recipient.getWallet().getAddress(),
                request.getAmount(),
                request.getMessage()
        );

        JSONObject response = BtcRpcInterface.callRpcMethod(sendRequest, sender.getWallet().getWalletName());

        if (response == null || response.has("error")) {
            throw new CustomErrorResponse(
                    ErrorMessages.TRANSACTION_FAILED.getMessage() + (response != null ? ": " + response.getJSONObject("error").getString("message") : ""),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        // Update balances
        Wallet senderWallet = sender.getWallet();
        Wallet recipientWallet = recipient.getWallet();
        
        senderWallet.setBalance(senderWallet.getBalance() - request.getAmount());
        recipientWallet.setBalance(recipientWallet.getBalance() + request.getAmount());
        
        // Save updated wallets to database
        walletRepository.save(senderWallet);
        walletRepository.save(recipientWallet);

        // Record transaction
        Transaction transaction = new Transaction();
        transaction.setFromUser(sender);
        transaction.setToUser(recipient);
        transaction.setAmount(request.getAmount());
        transaction.setMessage(request.getMessage());
        
        transaction = transactionRepository.save(transaction);

        return ResponseEntity.ok(new TransactionDTO(
                transaction.getTxid(),
                sender.getUser_id(),
                recipient.getUser_id(),
                transaction.getAmount(),
                transaction.getMessage()
        ));
    }
} 