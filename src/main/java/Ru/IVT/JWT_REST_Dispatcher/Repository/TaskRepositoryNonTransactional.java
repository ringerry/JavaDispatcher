package Ru.IVT.JWT_REST_Dispatcher.Repository;


import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;
import java.util.Date;

public interface TaskRepositoryNonTransactional extends JpaRepository<Task, Long> {

//    Integer countTasksBetween2Dates();

    @Query(value = "SELECT count(t) FROM Task t WHERE t.user_id = :user_id AND t.created BETWEEN :date_1 AND :date_2")
    Integer countUserTasksBetween2Dates(@Param("user_id") Long user_id,
                                        @Param("date_1") Date date_1,
                                        @Param("date_2") Date date_2);

    @Query(value = "SELECT t FROM Task t WHERE t.user_id = :id")
    ArrayList<Task> getUserTasks(@Param("id")Long id);

    @Query(value = "SELECT t FROM Task t WHERE t.id = :id")
    Task getTaskById(@Param("id")Long id);

    @Query(value = "SELECT t FROM Task t WHERE t.name = :name")
    Task getTaskByName(@Param("name")String name);

    @Query(value = "SELECT t.status FROM Task t WHERE t.id = :id AND t.user_id = :UserId")
    TaskStatusEnum getTaskStatusById(
            @Param("id") Long id,
            @Param("UserId") Long UserId);

    @Query(value = "SELECT t FROM Task t WHERE t.id = :id AND t.user_id = :UserId")
    Task getTaskById(
            @Param("id") Long id,
            @Param("UserId") Long UserId);


}
