package com.coinbase.cb.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegexUtilsTest {

    @Test
    void isValidPassword() {
        // Valid passwords
        assertTrue(RegexUtils.isValidPassword("hElewfonwef123@@"));
        assertTrue(RegexUtils.isValidPassword("SecurePass123!@"));
        
        // Invalid passwords
        assertFalse(RegexUtils.isValidPassword("short1!A")); // too short
        assertFalse(RegexUtils.isValidPassword("nouppercase123!")); // no uppercase
        assertFalse(RegexUtils.isValidPassword("NOLOWERCASE123!")); // no lowercase
        assertFalse(RegexUtils.isValidPassword("NoSpecialChar123")); // no special char
        assertFalse(RegexUtils.isValidPassword("NoNumber!@#")); // no number
        assertTrue(RegexUtils.isValidPassword("Password@@@123-"));
        assertTrue(RegexUtils.isValidPassword("Password@@@123"));
        // Test with null and empty
        assertFalse(RegexUtils.isValidPassword(null));
        assertFalse(RegexUtils.isValidPassword(""));
    }

}