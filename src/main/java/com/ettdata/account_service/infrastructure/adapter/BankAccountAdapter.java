package com.ettdata.account_service.infrastructure.adapter;

import com.ettdata.account_service.application.port.out.BankAccountRepositoryOutputPort;
import com.ettdata.account_service.domain.model.BankAccount;
import com.ettdata.account_service.infrastructure.entity.BankAccountEntity;
import com.ettdata.account_service.infrastructure.repository.BankAccountRepository;
import com.ettdata.account_service.infrastructure.utils.BankAccountUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class BankAccountAdapter implements BankAccountRepositoryOutputPort {

    private final BankAccountRepository bankAccountRepository;

    public BankAccountAdapter(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    @Override
    public Flux<BankAccountEntity> findAllBankAccount() {
        return bankAccountRepository.findAll();
    }

    @Override
    public Mono<BankAccount> saveBankAccount(BankAccount bankAccount) {
        log.debug("Persistiendo cuenta bancaria: {}", bankAccount);

        BankAccountEntity entity = BankAccountUtils.convertDomainToEntity(bankAccount);

        return bankAccountRepository.save(entity)
                .map(savedEntity -> BankAccountUtils.convertEntityToDomain(savedEntity))
                .doOnSuccess(saved ->
                        log.info("Cuenta bancaria guardada exitosamente con ID: {}", saved.getId()));
    }

    @Override
    public Mono<BankAccountEntity> findByIdBankAccount(String id) {
        return bankAccountRepository.findById(id);
    }

    @Override
    public Flux<BankAccountEntity> findByCustomerId(String customerId) {
        log.info("Buscando cuentas bancarias del cliente: {}", customerId);
        return bankAccountRepository.findByCustomerId(customerId)
                .doOnComplete(() -> log.info("Búsqueda de cuentas del cliente {} completada", customerId))
                .doOnError(error -> log.error("Error al buscar cuentas del cliente {}: {}",
                        customerId, error.getMessage()));
    }
}
