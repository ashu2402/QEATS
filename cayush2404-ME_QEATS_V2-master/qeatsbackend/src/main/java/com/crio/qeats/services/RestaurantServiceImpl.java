
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Menu;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantMenuRequest;
import com.crio.qeats.exchanges.GetRestaurantMenuResponse;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;


  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

      List<Restaurant> restaurants_list = new ArrayList<>();

      if((currentTime.getHour()>=8 && (currentTime.getHour()<=10 && currentTime.getMinute()<=0 && currentTime.getSecond()<=0) )
      || (currentTime.getHour()>=13 && (currentTime.getHour()<=14 && currentTime.getMinute()<=0 && currentTime.getSecond()<=0) )
      || (currentTime.getHour()>=19 && (currentTime.getHour()<=21  && currentTime.getMinute()<=0 && currentTime.getSecond()<=0))){
        //peak hours time:
        restaurants_list = restaurantRepositoryService.findAllRestaurantsCloseBy(getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), currentTime, peakHoursServingRadiusInKms);
      }
      else{
        //non-peak hours timing:
        restaurants_list = restaurantRepositoryService.findAllRestaurantsCloseBy(getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), currentTime, normalHoursServingRadiusInKms);
      }

      GetRestaurantsResponse response = new GetRestaurantsResponse(restaurants_list);
      log.info(response);
      return response;

  }




  @Override
  public GetRestaurantMenuResponse findMenuByRestaurantId(
      GetRestaurantMenuRequest getRestaurantMenuRequest) {

    Menu menu = restaurantRepositoryService.findRestaurantMenuByRestaurantId(getRestaurantMenuRequest.getRestaurantId());
    GetRestaurantMenuResponse response = new GetRestaurantMenuResponse(menu); 

    return response;
  }



  


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

      Set<String> distinct_restaurants = new HashSet<>();
      List<List<Restaurant>> restaurantAnsList=new ArrayList<>(); 
      List<Restaurant> restaurantList = new ArrayList<>();
      Double servingHoursInRadiusInKms = 0.0;

      if(!getRestaurantsRequest.getSearchFor().isEmpty()){
        if((currentTime.getHour()>=8 && (currentTime.getHour()<=10 && currentTime.getMinute()<=0 && currentTime.getSecond()<=0) )
          || (currentTime.getHour()>=13 && (currentTime.getHour()<=14 && currentTime.getMinute()<=0 && currentTime.getSecond()<=0) )
          || (currentTime.getHour()>=19 && (currentTime.getHour()<=21  && currentTime.getMinute()<=0 && currentTime.getSecond()<=0))){
            //peak hours time:
            servingHoursInRadiusInKms = peakHoursServingRadiusInKms;
          }
        else{
            //non-peak hours timing:
            servingHoursInRadiusInKms = normalHoursServingRadiusInKms;
          }

          //By restaurant_name
          restaurantAnsList.add(restaurantRepositoryService.findRestaurantsByName(getRestaurantsRequest.getLatitude(), 
                getRestaurantsRequest.getLongitude(), getRestaurantsRequest.getSearchFor(), currentTime, servingHoursInRadiusInKms));
  
          //By restaurant_attributes
          restaurantAnsList.add(restaurantRepositoryService.findRestaurantsByAttributes(getRestaurantsRequest.getLatitude(),
                getRestaurantsRequest.getLongitude(), getRestaurantsRequest.getSearchFor(), currentTime, servingHoursInRadiusInKms));
  
          //By ItemName
          restaurantAnsList.add(restaurantRepositoryService.findRestaurantsByItemName(getRestaurantsRequest.getLatitude(),
              getRestaurantsRequest.getLongitude(), getRestaurantsRequest.getSearchFor(), currentTime, servingHoursInRadiusInKms));
  
          //By ItemAttributes
          restaurantAnsList.add(restaurantRepositoryService.findRestaurantsByItemAttributes(getRestaurantsRequest.getLatitude(),
              getRestaurantsRequest.getLongitude(), getRestaurantsRequest.getSearchFor(), currentTime, servingHoursInRadiusInKms));

      }
      else{
        return new GetRestaurantsResponse(new ArrayList<>());
      }

      for(List<Restaurant> ListRestIter: restaurantAnsList){
        for(Restaurant restListRestIterIter: ListRestIter){
              distinct_restaurants.add(restListRestIterIter.getName());
              restaurantList.add(restListRestIterIter);
        }
      }

      GetRestaurantsResponse response = new GetRestaurantsResponse(restaurantList);
     return response;
  }

  

  // TODO: CRIO_TASK_MODULE_MULTITHREADING
  // Implement multi-threaded version of RestaurantSearch.
  // Implement variant of findRestaurantsBySearchQuery which is at least 1.5x time faster than
  // findRestaurantsBySearchQuery.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQueryMt(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

     return null;
  }
}

