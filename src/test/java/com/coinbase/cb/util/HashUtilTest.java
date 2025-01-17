package com.coinbase.cb.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashUtilTest {


    @Test
    void checkSha265HashWithStringInput() {
        assertEquals("5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8", HashUtil.getSHA256("password"));
    }

    @Test
    void checkMd5HashWithStringInput() {
        assertEquals("5f4dcc3b5aa765d61d8327deb882cf99", HashUtil.getMD5("password"));
    }
}