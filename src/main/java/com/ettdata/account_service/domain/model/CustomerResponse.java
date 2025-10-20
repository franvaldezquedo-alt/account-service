package com.ettdata.account_service.domain.model;

import lombok.Data;

@Data
public class CustomerResponse {
    private String id;
    private String documentType;
    private String documentNumber;
    private String fullName;
    private String businessName;
    private String customerType;
}
