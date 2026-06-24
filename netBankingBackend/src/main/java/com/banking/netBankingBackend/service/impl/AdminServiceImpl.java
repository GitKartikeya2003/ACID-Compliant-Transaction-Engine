package com.banking.netBankingBackend.service.impl;


import com.banking.netBankingBackend.dto.requestDtos.GetBalanceDto;
import com.banking.netBankingBackend.dto.responseDtos.FraudAlertDto;
import com.banking.netBankingBackend.dto.responseDtos.FraudStatsDto;
import com.banking.netBankingBackend.entity.AccountEntity;
import com.banking.netBankingBackend.entity.FraudDetectionEntities.FraudAlert;
import com.banking.netBankingBackend.enums.AccountStatus;
import com.banking.netBankingBackend.enums.AlertStatus;
import com.banking.netBankingBackend.enums.RuleType;
import com.banking.netBankingBackend.exception.ResourceNotFoundException;
import com.banking.netBankingBackend.mapper.AccountsMapper;
import com.banking.netBankingBackend.mapper.FraudMapper;
import com.banking.netBankingBackend.repository.AccountsRepository;
import com.banking.netBankingBackend.repository.FraudAlertRepository;
import com.banking.netBankingBackend.service.IAdminService;
import com.banking.netBankingBackend.util.AESUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements IAdminService {


    private final AccountsRepository accountsRepository;

    private final FraudAlertRepository fraudAlertRepository;

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

    @Override
    public List<FraudAlertDto> getAllAlerts() {

        List<FraudAlert> fraudAlerts = fraudAlertRepository.findAll();

        List<FraudAlertDto> response = new ArrayList<>();
        for (FraudAlert fraudAlert : fraudAlerts) {

            FraudAlertDto dto = new FraudAlertDto();
            FraudMapper.FraudAlert_To_Dto(fraudAlert, dto);
            response.add(dto);
        }


        return response;
    }

    @Override
    public List<FraudAlertDto> alertHistory_perAccount(String accountNumber) {

        String accountHash = AESUtil.hash(accountNumber);
        AccountEntity account = accountsRepository.findByAccountHash(accountHash).orElseThrow(
                () -> new ResourceNotFoundException("Account not found")
        );


        List<FraudAlert> fraudAlerts = fraudAlertRepository
                .findByAccount_AccountHash(AESUtil.hash(account.getAccountNumber()));

        if (fraudAlerts.isEmpty()) {
            throw new ResourceNotFoundException("No alerts found for this account");
        }

        List<FraudAlertDto> response = new ArrayList<>();


        for (FraudAlert fraudAlert : fraudAlerts) {

            FraudAlertDto dto = new FraudAlertDto();
            FraudMapper.FraudAlert_To_Dto(fraudAlert, dto);
            response.add(dto);


        }

        return response;

    }

    @Override
    public FraudStatsDto getStats() {
        FraudStatsDto stats = new FraudStatsDto();

        // Total alerts
        stats.setTotalAlerts(fraudAlertRepository.count());

        // Counts by status
        stats.setOpenAlerts(fraudAlertRepository.findByStatus(AlertStatus.OPEN).size());
        stats.setClearedAlerts(fraudAlertRepository.findByStatus(AlertStatus.CLEARED).size());
        stats.setConfirmedAlerts(fraudAlertRepository.findByStatus(AlertStatus.CONFIRMED).size());

        // Count by rule type (all statuses)
        List<Object[]> ruleTypeCounts = fraudAlertRepository.countByRuleType();
        Map<RuleType, Long> countByRuleType = new HashMap<>();
        for (Object[] row : ruleTypeCounts) {
            RuleType ruleType = (RuleType) row[0];
            Long count = (Long) row[1];
            countByRuleType.put(ruleType, count);
        }
        stats.setCountByRuleType(countByRuleType);

        // Count confirmed alerts by rule type
        List<Object[]> confirmedCounts = fraudAlertRepository.countByRuleTypeAndStatus(AlertStatus.CONFIRMED);
        Map<RuleType, Long> confirmedByRuleType = new HashMap<>();
        for (Object[] row : confirmedCounts) {
            RuleType ruleType = (RuleType) row[0];
            Long count = (Long) row[1];
            confirmedByRuleType.put(ruleType, count);
        }
        stats.setConfirmedByRuleType(confirmedByRuleType);

        return stats;
    }

    @Override
    @Transactional
    public void freezeAccount(GetBalanceDto getBalanceDto) {


        String accountHash = AESUtil.hash(getBalanceDto.getAccountNo());
        AccountEntity account = accountsRepository.findByAccountHash(accountHash).orElseThrow(
                () -> new ResourceNotFoundException("No  Such Account exists")
        );


        account.setStatus(AccountStatus.FROZEN);

    }

    @Override
    @Transactional
    public void ClearUser(FraudAlertDto fraudAlertDto) {


//        String accountHash = AESUtil.hash(fraudAlertDto.getAccountNumber());
//        AccountEntity account = accountsRepository.findByAccountHash(accountHash).orElseThrow(
//                () -> new ResourceNotFoundException("No  Such Account exists")
//        );
//
//        account.setStatus(AccountStatus.ACTIVE);


        // Find the alert by ID, not the account
        FraudAlert alert = fraudAlertRepository.findById(fraudAlertDto.getAlertId())
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));

        alert.setStatus(AlertStatus.CLEARED);


    }


}
