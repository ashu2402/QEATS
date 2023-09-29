package com.crio.qeats.exchanges;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.crio.qeats.dto.Menu;

@Data 
@AllArgsConstructor
@NoArgsConstructor
public class GetRestaurantMenuResponse {
    
    Menu menu;
    //List<Items> items;
}
