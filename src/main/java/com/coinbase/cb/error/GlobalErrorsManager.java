package com.coinbase.cb.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalErrorsManager {

    @ExceptionHandler(CustomErrorResponse.class)
    @ResponseBody
    public ResponseEntity<String> handleCustomError(CustomErrorResponse error) {
        return ResponseEntity
            .status(error.getStatus())
            .body(error.getMessage());
    }
}
