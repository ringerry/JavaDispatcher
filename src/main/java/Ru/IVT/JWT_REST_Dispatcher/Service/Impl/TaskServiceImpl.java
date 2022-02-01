package Ru.IVT.JWT_REST_Dispatcher.Service.Impl;

import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.Model.Constanta;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Repository.TaskRepository;
import Ru.IVT.JWT_REST_Dispatcher.Repository.UserRepository;
import Ru.IVT.JWT_REST_Dispatcher.Security.Jwt.JwtTokenProvider;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskLimitException;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
@Slf4j
public class TaskServiceImpl implements TaskService {
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
//    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public TaskServiceImpl(UserRepository userRepository, TaskRepository taskRepository/*,
                           JwtTokenProvider jwtTokenProvider*/) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
//        this.jwtTokenProvider = jwtTokenProvider;
    }

    private boolean isReachAddTaskLimit(Long userId/*,String token*/) {



//        Date tokenIssueAt = jwtTokenProvider.getIssueAt(token);
        Date now = new Date();
        Date hour_before = new Date(now.getTime() - Constanta.taskWindowLimitInMilliseconds);

        if(taskRepository.countUserTasksBetween2Dates(userId,hour_before,now)<Constanta.maxLimitTaskAtTokenTime)
            return true;
        else   return  false;

    }


    @Override
    public Task saveTask(NewTaskDto newTaskDto/*,String token*/ ) throws Exception {

        try {

            Task newTask = new Task();
            newTask.setName(newTaskDto.getTask_name());
            newTask.setStatus(newTaskDto.getStatus());
            newTask.setSource_file_name(newTaskDto.getData_file_name());
            newTask.setData_file_name(newTaskDto.getSource_file_name());
            newTask.setUser_id(newTaskDto.getUser_id());
            newTask.setCreated(new Date());
            newTask.setUpdated(new Date());

            if(this.isReachAddTaskLimit(newTaskDto.getUser_id()))
                return taskRepository.save(newTask);
            else{
                throw new TaskLimitException("Превышено максимальное количество задач за время сессии." +
                        "Разрешено загружать "+Constanta.maxLimitTaskAtTokenTime.toString()+" задач(-и,-у)" +
                        " за "+Constanta.taskWindowLimitInMilliseconds/60000+" минут.");
            }
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
