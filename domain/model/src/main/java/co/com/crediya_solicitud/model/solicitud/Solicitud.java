package co.com.crediya_solicitud.model.solicitud;

import lombok.*;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Solicitud {
    private String solicitudId;
    private BigDecimal amount;
    private int term;
    private String email;
    private String stateId;
    private String loanTypeId;
    private String documentId;
}
