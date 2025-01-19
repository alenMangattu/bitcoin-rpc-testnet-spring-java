package com.coinbase.cb.controllers;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coinbase.cb.controllers.services.AccountInfo;
import java.util.Map;

@RestController
public class MainRestController {

    private final AccountInfo accountInfo;

    public MainRestController(AccountInfo accountInfo) {
        this.accountInfo = accountInfo;
    }

    @GetMapping("/account")
    public ResponseEntity<Map<String, Object>> getAccountInfo() {
        return this.accountInfo.execute(null);
    }

}
