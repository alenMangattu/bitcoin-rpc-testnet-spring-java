package com.coinbase.cb.util.validators;

import com.coinbase.cb.error.CustomErrorResponse;
import com.coinbase.cb.error.ErrorMessages;
import com.coinbase.cb.models.DbTables.User;
import com.coinbase.cb.repository.UserRespository;
import com.coinbase.cb.util.Base64Util;
import com.coinbase.cb.util.RegexUtils;
import org.springframework.http.HttpStatus;

public class RegisterInputValidator {

    private static UserRespository userRespository;

    public static void setUserRepository(UserRespository repository) {
        userRespository = repository;
    }

    public static void validate(User user) {
        if (userRespository == null) {
            throw new IllegalStateException("UserRepository has not been initialized");
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()
                || user.getPassword() == null || user.getPassword().isEmpty()
                || user.getFirstName() == null || user.getFirstName().isEmpty()
                || user.getLastName() == null || user.getLastName().isEmpty()
        ) {
            throw new CustomErrorResponse(ErrorMessages.FORM_NOT_COMPLETE.getMessage(), HttpStatus.BAD_REQUEST);
        }

        String decodedPassword = Base64Util.decode(user.getPassword());
        System.out.println("Original Base64 password: " + user.getPassword());
        System.out.println("Decoded password: " + decodedPassword);
        System.out.println("Password validation result: " + RegexUtils.isValidPassword(decodedPassword));
        
        if (!RegexUtils.isValidPassword(decodedPassword)) {
            throw new CustomErrorResponse(ErrorMessages.PASSWORD_NOT_VALID.getMessage(), HttpStatus.BAD_REQUEST);
        }

        //Check if the first and last names only contains charecters
        if (!RegexUtils.onlycharecters(user.getFirstName()) || !RegexUtils.onlycharecters(user.getLastName())) {
            throw new CustomErrorResponse(ErrorMessages.ONLY_CHARACTERS_ALLOWED.getMessage(), HttpStatus.BAD_REQUEST);
        }

        //Check if the email is valid
        if (!RegexUtils.isValidEmail(user.getEmail())) {
            throw new CustomErrorResponse(ErrorMessages.EMAIL_NOT_VALID.getMessage(), HttpStatus.BAD_REQUEST);
        }

        boolean doesUserExist = userRespository.existsByEmail(user.getEmail());
        if (doesUserExist) {
            throw new CustomErrorResponse(ErrorMessages.EMAIL_ALREADY_USED.getMessage(), HttpStatus.CONFLICT);
        }

    }

}
