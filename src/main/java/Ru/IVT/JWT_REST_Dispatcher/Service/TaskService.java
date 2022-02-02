package Ru.IVT.JWT_REST_Dispatcher.Service;

import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.DTO.UserDto;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;

public interface TaskService {
    Task saveTask(NewTaskDto newTaskDto) throws Exception;

    boolean taskExist(UserDto userDto1, String taskName);

    boolean taskExist(UserDto userDto1, Long taskId);

    Task getTaskById(Long taskId) throws Exception;

    void updateTaskByName(NewTaskDto newTaskDto);

    void updateTaskById(NewTaskDto newTaskDto);
}
