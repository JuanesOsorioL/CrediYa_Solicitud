package co.com.crediya_solicitud.r2dbc.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("estados")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StateEntity {
    @Column("state_id")
    private String stateId;
    private String name;
    private String description;
}
