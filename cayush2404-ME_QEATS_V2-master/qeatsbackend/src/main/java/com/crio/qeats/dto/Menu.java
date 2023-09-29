package com.crio.qeats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import com.crio.qeats.models.ItemEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Menu {

//{
  //  "menu": {
  //    "items": [
  //      {
  //        "attributes": [
  //          "South Indian"
  //        ],
  //        "id": "1",
  //        "imageUrl": "www.google.com",
  //        "itemId": "10",
  //        "name": "Idly",
  //        "price": 45
  //      }
  //    ],
  //    "restaurantId": "11"
  //  }


  private String restaurantId;
  
  private ArrayList<ItemEntity> items;


}
