package com.coinbase.cb.services.walletupdater;

import com.coinbase.cb.util.BtcRpcInterface;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class BitcoinSyncChecker {
    private static final Logger logger = LoggerFactory.getLogger(BitcoinSyncChecker.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public BitcoinSyncChecker(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isBitcoinCoreSynced() {
        String cachedSync = (String) redisTemplate.opsForValue().get("btc-sync-status");
        if (cachedSync != null) {
            return Boolean.parseBoolean(cachedSync);
        }

        JSONObject syncRequest = BtcRpcInterface.generateReqJson("sync", "getblockchaininfo");
        JSONObject response = BtcRpcInterface.callRpcMethod(syncRequest);
        
        if (response != null && (!response.has("error") || response.isNull("error"))) {
            JSONObject result = response.getJSONObject("result");
            boolean isSynced = !result.getBoolean("initialblockdownload");
            double verificationProgress = result.getDouble("verificationprogress");
            
            boolean syncStatus = isSynced && verificationProgress > 0.9999;
            redisTemplate.opsForValue().set("btc-sync-status", String.valueOf(syncStatus), 1, TimeUnit.MINUTES);
            
            if (!syncStatus) {
                logger.warn("Bitcoin Core not fully synced: {}% complete", 
                    String.format("%.2f", verificationProgress * 100));
            }
            return syncStatus;
        }
        
        logger.error("Failed to check Bitcoin Core sync status");
        return false;
    }
} 
