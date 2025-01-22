package com.coinbase.cb.services;

import com.coinbase.cb.models.DbTables.Wallet;
import com.coinbase.cb.repository.WalletRepository;
import com.coinbase.cb.util.BtcRpcInterface;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WalletBalanceUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(WalletBalanceUpdateService.class);
    
    private final WalletRepository walletRepository;

    public WalletBalanceUpdateService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    private boolean ensureWalletLoaded(String walletName) {
        // First check if wallet is already loaded using getwalletinfo
        JSONObject infoRequest = BtcRpcInterface.generateReqJson(
            walletName,
            "getwalletinfo"
        );
        
        JSONObject infoResponse = BtcRpcInterface.callRpcMethod(infoRequest, walletName);
        
        // If we get a successful response or an "already loaded" error, the wallet is loaded
        if (infoResponse != null) {
            if (!infoResponse.has("error") || infoResponse.isNull("error")) {
                logger.debug("Wallet {} is already loaded", walletName);
                return true;
            }
            
            JSONObject error = infoResponse.getJSONObject("error");
            if (error.getInt("code") == -35 && error.getString("message").contains("already loaded")) {
                logger.debug("Wallet {} is already loaded (confirmed by error)", walletName);
                return true;
            }
        }

        // If wallet isn't loaded, try to load it
        JSONObject loadRequest = BtcRpcInterface.generateReqJson(
            walletName,
            "loadwallet",
            walletName
        );
        
        JSONObject loadResponse = BtcRpcInterface.callRpcMethod(loadRequest);
        if (loadResponse != null) {
            if (!loadResponse.has("error") || loadResponse.isNull("error")) {
                logger.info("Successfully loaded wallet: {}", walletName);
                return true;
            }
            
            JSONObject error = loadResponse.getJSONObject("error");
            // If wallet is already loaded, that's fine
            if (error.getInt("code") == -35 && error.getString("message").contains("already loaded")) {
                logger.debug("Wallet {} was already loaded", walletName);
                return true;
            }
            
            logger.error("Failed to load wallet {}: {}", walletName, error.getString("message"));
            return false;
        }
        
        logger.error("Failed to load wallet {}: No response from Bitcoin Core", walletName);
        return false;
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void updateWalletBalances() {
        List<Wallet> wallets = walletRepository.findAll();
        
        for (Wallet wallet : wallets) {
            try {
                String walletName = wallet.getWalletName();
                
                // Ensure wallet is loaded before checking balance
                if (!ensureWalletLoaded(walletName)) {
                    continue;
                }

                // Get wallet balance from Bitcoin Core
                JSONObject balanceRequest = BtcRpcInterface.generateReqJson(
                    walletName,
                    "getbalance",
                    "*",  // dummy parameter for backward compatibility
                    0     // minconf parameter
                );
                
                JSONObject response = BtcRpcInterface.callRpcMethod(balanceRequest, walletName);
                
                if (response != null && (!response.has("error") || response.isNull("error"))) {
                    // Convert BTC amount to satoshis (1 BTC = 100,000,000 satoshis)
                    double btcBalance = response.getDouble("result");
                    int satoshiBalance = (int)(btcBalance * 100000000);
                    
                    if (satoshiBalance != wallet.getBalance()) {
                        logger.info("Updating balance for wallet {}: {} -> {} satoshis", 
                            walletName, wallet.getBalance(), satoshiBalance);
                        wallet.setBalance(satoshiBalance);
                        walletRepository.save(wallet);
                    }
                } else if (response != null && response.has("error") && !response.isNull("error")) {
                    JSONObject error = response.getJSONObject("error");
                    logger.error("Failed to get balance for wallet {}: {}", 
                        walletName, error.getString("message"));
                } else {
                    logger.error("Failed to get balance for wallet {}: No response from Bitcoin Core", 
                        walletName);
                }
            } catch (Exception e) {
                logger.error("Error updating balance for wallet {}: {}", 
                    wallet.getWalletName(), e.getMessage(), e);
            }
        }
    }

    // Method to force an immediate balance update for a specific wallet
    @Transactional
    public void updateWalletBalance(String walletAddress) {
        Wallet wallet = walletRepository.findByAddress(walletAddress)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletAddress));
            
        String walletName = wallet.getWalletName();
        if (!ensureWalletLoaded(walletName)) {
            throw new RuntimeException("Failed to load wallet: " + walletName);
        }

        JSONObject balanceRequest = BtcRpcInterface.generateReqJson(
            walletName,
            "getbalance",
            "*",  // dummy parameter
            0     // minconf parameter
        );
        
        JSONObject response = BtcRpcInterface.callRpcMethod(balanceRequest, walletName);
        
        if (response != null && (!response.has("error") || response.isNull("error"))) {
            double btcBalance = response.getDouble("result");
            int satoshiBalance = (int)(btcBalance * 100000000);
            wallet.setBalance(satoshiBalance);
            walletRepository.save(wallet);
        } else if (response != null && response.has("error") && !response.isNull("error")) {
            JSONObject error = response.getJSONObject("error");
            throw new RuntimeException("Failed to get balance for wallet: " + error.getString("message"));
        } else {
            throw new RuntimeException("Failed to get balance for wallet: No response from Bitcoin Core");
        }
    }
} 