package co.com.crediya_solicitud.model.state;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class State {
    private String state_id;
    private String name;
    private String description;
}
