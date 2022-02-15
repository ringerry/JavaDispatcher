package Ru.IVT.JWT_REST_Dispatcher.Service;

import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.DTO.UserDto;
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
    void updateTaskInsideStatus(NewTaskDto newTaskDto, Long userId) throws Exception;

    List<Task> getUserTasks(Long UserId);

    ArrayList<Task> getTasksByStatus(TaskStatusEnum taskStatus);

}
