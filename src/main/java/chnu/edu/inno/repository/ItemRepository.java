package chnu.edu.inno.repository;


/*
  @author   george
  @project   inno
  @class  ItemRepository
  @version  1.0.0 
  @since 27/09/2026 - 19.04
*/

import chnu.edu.inno.model.Item;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends MongoRepository<Item, String> {
}
