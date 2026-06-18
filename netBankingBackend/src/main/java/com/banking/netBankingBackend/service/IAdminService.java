package com.banking.netBankingBackend.service;

import com.banking.netBankingBackend.dto.requestDtos.GetBalanceDto;

import java.util.List;

public interface IAdminService {


    List<GetBalanceDto> getAllUsers();
}
