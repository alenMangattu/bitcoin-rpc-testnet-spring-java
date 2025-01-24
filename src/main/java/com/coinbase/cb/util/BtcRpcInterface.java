package com.coinbase.cb.util;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class BtcRpcInterface {

    // HTTP client for making RPC calls
    private static final HttpClient client = HttpClient.newHttpClient();
    // Bitcoin Core RPC endpoint (localhost)
    private static final String RPC_URL = "http://127.0.0.1:18332";

    // Helper method to create JSON-RPC request objects
    // Takes method name, ID and optional parameters
    public static<T> JSONObject generateReqJson(String id, String method, @SuppressWarnings("unchecked") T... args) {
        JSONObject reqJson = new JSONObject();

        reqJson.put("id", id);
        reqJson.put("method", method);

        if (args.length > 0)
            reqJson.put("params", args);
        return reqJson;
    }

    // Main method to send RPC requests to Bitcoin Core
    // Takes a JSON request body and optional wallet address, returns JSON response
    public static JSONObject callRpcMethod(JSONObject requestBody, String walletAddress) {
        try {
            // Validate method exists
            if (requestBody.get("method") == null)
                throw new IllegalArgumentException("method cannot be null");

            // Add JSON-RPC version
            requestBody.put("jsonrpc", "1.0");

            // Build URL based on whether wallet address is provided
            String url = walletAddress != null ? RPC_URL + "/wallet/" + walletAddress : RPC_URL;

            // Build HTTP request with headers
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    // Basic auth header (base64 encoded credentials)
                    .header("Authorization", "Basic Yml0Y29pbnVzZXI6YnRjcGFzc3dvcmQ=")
                    .header("Accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                    .build();

            // Send request and parse response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // System.out.println(response.body().toString());
            return new JSONObject(response.body());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Overloaded method for backward compatibility
    public static JSONObject callRpcMethod(JSONObject requestBody) {
        return callRpcMethod(requestBody, null);
    }
}

