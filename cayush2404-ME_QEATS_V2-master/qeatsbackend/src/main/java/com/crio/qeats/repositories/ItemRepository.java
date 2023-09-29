
package com.crio.qeats.repositories;

import com.crio.qeats.models.ItemEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ItemRepository extends MongoRepository<ItemEntity, String> {

    List<ItemEntity> findAll();

    @Query("{'name':{$regex: '^?0$', $options: 'i'}}")
    Optional<List<ItemEntity>> findItemByNameExact(String name);

    @Query("{'name':{$regex: '.*?0.*', $options: 'i'}}")
    Optional<List<ItemEntity>> findItemByNameInExact(String name_string);
}

