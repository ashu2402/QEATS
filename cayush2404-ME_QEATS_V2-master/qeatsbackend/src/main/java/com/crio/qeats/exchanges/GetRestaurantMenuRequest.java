package com.crio.qeats.exchanges;

import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class GetRestaurantMenuRequest {

    @Min(value=1)
    @NotNull
    private String restaurantId;
  
}
