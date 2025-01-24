package com.coinbase.cb.services.walletupdater;

import com.coinbase.cb.models.DbTables.Wallet;
import com.coinbase.cb.repository.WalletRepository;
import com.coinbase.cb.util.BtcRpcInterface;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WalletLoader {
    private static final Logger logger = LoggerFactory.getLogger(WalletLoader.class);
    private static final int BATCH_SIZE = 1000;
    private final WalletRepository walletRepository;
    private ExecutorService executorService;
    
    @Value("${wallet.updater.partition.id:0}")
    private int partitionId;
    
    @Value("${wallet.updater.partition.count:1}")
    private int totalPartitions;
    
    @Value("${wallet.updater.threads:10}")
    private int threadCount;

    public WalletLoader(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @PostConstruct
    private void init() {
        this.executorService = Executors.newFixedThreadPool(threadCount);
    }

    public void loadAllWallets() {
        try {
            int pageNumber = 0;
            AtomicBoolean hasMore = new AtomicBoolean(true);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger activeThreads = new AtomicInteger(0);

            while (hasMore.get()) {
                Page<Wallet> walletPage = walletRepository.findAllByPartition(
                    partitionId, 
                    totalPartitions,
                    PageRequest.of(pageNumber, BATCH_SIZE)
                );
                
                List<Wallet> wallets = walletPage.getContent();
                if (wallets.isEmpty()) {
                    hasMore.set(false);
                    continue;
                }

                // Wait if too many active threads
                while (activeThreads.get() >= threadCount) {
                    Thread.sleep(100);
                }

                activeThreads.incrementAndGet();
                executorService.submit(() -> {
                    try {
                        processWalletBatch(wallets);
                    } finally {
                        activeThreads.decrementAndGet();
                        if (!hasMore.get() && activeThreads.get() == 0) {
                            latch.countDown();
                        }
                    }
                });

                pageNumber++;
            }

            // Wait for all threads to complete
            latch.await(5, TimeUnit.MINUTES);
            
        } catch (Exception e) {
            logger.error("Error loading wallets during startup: {}", e.getMessage());
        }
    }

    private void processWalletBatch(List<Wallet> wallets) {
        for (Wallet wallet : wallets) {
            try {
                if (!isWalletLoaded(wallet.getWalletName())) {
                    loadWallet(wallet.getWalletName());
                    logger.info("Successfully loaded wallet: {}", wallet.getWalletName());
                } else {
                    logger.debug("Wallet already loaded: {}", wallet.getWalletName());
                }
            } catch (Exception e) {
                logger.error("Error loading wallet {}: {}", wallet.getWalletName(), e.getMessage());
            }
        }
    }

    public boolean isWalletLoaded(String walletName) {
        JSONObject infoRequest = BtcRpcInterface.generateReqJson(walletName, "getwalletinfo");
        JSONObject infoResponse = BtcRpcInterface.callRpcMethod(infoRequest, walletName);
        
        if (infoResponse != null) {
            return !infoResponse.has("error") || infoResponse.isNull("error");
        }
        return false;
    }

    public boolean loadWallet(String walletName) {
        // Skip if wallet is already loaded
        if (isWalletLoaded(walletName)) {
            return true;
        }

        JSONObject loadRequest = BtcRpcInterface.generateReqJson(walletName, "loadwallet", walletName);
        JSONObject loadResponse = BtcRpcInterface.callRpcMethod(loadRequest);
        
        if (loadResponse != null) {
            if (!loadResponse.has("error") || loadResponse.isNull("error")) {
                logger.info("Successfully loaded wallet: {}", walletName);
                return true;
            }
            
            JSONObject error = loadResponse.getJSONObject("error");
            if (error.getInt("code") == -35 && error.getString("message").contains("already loaded")) {
                return true;
            }
            
            // Try to create wallet if it doesn't exist
            if (error.getInt("code") == -18) { // Wallet not found
                return createAndLoadWallet(walletName);
            }
        }
        return false;
    }

    private boolean createAndLoadWallet(String walletName) {
        JSONObject createRequest = BtcRpcInterface.generateReqJson(walletName, "createwallet", walletName);
        JSONObject createResponse = BtcRpcInterface.callRpcMethod(createRequest);
        
        if (createResponse != null && (!createResponse.has("error") || createResponse.isNull("error"))) {
            logger.info("Created new wallet: {}", walletName);
            return true;
        }
        return false;
    }
} 