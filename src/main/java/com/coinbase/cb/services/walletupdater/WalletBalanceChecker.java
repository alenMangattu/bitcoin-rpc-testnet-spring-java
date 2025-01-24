package com.coinbase.cb.services.walletupdater;

import com.coinbase.cb.util.BtcRpcInterface;
import org.json.JSONObject;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class WalletBalanceChecker {
    // private static final Logger logger = LoggerFactory.getLogger(WalletBalanceChecker.class);
    private static final String WALLET_BALANCE_CACHE = "wallet-balances";
    private static final int MIN_CONFIRMATIONS = 3;

    private final RedisTemplate<String, Object> redisTemplate;

    public WalletBalanceChecker(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Double> getWalletBalances(String walletName) {
        // Get cached balance if available
        Double cachedBalance = (Double) redisTemplate.opsForValue()
            .get(WALLET_BALANCE_CACHE + ":" + walletName);
        if (cachedBalance != null) {
            Map<String, Double> balances = new HashMap<>();
            balances.put("confirmed", cachedBalance);
            balances.put("unconfirmed", 0.0);
            return balances;
        }

        // Get confirmed balance (with MIN_CONFIRMATIONS)
        JSONObject confirmedRequest = BtcRpcInterface.generateReqJson(
            walletName,
            "getbalance",
            "*",
            MIN_CONFIRMATIONS
        );
        
        JSONObject confirmedResponse = BtcRpcInterface.callRpcMethod(confirmedRequest, walletName);
        
        if (confirmedResponse != null && (!confirmedResponse.has("error") || confirmedResponse.isNull("error"))) {
            double confirmedBalance = confirmedResponse.getDouble("result");
            Map<String, Double> balances = new HashMap<>();
            balances.put("confirmed", confirmedBalance);
            balances.put("unconfirmed", 0.0);
            
            // Cache the balance
            redisTemplate.opsForValue()
                .set(WALLET_BALANCE_CACHE + ":" + walletName, 
                    confirmedBalance, 
                    30, 
                    TimeUnit.MINUTES);
                    
            return balances;
        }
        
        return null;
    }
} 
