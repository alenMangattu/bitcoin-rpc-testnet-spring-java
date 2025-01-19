package com.coinbase.cb.error;

public enum ErrorMessages {
    PASSWORD_NOT_VALID("Password must be between 12 and 240 characters, containing at least one lowercase letter, one uppercase letter, one number, and one special character (e.g., @$!%*?&-_,;)."),
    EMAIL_ALREADY_USED("The provided email address is already associated with an account."),
    FORM_NOT_COMPLETE("Please ensure all required fields are completed."),
    ONLY_CHARACTERS_ALLOWED("First name and last name should only contain alphabetic characters."),
    EMAIL_NOT_VALID("The provided email address is not valid."),
    ERROR_CREATING_WALLET("An error occurred while attempting to create the wallet. Please try again."),
    INVALID_CREDENTIALS("Invalid email or password."),
    USER_NOT_FOUND("User not found with the provided email."),
    ERROR_GETTING_ADDRESS("An error occurred while attempting to get the address. Please try again."),


    ;


    private final String message;

    ErrorMessages(String s) {
        this.message = s;
    }

    public String getMessage() {
        return message;
    }
}
