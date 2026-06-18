package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.requestDtos.GetBalanceDto;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.exception.ResourceNotFoundException;
import com.banking.netBankingBackend.mapper.AccountsMapper;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.service.IAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements IAdminService {


    private final AccountsRepository accountsRepository;

    @Override
    public List<GetBalanceDto> getAllUsers() {

        List<AccountEntity> users = accountsRepository.findAll();


        if (users.isEmpty()) {

            throw new ResourceNotFoundException("No Users found");
        }
        List<GetBalanceDto> response = new ArrayList<>();


        for (AccountEntity user : users) {

            GetBalanceDto res = new GetBalanceDto();
            AccountsMapper.AccountsEntitytoSetBalanceDto(user, res);
            response.add(res);

        }


        return response;
    }
}
