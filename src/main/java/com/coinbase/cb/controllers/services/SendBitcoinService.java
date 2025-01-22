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
                sender.getWallet().getWalletName(),
                "sendtoaddress",
                recipient.getWallet().getAddress(),
                request.getAmount(),
                request.getMessage()
        );

        JSONObject response = BtcRpcInterface.callRpcMethod(sendRequest, sender.getWallet().getWalletName());

        if (response == null) {
            throw new CustomErrorResponse(ErrorMessages.TRANSACTION_FAILED.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Check for errors in response
        if (response.has("error") && !response.isNull("error")) {
            JSONObject error = response.optJSONObject("error");
            String errorMessage = error != null ? error.optString("message", "Unknown error") : 
                                response.get("error").toString();
            throw new CustomErrorResponse(
                    ErrorMessages.TRANSACTION_FAILED.getMessage() + ": " + errorMessage,
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        // Get the transaction ID from response
        // Bitcoin Core's sendtoaddress returns the txid directly as a string in the result field
        String blockchainTxid;
        try {
            blockchainTxid = response.getString("result");
        } catch (Exception e) {
            throw new CustomErrorResponse(
                    ErrorMessages.TRANSACTION_FAILED.getMessage() + ": Invalid response format",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        if (blockchainTxid == null || blockchainTxid.trim().isEmpty()) {
            throw new CustomErrorResponse(
                    ErrorMessages.TRANSACTION_FAILED.getMessage() + ": No transaction ID returned",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        System.out.println(blockchainTxid); 
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
        transaction.setBlockchainTxid(blockchainTxid);
        
        transaction = transactionRepository.save(transaction);

        return ResponseEntity.ok(new TransactionDTO(
                transaction.getTxid(),
                sender.getUser_id(),
                recipient.getUser_id(),
                transaction.getAmount(),
                transaction.getMessage(),
                transaction.getBlockchainTxid()
        ));
    }
} 