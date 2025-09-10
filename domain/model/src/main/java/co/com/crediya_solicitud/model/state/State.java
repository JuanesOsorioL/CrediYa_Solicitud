package co.com.crediya_solicitud.model.state;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class State {
    private String stateId;
    private String name;
    private String description;
}
