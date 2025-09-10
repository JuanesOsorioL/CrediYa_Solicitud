package co.com.crediya_solicitud.model.user;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;


@Builder(toBuilder = true)
public record User(String userId,
                   String firstName,
                   String lastName,
                   String documentId,
                   LocalDate birthDate,
                   String phone,
                   String email,
                   String password,
                   String rolId,
                   BigDecimal baseSalary) {

}
