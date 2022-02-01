package Ru.IVT.JWT_REST_Dispatcher.Service;

import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import org.springframework.stereotype.Repository;

public interface TaskService {
    Task saveTask(NewTaskDto newTaskDto) throws Exception;
}
