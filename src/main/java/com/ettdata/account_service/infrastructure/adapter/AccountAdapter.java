package com.ettdata.account_service.infrastructure.adapter;

import com.ettdata.account_service.application.port.out.AccountRepositoryOutputPort;
import com.ettdata.account_service.domain.model.Account;
import com.ettdata.account_service.infrastructure.entity.AccountEntity;
import com.ettdata.account_service.infrastructure.repository.AccountRepository;
import com.ettdata.account_service.infrastructure.utils.AccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AccountAdapter implements AccountRepositoryOutputPort {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountAdapter(AccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
      this.accountMapper = accountMapper;
    }

    @Override
    public Flux<Account> findAllBankAccount() {
        return accountRepository.findAll()
              .map(accountMapper::toDomain);
    }



    @Override
    public Mono<Account> saveAccount(Account account) {
      AccountEntity entity = accountMapper.toEntity(account);
      return accountRepository.save(entity)
            .map(accountMapper::toDomain);
    }

    @Override
    public Mono<Account> findByIdBankAccount(String id) {
      return accountRepository.findByAccountNumber(id)
            .map(accountMapper::toDomain);
    }

    @Override
    public Flux<Account> findByCustomerId(String customerId) {
        log.info("Buscando cuentas bancarias del cliente: {}", customerId);
        return accountRepository.findByCustomerId(customerId)
              .map(accountMapper::toDomain);

    }

    @Override
    public Mono<Void> deleteByIdBankAccount(String id) {
      return accountRepository.deleteById(id);
    }
}
