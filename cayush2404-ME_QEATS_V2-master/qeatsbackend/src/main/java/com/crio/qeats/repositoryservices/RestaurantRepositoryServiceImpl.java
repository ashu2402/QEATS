/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;


import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Items;
import com.crio.qeats.dto.Menu;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ch.hsr.geohash.GeoHash;
import redis.clients.jedis.Jedis;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Primary
@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private RedisConfiguration redisConfiguration;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;
  
  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private MenuRepository menuRepository;

  @Autowired
  private ItemRepository itRepository;


  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
      LocalTime openingTime = LocalTime.parse(res.getOpensAt());
      LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }


  // * Utility method to check if a restaurant is within the serving radius at a given time.
  // * @return boolean True if restaurant falls within serving radius and is open, false otherwise
  // */
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude,
          restaurantEntity.getLatitude(), restaurantEntity.getLongitude())
          < servingRadiusInKms;
    }
    return false;
  }

  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objectives:
  // 1. Implement findAllRestaurantsCloseby.
  // 2. Remember to keep the precision of GeoHash in mind while using it as a key.
  // Check RestaurantRepositoryService.java file for the interface contract.
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude, LocalTime currentTime, Double servingRadiusInkms){
      List<Restaurant> restaurants = new ArrayList<>();

      if(redisConfiguration.isCacheAvailable()){
          restaurants = findAllRestaurantCloseByFromCache(latitude, longitude, currentTime, servingRadiusInkms) ;
      }
      else{
        restaurants = findAllRestaurantsCloseByFromDb(latitude, longitude, currentTime, servingRadiusInkms);
      }
    return restaurants;
  }



  public List<Restaurant> findAllRestaurantsCloseByFromDb(Double latitude, Double longitude, LocalTime currentTime, Double servingRadiusInKms) {
  
      List<RestaurantEntity> restaurantEntities = restaurantRepository.findAll();

    return restaurantEntityListfilterOpenRestaurantsReturnRestaurantList(restaurantEntities, latitude, longitude, currentTime, servingRadiusInKms);
  }




  private List<Restaurant> findAllRestaurantCloseByFromCache(Double latitude, Double longitude, LocalTime currentTime, Double servingRadiusInkms){
    List<Restaurant> restaurants_list = new ArrayList<>();

    GeoLocation geoLocation = new GeoLocation(latitude, longitude);
    GeoHash geoHash = GeoHash.withCharacterPrecision(geoLocation.getLatitude(), geoLocation.getLongitude(), 7);

    try (Jedis jedis = redisConfiguration.getJedisPool().getResource() ){
      String jsonStringFromCache = jedis.get(geoHash.toBase32());
      if(jsonStringFromCache == null){
        //it means cache needs to be updated
        String createdJsonString = "";
        try {
          restaurants_list = findAllRestaurantsCloseByFromDb(latitude, longitude, currentTime, servingRadiusInkms);
          createdJsonString = new ObjectMapper().writeValueAsString(restaurants_list);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
        //do operations with jedis resource
        jedis.setex(geoHash.toBase32(),GlobalConstants.REDIS_ENTRY_EXPIRY_IN_SECONDS, createdJsonString);
      }
        else{
          try {
            restaurants_list = new ObjectMapper().readValue(jsonStringFromCache, new TypeReference<List<Restaurant>>() {  
            });
    } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    return restaurants_list;
  }



  @Override
  public Menu findRestaurantMenuByRestaurantId(String restaurant_id) {
    
    ModelMapper modelMapper = modelMapperProvider.get();

    Optional<MenuEntity> restaurant_menu = menuRepository.findMenuByRestaurantId(restaurant_id);
    Menu restaurantMenu = new Menu();

    if(restaurant_menu.isPresent()){
      restaurantMenu = modelMapper.map(restaurant_menu, Menu.class);
    }
    System.out.println(restaurantMenu.getItems().size());
    return restaurantMenu;
  }



  @Override
  public List<Items> findAll() {
    
    ModelMapper modelMapper = modelMapperProvider.get();

    List<Items> item_list = new ArrayList<>();
    List<ItemEntity> itemsEntityList = itRepository.findAll();

    for(ItemEntity item : itemsEntityList){
      item_list.add(modelMapper.map(item, Items.class));
    }
    return item_list;
  }
  


  public List<Restaurant> restaurantEntityListfilterOpenRestaurantsReturnRestaurantList(List<RestaurantEntity> restaurantEntityList, 
      Double latitude, Double longitude, LocalTime currentTime, Double servingRadiusInKms){

    ModelMapper modelMapper = modelMapperProvider.get();
    List<Restaurant> restaurantList = new ArrayList<>();

    for (RestaurantEntity restaurantEntity : restaurantEntityList) { 
      if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude, servingRadiusInKms)) { 
        restaurantList.add(modelMapper.map(restaurantEntity, Restaurant.class)); 
      } 
    } 
    return restaurantList;
  }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search query.
  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

        Optional<List<RestaurantEntity>> optionalExactRestaurantEntityList = restaurantRepository.findRestaurantsByNameExact(searchString);
        Optional<List<RestaurantEntity>> optionalInexactRestaurantEntityLis = restaurantRepository.findRestaurantsByName(searchString);
        List<RestaurantEntity> restaurantEntityList = new ArrayList<>();

        if(optionalExactRestaurantEntityList.isPresent()){
          restaurantEntityList = optionalExactRestaurantEntityList.get().stream().collect(Collectors.toList());
        }
        if(optionalInexactRestaurantEntityLis.isPresent()){
          restaurantEntityList.addAll(optionalInexactRestaurantEntityLis.get().stream().collect(Collectors.toList()));
        }

    return restaurantEntityListfilterOpenRestaurantsReturnRestaurantList(restaurantEntityList, latitude, longitude, currentTime, servingRadiusInKms);

    //   List<Restaurant> all_available_restaurants = findAllRestaurantsCloseBy(latitude, longitude, currentTime, servingRadiusInKms) ; 
    //   Iterator<Restaurant> i = all_available_restaurants.iterator();
    //   while (i.hasNext()) {
    //     if(!(((Restaurant) i).getName().contains(searchString))){
    //       i.remove();
    //     }
    //   }
    //  return all_available_restaurants;
  }


  

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(
    Double latitude, Double longitude,
    String searchString, LocalTime currentTime, Double servingRadiusInKms) {

      List<Pattern> patterns = Arrays .stream(searchString.split(" ")) 
              .map(attr -> Pattern.compile(attr, Pattern.CASE_INSENSITIVE)) .collect(Collectors.toList()); 
      Query query = new Query(); 
      for (Pattern pattern : patterns) { 
         query.addCriteria(Criteria.where("attributes").regex(pattern)); 
      } 
      List<RestaurantEntity> restaurantEntityList = mongoTemplate.find(query, RestaurantEntity.class);
     
    return restaurantEntityListfilterOpenRestaurantsReturnRestaurantList(restaurantEntityList, latitude, longitude, currentTime, servingRadiusInKms);  
  }




 
  


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose names form a complete or partial match
  // with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemName(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

      Optional<List<ItemEntity>> optionalItemEntityList = itRepository.findItemByNameExact(searchString);
      Optional<List<ItemEntity>> optionalItemEntityList1 = itRepository.findItemByNameInExact(searchString);
      List<ItemEntity> tempItemEntityList = new ArrayList<>();

      if(optionalItemEntityList.isPresent()){
        tempItemEntityList = optionalItemEntityList.get().stream().collect(Collectors.toList());
      }
      if(optionalItemEntityList1.isPresent()){
        tempItemEntityList.addAll(optionalItemEntityList1.get().stream().collect(Collectors.toList()));
      }

    return getRestaurantServingItems(latitude,longitude,currentTime,servingRadiusInKms,tempItemEntityList);
  }



  public List<Restaurant> getRestaurantServingItems(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms, List<ItemEntity> entitylist){

        List<String> itemsIdList = entitylist.stream().map(i->i.getItemId()).collect(Collectors.toList());

        //get all the menuentities in which the item is present so as we can get the restaurant id's in the menuentity
        Optional<List<MenuEntity>> menuEntityList = menuRepository.findMenusByItemsItemIdIn(itemsIdList);

        List<String> restaurantIdsList = new ArrayList<>();
        //stream over to the menuentity list to collect all the restaurant id's
        if(menuEntityList.isPresent()){
          restaurantIdsList = menuEntityList.get().stream().map(e->e.getRestaurantId()).collect(Collectors.toList());
        }
        
        //now we have fetched all restaurantEntity based on restaurant id's
        Optional<List<RestaurantEntity>> restaurantEntityList = restaurantRepository.findRestaurantsByRestaurantIdIn(restaurantIdsList);

    return restaurantEntityListfilterOpenRestaurantsReturnRestaurantList(restaurantEntityList.get().stream().collect(Collectors.toList()), latitude, longitude, currentTime, servingRadiusInKms);
  }




  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

      List<Pattern> patterns = Arrays .stream(searchString.split(" ")) 
              .map(attr -> Pattern.compile(attr, Pattern.CASE_INSENSITIVE)).collect(Collectors.toList()); 
      Query query = new Query();
      for(Pattern pattern : patterns) {
          query.addCriteria(Criteria.where("attributes").regex(pattern));
      }
      List<ItemEntity> itemEntityList = mongoTemplate.find(query, ItemEntity.class);

    return getRestaurantServingItems(latitude, longitude, currentTime, servingRadiusInKms, itemEntityList);
  }
}
