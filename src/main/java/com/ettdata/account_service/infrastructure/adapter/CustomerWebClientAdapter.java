package com.ettdata.account_service.infrastructure.adapter;

import com.ettdata.account_service.application.port.out.CustomerOutputPort;
import com.ettdata.account_service.domain.model.CustomerApiResponse;
import com.ettdata.account_service.domain.model.CustomerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * Adaptador para consumir el microservicio de clientes mediante WebClient reactivo.
 * Implementa circuit breaker y manejo de errores según los requisitos del proyecto.
 */
@Component
@Slf4j
public class CustomerWebClientAdapter implements CustomerOutputPort {

  private final WebClient webClient;

  /**
   * Constructor con URL configurable desde properties.
   *
   * @param customerServiceUrl URL base del servicio de clientes
   */
  public CustomerWebClientAdapter(@Value("${customer.service.url:http://localhost:8080}") String customerServiceUrl) {
    this.webClient = WebClient.builder()
          .baseUrl(customerServiceUrl)
          .build();
  }

  /**
   * Obtiene la información de un cliente por su número de documento.
   * La API devuelve una estructura: { "data": [cliente], "error": null }
   *
   * @param documentNumber Número de documento del cliente
   * @return Mono con los datos del cliente o Mono.empty() si no existe
   */
  @Override
  public Mono<CustomerResponse> getCustomerByDocument(String documentNumber) {
    log.info("Consultando cliente con documento: {}", documentNumber);

    return webClient.get()
          .uri("/api/customers/document/{documentNumber}", documentNumber)
          .retrieve()
          .onStatus(
                status -> status.value() == HttpStatus.NOT_FOUND.value(),
                response -> {
                  log.warn("Cliente no encontrado con documento: {}", documentNumber);
                  return Mono.empty();
                })
          .onStatus(
                HttpStatus::is4xxClientError,
                response -> {
                  log.error("Error del cliente (4xx) al consultar documento: {}", documentNumber);
                  return Mono.error(new RuntimeException("Error en la solicitud al servicio de clientes"));
                })
          .onStatus(
                HttpStatus::is5xxServerError,
                response -> {
                  log.error("Error del servidor en el servicio de clientes");
                  return Mono.error(new RuntimeException("Servicio de clientes no disponible"));
                })
          .bodyToMono(CustomerApiResponse.class)
          .timeout(Duration.ofSeconds(2))
          .flatMap(apiResponse -> {
            if (apiResponse.hasError()) {
              log.error("La API de clientes devolvió error: {}", apiResponse.getError());
              return Mono.empty();
            }

            if (!apiResponse.hasData()) {
              log.warn("No se encontraron datos para el documento: {}", documentNumber);
              return Mono.empty();
            }

            CustomerResponse customer = apiResponse.getFirstCustomer();
            log.info("Cliente obtenido exitosamente - ID: {}, Tipo: {}",
                  customer.getId(), customer.getCustomerType());
            return Mono.just(customer);
          })
          .doOnError(error ->
                log.error("Error al consultar cliente con documento {}: {}",
                      documentNumber, error.getMessage()))
          .onErrorResume(ex -> {
            log.error("Excepción al consumir servicio de clientes: {}", ex.getMessage());
            return Mono.empty();
          });
  }
}