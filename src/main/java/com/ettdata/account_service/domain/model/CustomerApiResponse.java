package com.ettdata.account_service.domain.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class CustomerApiResponse {
    @JsonProperty("data")
    private List<CustomerResponse> data;

    @JsonProperty("error")
    private String error;

    /**
     * Obtiene el primer cliente de la lista de datos.
     * @return CustomerResponse o null si la lista está vacía
     */
    public CustomerResponse getFirstCustomer() {
        return (data != null && !data.isEmpty()) ? data.get(0) : null;
    }

    /**
     * Verifica si la respuesta contiene datos.
     * @return true si hay al menos un cliente en la respuesta
     */
    public boolean hasData() {
        return data != null && !data.isEmpty();
    }

    /**
     * Verifica si la respuesta contiene un error.
     * @return true si hay un mensaje de error
     */
    public boolean hasError() {
        return error != null && !error.trim().isEmpty();
    }
}
