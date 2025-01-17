package com.coinbase.cb.util.validators;

import com.coinbase.cb.error.CustomErrorResponse;
import com.coinbase.cb.error.ErrorMessages;
import com.coinbase.cb.models.DbTables.User;
import com.coinbase.cb.util.RegexUtils;
import org.springframework.http.HttpStatus;

public class LoginInputValidator {

    public static void validate(User loginRequest) {
        // Validate required fields
        if (loginRequest.getEmail() == null || loginRequest.getEmail().isEmpty() 
            || loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
            throw new CustomErrorResponse(ErrorMessages.FORM_NOT_COMPLETE.getMessage(), HttpStatus.BAD_REQUEST);
        }

        if (!RegexUtils.isValidEmail(loginRequest.getEmail())) {
            throw new CustomErrorResponse(ErrorMessages.EMAIL_NOT_VALID.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
