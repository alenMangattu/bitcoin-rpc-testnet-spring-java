package com.coinbase.cb.controllers.services;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

import com.coinbase.cb.controllers.Query;
import org.springframework.stereotype.Service;

@Service
public class AccountInfo implements Query<Void, JSONObject>{

    @Override
    public ResponseEntity<JSONObject> execute(Void input) {
        
        
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }
    
}
