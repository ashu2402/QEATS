package com.crio.qeats.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
@Data
@NoArgsConstructor
public class Items {

    private String itemId;

    private String name;
    
    private String imageUrl;

    private Double price;

    private ArrayList<String> attributes;
    
}
