/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositories;

import com.crio.qeats.models.RestaurantEntity;
import java.util.Optional;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface RestaurantRepository extends MongoRepository<RestaurantEntity, String> {
    
  // List<RestaurantEntity> findAll();
  
  //You have to write queries in a such way
  @Query("{'name':{$regex: '^?0$', $options: 'i'}}")
  Optional<List<RestaurantEntity>> findRestaurantsByNameExact(String name);
  
  @Query("{'name': {$regex: '.*?0.*', $options: 'i'}}")
  Optional<List<RestaurantEntity>> findRestaurantsByName(String searchString);


  Optional<List<RestaurantEntity>> findByAttributesInIgnoreCase(List<String> attributes);

  Optional<List<RestaurantEntity>> findRestaurantsByRestaurantIdIn(List<String> restaurantIds);

}

