package co.com.crediya_solicitud.consumer.dto;


import lombok.Builder;

import java.math.BigDecimal;

@Builder(toBuilder = true)
public record ExternalUserDto(String userId,
                              String firstName,
                              String lastName,
                              String birthDate,
                              String phone,
                              String email,
                              String documentId,
                              BigDecimal baseSalary) {


}