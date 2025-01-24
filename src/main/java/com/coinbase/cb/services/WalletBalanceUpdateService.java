package com.coinbase.cb.services;

import com.coinbase.cb.models.DbTables.Wallet;
import com.coinbase.cb.repository.WalletRepository;
import com.coinbase.cb.services.walletupdater.BitcoinSyncChecker;
import com.coinbase.cb.services.walletupdater.WalletBalanceChecker;
import com.coinbase.cb.services.walletupdater.WalletLoader;
import com.coinbase.cb.controllers.services.AccountInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class WalletBalanceUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(WalletBalanceUpdateService.class);
    // private static final String LAST_BLOCK_HEIGHT_KEY = "btc-last-block-height";
    private static final String LAST_PROCESSED_TXS_KEY = "btc-processed-txs";
    private static final int BATCH_SIZE = 1000;
    
    private final WalletRepository walletRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WalletLoader walletLoader;
    private final WalletBalanceChecker balanceChecker;
    private final BitcoinSyncChecker syncChecker;
    private final AccountInfo accountInfo;
    private ExecutorService executorService;
    private final ConcurrentHashMap<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    private final Set<String> processedTxs = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final long UPDATE_THRESHOLD = TimeUnit.MINUTES.toMillis(30);
    
    @Value("${wallet.updater.partition.id:0}")
    private int partitionId;
    
    @Value("${wallet.updater.partition.count:1}")
    private int totalPartitions;
    
    @Value("${wallet.updater.threads:10}")
    private int threadCount;

    public WalletBalanceUpdateService(
            WalletRepository walletRepository,
            RedisTemplate<String, Object> redisTemplate,
            WalletLoader walletLoader,
            WalletBalanceChecker balanceChecker,
            BitcoinSyncChecker syncChecker,
            AccountInfo accountInfo) {
        this.walletRepository = walletRepository;
        this.redisTemplate = redisTemplate;
        this.walletLoader = walletLoader;
        this.balanceChecker = balanceChecker;
        this.syncChecker = syncChecker;
        this.accountInfo = accountInfo;
    }

    @PostConstruct
    private void init() {
        // Load last processed transactions from Redis
        Set<Object> cachedTxs = redisTemplate.opsForSet().members(LAST_PROCESSED_TXS_KEY);
        if (cachedTxs != null) {
            cachedTxs.forEach(tx -> processedTxs.add(tx.toString()));
        }
        
        // Initialize executor service
        this.executorService = Executors.newFixedThreadPool(threadCount);
        
        // Load all wallets during startup
        walletLoader.loadAllWallets();
    }

    @Scheduled(fixedRate = 1000) // Poll every second
    @Transactional
    public void pollForUpdates() {
        if (!syncChecker.isBitcoinCoreSynced()) {
            return;
        }

        try {
            updateWalletBalances();
        } catch (Exception e) {
            logger.error("Error polling for updates: {}", e.getMessage(), e);
        }
    }

    private void updateWalletBalances() {
        int pageNumber = 0;
        AtomicBoolean hasMore = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger activeThreads = new AtomicInteger(0);
        Map<String, Double> batchUpdates = new ConcurrentHashMap<>();

        try {
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

                // Process wallets in batches to reduce database load
                List<List<Wallet>> batches = new ArrayList<>();
                for (int i = 0; i < wallets.size(); i += threadCount) {
                    batches.add(wallets.subList(i, Math.min(i + threadCount, wallets.size())));
                }

                CountDownLatch batchLatch = new CountDownLatch(batches.size());

                for (List<Wallet> batch : batches) {
                    activeThreads.incrementAndGet();
                    executorService.submit(() -> {
                        try {
                            processWalletBatch(batch, batchUpdates);
                        } finally {
                            activeThreads.decrementAndGet();
                            batchLatch.countDown();
                            if (!hasMore.get() && activeThreads.get() == 0) {
                                latch.countDown();
                            }
                        }
                    });
                }

                // Wait for current batch to complete before moving to next page
                batchLatch.await(1, TimeUnit.MINUTES);
                pageNumber++;
            }

            // Wait for all threads to complete
            latch.await(5, TimeUnit.MINUTES);

            // Batch update the database if there are changes
            if (!batchUpdates.isEmpty()) {
                List<String> walletNames = new ArrayList<>(batchUpdates.keySet());
                List<Wallet> walletsToUpdate = walletRepository.findAllByWalletNameIn(walletNames);
                
                walletsToUpdate.forEach(wallet -> {
                    Double newBalance = batchUpdates.get(wallet.getWalletName());
                    if (newBalance != null && !newBalance.equals(wallet.getBalance())) {
                        wallet.setBalance(newBalance);
                        // Clear account cache when balance changes
                        accountInfo.clearAccountCache(wallet.getUser().getEmail());
                    }
                });
                
                if (!walletsToUpdate.isEmpty()) {
                    walletRepository.saveAll(walletsToUpdate);
                    logger.info("Updated {} wallet balances in batch", walletsToUpdate.size());
                }
            }

        } catch (Exception e) {
            logger.error("Error updating wallet balances: {}", e.getMessage(), e);
        }
    }

    private void processWalletBatch(List<Wallet> wallets, Map<String, Double> batchUpdates) {
        for (Wallet wallet : wallets) {
            try {
                String walletName = wallet.getWalletName();
                
                // Skip if updated recently
                Long lastUpdate = lastUpdateTime.get(walletName);
                if (lastUpdate != null && System.currentTimeMillis() - lastUpdate < UPDATE_THRESHOLD) {
                    continue;
                }

                // Ensure wallet is loaded
                if (!walletLoader.isWalletLoaded(walletName)) {
                    walletLoader.loadWallet(walletName);
                }

                // Get balance
                Map<String, Double> balances = balanceChecker.getWalletBalances(walletName);
                if (balances != null) {
                    double confirmedBalance = balances.get("confirmed");
                    if (confirmedBalance != wallet.getBalance()) {
                        batchUpdates.put(walletName, confirmedBalance);
                        lastUpdateTime.put(walletName, System.currentTimeMillis());
                    }
                }
            } catch (Exception e) {
                logger.error("Error updating wallet {}: {}", wallet.getWalletName(), e.getMessage());
            }
        }
    }

    @Transactional
    public void updateWalletBalance(String walletAddress) {
        if (!syncChecker.isBitcoinCoreSynced()) {
            throw new RuntimeException("Cannot update balance - Bitcoin Core not fully synced");
        }
        
        Wallet wallet = walletRepository.findByAddress(walletAddress)
            .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletAddress));
            
        if (!walletLoader.isWalletLoaded(wallet.getWalletName())) {
            walletLoader.loadWallet(wallet.getWalletName());
        }
        
        Map<String, Double> balances = balanceChecker.getWalletBalances(wallet.getWalletName());
        if (balances != null) {
            double confirmedBalance = balances.get("confirmed");
            if (confirmedBalance != wallet.getBalance()) {
                wallet.setBalance(confirmedBalance);
                walletRepository.save(wallet);
                lastUpdateTime.put(wallet.getWalletName(), System.currentTimeMillis());
                // Clear account cache when balance changes
                accountInfo.clearAccountCache(wallet.getUser().getEmail());
            }
        }
    }
} 