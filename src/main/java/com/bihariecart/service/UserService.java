package com.bihariecart.service;

import com.bihariecart.dto.RegisterRequest;
import com.bihariecart.dto.LoginRequest;

public interface UserService {

    String register(RegisterRequest request);
    String login(LoginRequest request);

}