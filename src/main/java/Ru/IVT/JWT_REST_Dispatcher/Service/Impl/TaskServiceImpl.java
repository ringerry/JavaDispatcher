package Ru.IVT.JWT_REST_Dispatcher.Service.Impl;

import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.DTO.UserDto;
import Ru.IVT.JWT_REST_Dispatcher.Model.Constanta;
import Ru.IVT.JWT_REST_Dispatcher.Model.Role;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;
import Ru.IVT.JWT_REST_Dispatcher.Repository.TaskRepository;
import Ru.IVT.JWT_REST_Dispatcher.Repository.TaskRepositoryNT;
import Ru.IVT.JWT_REST_Dispatcher.Repository.UserRepository;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskDoesNotExistException;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskLimitException;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
@Slf4j
public class TaskServiceImpl implements TaskService {
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskRepositoryNT taskRepositoryNT;
//    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public TaskServiceImpl(UserRepository userRepository, TaskRepository taskRepository,/*,
                           JwtTokenProvider jwtTokenProvider*/TaskRepositoryNT taskRepositoryNT) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
//        this.jwtTokenProvider = jwtTokenProvider;
        this.taskRepositoryNT = taskRepositoryNT;
    }

    private boolean isReachAddTaskLimit(Long userId/*,String token*/) {



//        Date tokenIssueAt = jwtTokenProvider.getIssueAt(token);
        Date now = new Date();
        Date hour_before = new Date(now.getTime() - Constanta.taskWindowLimitInMilliseconds);

        if(taskRepositoryNT.countUserTasksBetween2Dates(userId,hour_before,now)<Constanta.maxLimitTaskAtTokenTime)
            return true;
        else   return  false;

    }


    @Override
    public Task saveTask(NewTaskDto newTaskDto/*,String token*/ ) throws Exception {

        try {

            Task newTask = new Task();
            newTask.setName(newTaskDto.getTask_name());
            newTask.setStatus(newTaskDto.getStatus());
            newTask.setSource_file_name(newTaskDto.getSource_file_name());
            newTask.setData_file_name(newTaskDto.getData_file_name());
            newTask.setUser_id(newTaskDto.getUser_id());
            newTask.setCreated(new Date());
            newTask.setUpdated(new Date());



//            ArrayList<Role> = userRepository.getUserById(newTaskDto.getUser_id()).getRoles();
            List<Role> rolesList =  userRepository.getUserById(newTaskDto.getUser_id()).getRoles();

            boolean Admin = false;
            for (Role role: rolesList) {
                if(role.toString().indexOf("ADMIN")!=-1) Admin = true;
            }

            if(Admin){
                return taskRepository.save(newTask);
            }
            else {
                if(this.isReachAddTaskLimit(newTaskDto.getUser_id()))
                    return taskRepository.save(newTask);
                else{
                    throw new TaskLimitException("Превышено максимальное количество задач за время сессии." +
                            " Разрешено загружать "+Constanta.maxLimitTaskAtTokenTime.toString()+" задач(-и,-у)" +
                            " за "+Constanta.taskWindowLimitInMilliseconds/60000+" минут.");
                }
            }
//            return true;
        }
        catch (IllegalArgumentException exception){
            throw  new IllegalArgumentException(exception);
        }
        catch (TaskLimitException exc){
            throw exc;
        }
        catch (Exception exc){
            throw  new Exception(exc.getCause());
        }

    }

    @Override
    public boolean taskExist(UserDto userDto1, Long taskId) {

        ArrayList<Task> tasks = taskRepositoryNT.getUserTasks(userDto1.getId());
        for (Task task:
             tasks) {
            if (taskId==task.getId()) return true;
        }

        return false;
    }


    @Override
    public boolean taskExist(UserDto userDto1, String taskName) {

//        Date now = new Date();
//        Date hour_before = new Date(now.getTime() -10000000);
//
//        int a = taskRepository.countUserTasksBetween2Dates(15L,hour_before,now);
//
//        taskRepository.test();

        ArrayList<Task> tasks = taskRepositoryNT.getUserTasks(userDto1.getId());
        for (Task task:
                tasks) {
            if (taskName.equals(taskName)) return true;
        }

        return false;
    }

    @Override
    public Task getTaskById(Long taskId) throws Exception {
        Task task = taskRepositoryNT.getTaskById(taskId);

        if (task!=null){
            return task;
        }
        else {throw new TaskDoesNotExistException("Задачи "+taskId+"не существует");}
    }

    @Override
    public Task getTaskById(Long taskId,Long UserId) throws Exception {


        NewTaskDto newTaskDto = new NewTaskDto();

        newTaskDto.setId(taskId);

        if (isUserHaveTask(newTaskDto, UserId)){

            return taskRepositoryNT.getTaskById(taskId,UserId);
        }
        else {throw new TaskDoesNotExistException("Задачи с "+taskId+" не существует.");}
    }

    @Override
    public Task getTaskByName(String taskName) throws Exception {
        Task task = taskRepositoryNT.getTaskByName(taskName);

        if (task!=null){
            return task;
        }
        else {throw new TaskDoesNotExistException("Задачи "+taskName+" не существует");}
    }

    @Override
    public TaskStatusEnum getStatusById(Long taskId) throws Exception {


        try{return taskRepositoryNT.getTaskById(taskId).getStatus();}
        catch (Exception e){throw e;}


    }

    @Override
    public TaskStatusEnum getStatusByName(String taskName) throws Exception {
        try{return taskRepositoryNT.getTaskByName(taskName).getStatus();}
        catch (Exception e){throw e;}
    }

    @Override
    public Task updateTaskFilesByName(NewTaskDto newTaskDto, Long UserId) throws Exception {
        try{

            if (isUserHaveTask(newTaskDto, UserId)){
                taskRepository.updateTaskByName(newTaskDto.getTask_name(),
                        newTaskDto.getData_file_name(),newTaskDto.getSource_file_name(),UserId);
                return taskRepositoryNT.getTaskByName(newTaskDto.getTask_name());
            }
            else {throw new TaskDoesNotExistException("Задачи "+newTaskDto.getTask_name()+" не существует");}

        }
        catch (TaskDoesNotExistException e){
            throw e;
        }
        catch (Exception e){
            throw e;
        }
    }

    @Override
    public Task updateTaskSourceFileByName(NewTaskDto newTaskDto, Long UserId) throws Exception {
        try{


            if (isUserHaveTask(newTaskDto, UserId)){
                taskRepository.updateTaskSourceFileByName(newTaskDto.getTask_name(),
                        newTaskDto.getSource_file_name(),UserId);
                return taskRepositoryNT.getTaskByName(newTaskDto.getTask_name());
            }
            else {throw new TaskDoesNotExistException("Задачи "+newTaskDto.getTask_name()+" не существует");}

        }
        catch (TaskDoesNotExistException e){
            throw e;
        }
        catch (Exception e){
            throw e;
        }
    }

    @Override
    public Task updateTaskDataFileByName(NewTaskDto newTaskDto, Long UserId) throws Exception {
        try{

            if (isUserHaveTask(newTaskDto, UserId)){
                taskRepository.updateTaskDataFileByName(newTaskDto.getTask_name(),
                        newTaskDto.getData_file_name(),UserId);
                return taskRepositoryNT.getTaskByName(newTaskDto.getTask_name());
            }
            else {throw new TaskDoesNotExistException("Задачи "+newTaskDto.getTask_name()+" не существует");}

        }
        catch (TaskDoesNotExistException e){
            throw e;
        }
        catch (Exception e){
            throw e;
        }
    }

    @Override
    public Task updateTaskSourceFileById(NewTaskDto newTaskDto, Long UserId) throws Exception {
        try{

            if (isUserHaveTask(newTaskDto, UserId)){
                taskRepository.updateTaskSourceFileById(newTaskDto.getId(), newTaskDto.getSource_file_name(),UserId);
                return taskRepositoryNT.getTaskById(newTaskDto.getId());
            }
            else {throw new TaskDoesNotExistException("Задачи "+newTaskDto.getId()+" не существует");}

        }
        catch (TaskDoesNotExistException e){
            throw e;
        }
        catch (Exception e){
            throw e;
        }
    }

    @Override
    public Task updateTaskDataFileById(NewTaskDto newTaskDto, Long UserId) throws Exception,TaskDoesNotExistException {
        try{

            if (isUserHaveTask(newTaskDto, UserId)){
                taskRepository.updateTaskDataFileById(newTaskDto.getId(), newTaskDto.getData_file_name(), UserId);
                return taskRepositoryNT.getTaskById(newTaskDto.getId());
            }
            else {throw new TaskDoesNotExistException("Задачи "+newTaskDto.getId()+" не существует");}

        }
        catch (TaskDoesNotExistException e){
            throw e;
        }
        catch (Exception e){
            throw e;
        }
    }

    @Override
    public void updateTaskFilesById(NewTaskDto newTaskDto, Long UserId) throws Exception {
        try{

            if (isUserHaveTask(newTaskDto, UserId)){
                taskRepository.updateTaskById(newTaskDto.getId(),
                        newTaskDto.getData_file_name(),newTaskDto.getSource_file_name(),
                        newTaskDto.getStatus(),UserId);
            }
            else {throw new TaskDoesNotExistException("Задачи "+newTaskDto.getId()+" не существует");}


        }
        catch (TaskDoesNotExistException e){
            throw e;
        }
        catch (Exception e){
            throw e;
        }

    }

    @Override
    public void updateTaskStatus(NewTaskDto newTaskDto, Long UserId) throws Exception {
        try{

            if (isUserHaveTask(newTaskDto, UserId)){
                taskRepository.updateTaskStatusById(newTaskDto.getId(),newTaskDto.getStatus(),UserId);
//                taskRepository.updateTaskStatusById(newTaskDto.getId(),newTaskDto.getStatus(),UserId);

//                while(taskRepository.getTaskStatusById(newTaskDto.getId(),UserId)!=newTaskDto.getStatus()){
//                    taskRepository.updateTaskStatusById(newTaskDto.getId(),newTaskDto.getStatus(),UserId);
//                }

//                return taskRepository.getTaskById(newTaskDto.getId());
            }
            else {throw new TaskDoesNotExistException("Задачи "+newTaskDto.getId()+" не существует");}


        }
        catch (TaskDoesNotExistException e){
            throw e;
        }
        catch (Exception e){
            throw e;
        }
    }

    @Override
    public void updateTaskInsideStatus(NewTaskDto newTaskDto, Long userId) throws Exception {
        try{

            if (isUserHaveTask(newTaskDto, userId)){
                taskRepository.updateTaskInsideStatusById(newTaskDto.getId(),newTaskDto.getStatus(),userId);

            }
            else {throw new TaskDoesNotExistException("Задачи "+newTaskDto.getId()+" не существует");}


        }
        catch (TaskDoesNotExistException e){
            throw e;
        }
        catch (Exception e){
            throw e;
        }
    }

    @Override
    public List<Task> getUserTasks(Long UserId) {
        return taskRepositoryNT.getUserTasks(UserId);
    }
//
    @Override
    public ArrayList<Task> getTasksByStatus(TaskStatusEnum taskStatus) {

        return taskRepositoryNT.getTasksByStatus(taskStatus);

    }

    private boolean isUserHaveTask(NewTaskDto newTaskDto, Long userId) {
        boolean UserHaveTask = false;
//        taskRepositoryNT.u
        ArrayList<Task> tasks = taskRepositoryNT.getUserTasks(userId);
        for (Task task: tasks) {
            if (newTaskDto.getId().equals(task.getId())){
                UserHaveTask = true;
                break;
            }
        }
        return UserHaveTask;
    }

}
