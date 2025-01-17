package com.coinbase.cb.controllers;

import org.springframework.http.ResponseEntity;

public interface Query <Input, Output> {
    public ResponseEntity<Output> execute(Input input);
}
