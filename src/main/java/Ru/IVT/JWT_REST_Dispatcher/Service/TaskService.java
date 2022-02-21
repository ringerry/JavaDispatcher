package Ru.IVT.JWT_REST_Dispatcher.Service;

import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.DTO.UserDto;
import Ru.IVT.JWT_REST_Dispatcher.Model.InsideTaskStatusEnum;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;

import java.util.ArrayList;
import java.util.List;

public interface TaskService {
    Task saveTask(NewTaskDto newTaskDto) throws Exception;

    boolean taskExist(UserDto userDto1, String taskName);

    boolean taskExist(UserDto userDto1, Long taskId);

    Task getTaskById(Long taskId) throws Exception;

    Task getTaskById(Long taskId, Long UserId) throws Exception;

    Task getTaskByName(String taskName) throws Exception;

    TaskStatusEnum getStatusById(Long taskId) throws Exception;
    TaskStatusEnum getStatusByName(String taskName) throws Exception;

    Task updateTaskFilesByName(NewTaskDto newTaskDto, Long userId) throws Exception;
    Task updateTaskSourceFileByName(NewTaskDto newTaskDto, Long UserId) throws Exception;
    Task updateTaskDataFileByName(NewTaskDto newTaskDto, Long UserId) throws Exception;

    Task updateTaskSourceFileById(NewTaskDto newTaskDto, Long UserId) throws Exception;
    Task updateTaskDataFileById(NewTaskDto newTaskDto, Long UserId) throws Exception;

    void updateTaskFilesById(NewTaskDto newTaskDto, Long userId) throws Exception;

    void updateTaskStatus(NewTaskDto newTaskDto, Long userId) throws Exception;
    void updateTaskStatusByTaskId(NewTaskDto newTaskDto) throws Exception;
    void updateInsideTaskStatus(NewTaskDto newTaskDto) throws Exception;

    void updateInsideTaskStatusWithUserId(NewTaskDto newTaskDto, Long userId) throws Exception;

    void deleteTask(NewTaskDto newTaskDto,Long userId) throws Exception;

    String getUnzipDirById(Long taskId,Long UserID) throws TaskDoesNotExistException;

    List<Task> getUserTasks(Long UserId);

    ArrayList<Task> getTasksByStatus(TaskStatusEnum taskStatus);
    ArrayList<Task> getTasksByInsideStatus(InsideTaskStatusEnum insideTaskStatus);
    ArrayList<Task> getAllTasks();

    void setTest(String test);

    String getTest();

}
