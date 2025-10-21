package com.ettdata.account_service.infrastructure.adapter;

import com.ettdata.account_service.application.port.out.AccountRepositoryOutputPort;
import com.ettdata.account_service.domain.model.Account;
import com.ettdata.account_service.infrastructure.entity.AccountEntity;
import com.ettdata.account_service.infrastructure.repository.AccountRepository;
import com.ettdata.account_service.infrastructure.utils.AccountUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AccountAdapter implements AccountRepositoryOutputPort {

    private final AccountRepository accountRepository;

    public AccountAdapter(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Flux<AccountEntity> findAllBankAccount() {
        return accountRepository.findAll();
    }

    @Override
    public Mono<Account> saveBankAccount(Account account) {
        log.debug("Persistiendo cuenta bancaria: {}", account);

        AccountEntity entity = AccountUtils.convertDomainToEntity(account);

        return accountRepository.save(entity)
                .map(AccountUtils::convertEntityToDomain)
                .doOnSuccess(saved ->
                        log.info("Cuenta bancaria guardada exitosamente con ID: {}", saved.getId()));
    }

    @Override
    public Mono<AccountEntity> findByIdBankAccount(String id) {
        return accountRepository.findById(id);
    }

    @Override
    public Flux<AccountEntity> findByCustomerId(String customerId) {
        log.info("Buscando cuentas bancarias del cliente: {}", customerId);
        return accountRepository.findByCustomerId(customerId)
                .doOnComplete(() -> log.info("Búsqueda de cuentas del cliente {} completada", customerId))
                .doOnError(error -> log.error("Error al buscar cuentas del cliente {}: {}",
                        customerId, error.getMessage()));
    }

    @Override
    public Mono<Void> deleteByIdBankAccount(String id) {
        return accountRepository.deleteById(id);
    }
}
