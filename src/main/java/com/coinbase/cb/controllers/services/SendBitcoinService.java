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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Service responsible for handling Bitcoin send operations.
 * Implements atomic transactions for both database updates and blockchain operations.
 * Uses pessimistic locking to prevent concurrent modifications of the same accounts.
 */
@Service
public class SendBitcoinService implements Query<SendRequestDTO, TransactionDTO> {

    private final UserRespository userRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final AccountInfo accountInfo;

    public SendBitcoinService(UserRespository userRepository, 
                             TransactionRepository transactionRepository,
                             WalletRepository walletRepository,
                             AccountInfo accountInfo) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.accountInfo = accountInfo;
    }

    /**
     * Executes a Bitcoin send operation with full transaction safety.
     * 
     * The method ensures atomicity by:
     * 1. Locking both sender and recipient records
     * 2. Verifying balances
     * 3. Creating a pending transaction record
     * 4. Executing the blockchain transaction
     * 5. Updating balances and transaction status
     * 
     * If any step fails, the entire transaction is rolled back.
     * 
     * @param request The send request containing amount, recipient, and message
     * @return ResponseEntity containing the transaction details
     * @throws CustomErrorResponse for various error conditions (insufficient balance, user not found, etc.)
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<TransactionDTO> execute(SendRequestDTO request) {
        // Verify transaction management is active
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new CustomErrorResponse("Transaction not properly initialized", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();

        // Lock both sender and recipient records for update
        User sender = userRepository.findByEmailWithWalletForUpdate(currentUserEmail)
                .orElseThrow(() -> new CustomErrorResponse(ErrorMessages.SENDER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));

        // Find and lock recipient by userId or wallet address
        User recipient;
        try {
            Long recipientId = Long.parseLong(request.getRecipient());
            recipient = userRepository.findByIdWithWalletForUpdate(recipientId)
                    .orElseThrow(() -> new CustomErrorResponse(ErrorMessages.RECIPIENT_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        } catch (NumberFormatException e) {
            recipient = userRepository.findByWalletAddressForUpdate(request.getRecipient())
                    .orElseThrow(() -> new CustomErrorResponse(ErrorMessages.RECIPIENT_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        }

        // Verify sender has sufficient balance
        if (sender.getWallet().getBalance() < request.getAmount()) {
            throw new CustomErrorResponse(ErrorMessages.INSUFFICIENT_BALANCE.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // First record the pending transaction
        Transaction transaction = new Transaction();
        transaction.setFromUser(sender);
        transaction.setToUser(recipient);
        transaction.setAmount(request.getAmount());
        transaction.setMessage(request.getMessage());
        transaction = transactionRepository.save(transaction);

        // Execute Bitcoin RPC transaction
        try {
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

            // Validate RPC response
            if (response.has("error") && !response.isNull("error")) {
                JSONObject error = response.optJSONObject("error");
                String errorMessage = error != null ? error.optString("message", "Unknown error") : 
                                    response.get("error").toString();
                throw new CustomErrorResponse(
                        ErrorMessages.TRANSACTION_FAILED.getMessage() + ": " + errorMessage,
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

            String blockchainTxid = response.getString("result");
            if (blockchainTxid == null || blockchainTxid.trim().isEmpty()) {
                throw new CustomErrorResponse(
                        ErrorMessages.TRANSACTION_FAILED.getMessage() + ": No transaction ID returned",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }

            // Update transaction with blockchain txid
            transaction.setBlockchainTxid(blockchainTxid);
            transaction = transactionRepository.save(transaction);

            // Update wallet balances
            Wallet senderWallet = sender.getWallet();
            Wallet recipientWallet = recipient.getWallet();
            
            senderWallet.setBalance(senderWallet.getBalance() - request.getAmount());
            recipientWallet.setBalance(recipientWallet.getBalance() + request.getAmount());
            
            walletRepository.save(senderWallet);
            walletRepository.save(recipientWallet);

            // Clear account caches
            accountInfo.clearAccountCache(sender.getEmail());
            accountInfo.clearAccountCache(recipient.getEmail());

            return ResponseEntity.ok(new TransactionDTO(
                    transaction.getTxid(),
                    sender.getUser_id(),
                    recipient.getUser_id(),
                    transaction.getAmount(),
                    transaction.getMessage(),
                    transaction.getBlockchainTxid()
            ));

        } catch (Exception e) {
            // Any exception triggers a full transaction rollback
            throw new CustomErrorResponse(
                    ErrorMessages.TRANSACTION_FAILED.getMessage() + ": " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
} 
