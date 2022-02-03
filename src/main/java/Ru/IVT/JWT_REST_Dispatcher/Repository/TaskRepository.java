package Ru.IVT.JWT_REST_Dispatcher.Repository;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;

@Transactional
public interface TaskRepository extends JpaRepository<Task, Long>{


    @Modifying
    @Query(value = "UPDATE Task set data_file_name = :data_file, source_file_name=:source_file, status = :status WHERE id = :id AND user_id = :UserId")
    void updateTaskById(
            @Param("id") Long id,
            @Param("data_file") String data_file,
            @Param("source_file") String source_file,
            @Param("status") TaskStatusEnum status,
            @Param("UserId") Long UserId);

    @Modifying
    @Query(value = "UPDATE Task t SET t.data_file_name = :data_file, t.source_file_name=:source_file WHERE " +
            "t.name = :name AND t.user_id = :UserId")
    void updateTaskByName(
            @Param("name") String name,
            @Param("data_file") String data_file,
            @Param("source_file") String source_file,
            @Param("UserId") Long UserId);


    @Modifying
    @Query(value = "UPDATE Task t SET t.source_file_name=:source_file WHERE t.name = :name AND t.user_id = :UserId")
    void updateTaskSourceFileByName(
            @Param("name") String name,
            @Param("source_file") String source_file,
            @Param("UserId") Long UserId);

    @Modifying
    @Query(value = "UPDATE Task t SET t.data_file_name=:data_file WHERE t.name = :name AND t.user_id = :UserId")
    void updateTaskDataFileByName(
            @Param("name") String name,
            @Param("data_file") String data_file,
            @Param("UserId") Long UserId);

    @Modifying
    @Query(value = "UPDATE Task t SET t.source_file_name=:source_file WHERE t.id = :id AND t.user_id = :UserId")
    void updateTaskSourceFileById(
            @Param("id") Long id,
            @Param("source_file") String source_file,
            @Param("UserId") Long UserId);

    @Modifying
    @Query(value = "UPDATE Task t SET t.data_file_name=:data_file WHERE t.id = :id AND t.user_id = :UserId")
    void updateTaskDataFileById(
            @Param("id") Long id,
            @Param("data_file") String data_file,
            @Param("UserId") Long UserId);


    @Modifying
    @Query(value = "UPDATE Task t SET t.status=:status WHERE t.id = :id AND t.user_id = :UserId")
    void updateTaskStatusById(
            @Param("id") Long id,
            @Param("status") TaskStatusEnum status,
            @Param("UserId") Long UserId);

//    @Modifying


}
