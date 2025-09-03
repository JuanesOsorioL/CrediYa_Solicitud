package co.com.crediya_solicitud.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRespons<T> {
    private Integer status;
    private String message;
    private T body;
}
