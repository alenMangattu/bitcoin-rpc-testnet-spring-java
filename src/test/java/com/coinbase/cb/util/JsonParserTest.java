package com.coinbase.cb.util;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JsonParserTest {

    @Test
    void whenParsingValidTransactionResponse_thenExtractCorrectTxid() {
        // Arrange
        String jsonResponse = "{\n" +
            "    \"result\": {\n" +
            "        \"txid\": \"4967c9d7848e088450d7b1e812f2ab9b86cec1e03f1b00f16229a2d66a61f415\",\n" +
            "        \"complete\": true\n" +
            "    },\n" +
            "    \"error\": null,\n" +
            "    \"id\": \"curltest\"\n" +
            "}";
        
        // Act
        JSONObject jsonObject = new JSONObject(jsonResponse);
        String txid = jsonObject.getJSONObject("result").getString("txid");
        boolean isComplete = jsonObject.getJSONObject("result").getBoolean("complete");
        Object error = jsonObject.get("error");
        
        assertEquals("4967c9d7848e088450d7b1e812f2ab9b86cec1e03f1b00f16229a2d66a61f415", txid);
        assertTrue(isComplete);
        assertTrue(jsonObject.isNull("error"));
    }

    @Test
    void whenParsingInvalidJson_thenThrowException() {
        // Arrange
        String invalidJson = "{ invalid json }";
        
        // Act & Assert
        assertThrows(org.json.JSONException.class, () -> {
            new JSONObject(invalidJson);
        });
    }

    @Test
    void whenMissingTxid_thenThrowException() {
        // Arrange
        String jsonWithoutTxid = "{\n" +
            "    \"result\": {\n" +
            "        \"complete\": true\n" +
            "    },\n" +
            "    \"error\": null,\n" +
            "    \"id\": \"curltest\"\n" +
            "}";
        
        // Act & Assert
        JSONObject jsonObject = new JSONObject(jsonWithoutTxid);
        assertThrows(org.json.JSONException.class, () -> {
            jsonObject.getJSONObject("result").getString("txid");
        });
    }
} 
