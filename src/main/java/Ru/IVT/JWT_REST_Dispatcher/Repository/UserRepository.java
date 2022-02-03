package Ru.IVT.JWT_REST_Dispatcher.Repository;

import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;

/**
 * Repository interface that extends {@link JpaRepository} for class {@link User}.
 *
 * @author Eugene Suleimanov
 * @version 1.0
 */

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String name);

    @Query(value = "SELECT U FROM User U WHERE U.id = :id")
    User getUserById(@Param("id")Long id);

}
