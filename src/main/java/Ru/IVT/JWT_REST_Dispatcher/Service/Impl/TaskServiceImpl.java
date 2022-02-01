package Ru.IVT.JWT_REST_Dispatcher.Service.Impl;

import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;
import Ru.IVT.JWT_REST_Dispatcher.Repository.TaskRepository;
import Ru.IVT.JWT_REST_Dispatcher.Repository.UserRepository;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
public class TaskServiceImpl implements TaskService {
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    @Autowired
    public TaskServiceImpl(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public Task saveTask(NewTaskDto newTaskDto ) throws Exception {

        try {

            Task newTask = new Task();
            newTask.setName(newTaskDto.getTask_name());
            newTask.setStatus(newTaskDto.getStatus());
            newTask.setSource_file_name(newTaskDto.getData_file_name());
            newTask.setData_file_name(newTaskDto.getSource_file_name());
            newTask.setUser_id(newTaskDto.getUser_id());
            newTask.setCreated(new Date());
            newTask.setUpdated(new Date());

            return taskRepository.save(newTask);
//            return true;
        }
        catch (IllegalArgumentException exception){
            throw  new IllegalArgumentException(exception);
        }
        catch (Exception exc){
            throw  new Exception(exc.getCause());
        }

    }

}
