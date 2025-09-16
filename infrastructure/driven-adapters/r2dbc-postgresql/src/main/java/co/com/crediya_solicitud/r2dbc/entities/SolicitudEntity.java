package co.com.crediya_solicitud.r2dbc.entities;


import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("solicitud")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SolicitudEntity {
    @Id
    @Column("solicitud_id")
    private String solicitudId;
    private BigDecimal amount;
    private int term;
    private String email;
    @Column("state_id")
    private String stateId;
    @Column("loan_type_id")
    private String loanTypeId;
}
