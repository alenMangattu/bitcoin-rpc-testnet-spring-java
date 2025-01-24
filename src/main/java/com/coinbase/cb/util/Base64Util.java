package com.coinbase.cb.util;

import com.coinbase.cb.error.CustomErrorResponse;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Util {

    public static String decode(String input) {
        if (input == null || input.length() == 0) {
            return null;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(input);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Handle invalid Base64 input
            throw new CustomErrorResponse("Invalid Base64 encoded string: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public static String encode(String input) {
        if (input == null || input.length() == 0) {
            return null;
        }

        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(bytes);
    }
}

