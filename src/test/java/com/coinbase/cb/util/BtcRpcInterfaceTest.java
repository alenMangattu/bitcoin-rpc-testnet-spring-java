package com.coinbase.cb.util;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

class BtcRpcInterfaceTest {

    @Test
    void generateReqJson() throws JSONException {
        JSONObject reqJson = BtcRpcInterface.generateReqJson("2", "getbestblockhash");
        JSONObject expectedJson = new JSONObject();
        expectedJson.put("id", "2");
        expectedJson.put("method", "getbestblockhash");

        JSONAssert.assertEquals(expectedJson.toString(), reqJson.toString(), false);
    }

    @Test
    void callRpcMethod() {
        JSONObject paramsJson = BtcRpcInterface.generateReqJson("2", "getbestblockhash");
        JSONObject reqJson = BtcRpcInterface.callRpcMethod(paramsJson);

        JSONObject expectedJson = new JSONObject();
        expectedJson.put("result", "00000000000000000000b5d8903325a0f9fd20f05975182c46b4a9956304d43f");
        expectedJson.put("error", JSONObject.NULL);
        expectedJson.put("id", "2");

        JSONAssert.assertEquals(expectedJson.toString(), reqJson.toString(), false);
    }
}
