package com.coinbase.cb.controllers.api.api_services;

import com.coinbase.cb.controllers.Query;
import com.coinbase.cb.error.CustomErrorResponse;
import com.coinbase.cb.error.ErrorMessages;
import com.coinbase.cb.models.DTOs.UserDTO;
import com.coinbase.cb.models.DbTables.User;
import com.coinbase.cb.models.DbTables.Wallet;
import com.coinbase.cb.repository.UserRespository;
import com.coinbase.cb.util.Base64Util;
import com.coinbase.cb.util.BtcRpcInterface;
import com.coinbase.cb.util.HashUtil;
import com.coinbase.cb.util.validators.RegisterInputValidator;
import com.coinbase.cb.configuration.security.JwtService;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RegisterController implements Query<User, UserDTO> {

    private final UserRespository userRespository;
    private final JwtService jwtService;

    public RegisterController(UserRespository userRespository, JwtService jwtService) {
        this.userRespository = userRespository;
        this.jwtService = jwtService;
    }

    @Override
    public ResponseEntity<UserDTO> execute(User user) {
        RegisterInputValidator.validate(user);

        String password = Base64Util.decode(user.getPassword());
        user.setPassword(HashUtil.getSHA256(password));

        Wallet wallet = RegisterController.createWallet(user.getEmail());
        wallet.setUser(user);
        user.setWallet(wallet);
        User savedUser = userRespository.save(user);

        System.out.println("creating token");
        // Generate JWT token
        String token = jwtService.generateToken(savedUser);
        
        // Create response headers with token
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(new UserDTO(user.getFirstName(), user.getLastName(), user.getEmail(), user.getWallet().getBalance()));
    }

    private static Wallet createWallet(String email) {
        String hashedEmail = HashUtil.getMD5(email + Math.random());

        //Create wallet on btc
        JSONObject reqJson = BtcRpcInterface.generateReqJson("id", "createwallet", hashedEmail);
        JSONObject execute = BtcRpcInterface.callRpcMethod(reqJson);

        if (execute == null || execute.get("result") == null) {
            throw new CustomErrorResponse(ErrorMessages.ERROR_CREATING_WALLET.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String walletName = (String) new JSONObject(execute.get("result").toString()).get("name");
        if (walletName == null || walletName.isEmpty()) {
            throw new CustomErrorResponse(ErrorMessages.ERROR_CREATING_WALLET.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Wallet wallet = new Wallet();
        wallet.setWalletName(walletName);
        wallet.setBalance(0.0d);

        //Get address from btc
        reqJson = BtcRpcInterface.generateReqJson("id", "getnewaddress");
        execute = BtcRpcInterface.callRpcMethod(reqJson, walletName);

        if (execute == null || execute.get("result") == null) {
            throw new CustomErrorResponse(ErrorMessages.ERROR_GETTING_ADDRESS.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String address = execute.get("result").toString();
        if (address == null || address.isEmpty()) {
            throw new CustomErrorResponse(ErrorMessages.ERROR_GETTING_ADDRESS.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        wallet.setAddress(address);

        return wallet;
    }
}

