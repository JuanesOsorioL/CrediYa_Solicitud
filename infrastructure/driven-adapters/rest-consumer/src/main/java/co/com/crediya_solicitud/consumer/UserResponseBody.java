package co.com.crediya_solicitud.consumer;


import lombok.*;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserResponseBody {

    private String userId;
    private String firstName;
    private String lastName;
    private String birthDate;
    private String phone;
    private String email;
    private String documentId;
    private BigDecimal baseSalary;

}