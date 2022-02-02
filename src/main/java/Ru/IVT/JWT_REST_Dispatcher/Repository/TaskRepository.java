package Ru.IVT.JWT_REST_Dispatcher.Repository;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;

@Transactional
public interface TaskRepository extends JpaRepository<Task, Long>{

//    Integer countTasksBetween2Dates();

    @Query(value = "SELECT count(t) FROM Task t WHERE t.user_id = :user_id AND t.created BETWEEN :date_1 AND :date_2")
    Integer countUserTasksBetween2Dates(@Param("user_id") Long user_id,
                                        @Param("date_1") Date date_1,
                                        @Param("date_2") Date date_2);

    @Query(value = "SELECT t FROM Task t WHERE t.user_id = :id")
    ArrayList<Task> getUserTasks(@Param("id")Long id);

    @Query(value = "SELECT t FROM Task t WHERE t.id = :id")
    Task getTaskById(@Param("id")Long id);

    @Query(value = "UPDATE Task set data_file_name = :data_file, source_file_name=:source_file WHERE id = :id")
    void updateTaskById(
                        @Param("id") Long id,
                        @Param("data_file") String data_file,
                        @Param("source_file") String source_file);

    @Modifying
    @Query(value = "UPDATE Task t SET t.data_file_name = :data_file, t.source_file_name=:source_file WHERE t.name = :name")
    void updateTaskByName(
            @Param("name") String name,
            @Param("data_file") String data_file,
            @Param("source_file") String source_file);

}
