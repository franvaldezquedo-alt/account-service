package com.ettdata.account_service.application.port.in;

import com.ettdata.account_service.domain.model.CustomerResponse;
import reactor.core.publisher.Mono;

public interface CustomerInputService {
    Mono<CustomerResponse> getCustomerByDocument(String customerDocument);
}
