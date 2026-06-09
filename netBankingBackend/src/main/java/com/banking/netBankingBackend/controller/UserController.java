package com.banking.netBankingBackend.controller;


import com.banking.netBankingBackend.dto.ResponseDto;
import com.banking.netBankingBackend.dto.requestDtos.UserRegistrationDto;
import com.banking.netBankingBackend.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/auth", produces = {MediaType.APPLICATION_JSON_VALUE})
@RequiredArgsConstructor
public class UserController {


    private final UserServiceImpl userService;


    @PostMapping("/register")
    public ResponseEntity<ResponseDto> registration(@RequestBody UserRegistrationDto userDto) {

        userService.register(userDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ResponseDto("201", "User created successfully"));


    }


}
