package Ru.IVT.JWT_REST_Dispatcher.Repository;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long>{

}
