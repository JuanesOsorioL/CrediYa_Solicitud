package co.com.crediya_solicitud.r2dbc.entities;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("tipo_prestamo")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LoanTypeEntity {
    @Id
    @Column("loan_type_id")
    private String loanTypeId;
    private String name;
    @Column("minimum_amount")
    private BigDecimal minimumAmount;
    @Column("maximum_amount")
    private BigDecimal maximumAmount;
    @Column("interest_rate")
    private Integer interestRate;
    @Column("automatic_validation")
    private Boolean automaticValidation;
}
