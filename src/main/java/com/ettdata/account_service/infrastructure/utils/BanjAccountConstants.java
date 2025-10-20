package com.ettdata.account_service.infrastructure.utils;

public class BanjAccountConstants {
    public static final int HTTP_OK = 200;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_INTERNAL_ERROR = 500;

    public static final String ACCOUNT_CREATED = "Bank account created successfully";
    public static final String ACCOUNT_ALREADY_EXISTS = "Customer already has this account type";
    public static final String ACCOUNT_TYPE_NOT_ALLOWED = "Account type not allowed for this customer type";
    public static final String ACCOUNT_MIN_BALANCE_ERROR = "Initial balance below minimum opening amount";
    public static final String CUSTOMER_NOT_FOUND = "Customer not found";
}
