package Ru.IVT.JWT_REST_Dispatcher.REST;

import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.DTO.UserDto;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.User;
import Ru.IVT.JWT_REST_Dispatcher.Security.Jwt.JwtTokenProvider;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskLimitException;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskService;
import Ru.IVT.JWT_REST_Dispatcher.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller user connected requestst.
 *
 * @author Eugene Suleimanov
 * @author Меньшиков Артём
 * @version 1.0
 */

// TODO еще одну сущноть пользователия для отправки
@RestController
@RequestMapping(value = "/api/")
public class UserRestControllerV1 {

    private final UserService userService;
    private final TaskService taskService;
//    private final DispathcerProvider dispathcerProvider;

    @Value("${local.paths.save.taskSources}")
    private String taskUploadPath;

    @Autowired
    public UserRestControllerV1(UserService userService, TaskService taskService/*, DispathcerProvider dispathcerProvider*/) {
        this.userService = userService;
        this.taskService = taskService;
//        this.dispathcerProvider = dispathcerProvider;
    }

    @GetMapping(value = "hello")
    public ResponseEntity<String> responseHello(){

        return new ResponseEntity<>("Здравствуйте!", HttpStatus.OK);
    }

    @PostMapping(value = "add_task",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity
        addTask(@RequestHeader("Authorization") String token,
                @RequestParam(value = "TaskName") String TaskName,
                @RequestParam(value = "TaskId", required = false) Long TaskId,
                @RequestParam(value = "CanNameDuplicate") boolean CanNameDuplicate,
                @RequestParam("TaskSourcesFile")MultipartFile TaskSourcesFile,
                @RequestParam("TaskDatfFile")MultipartFile TaskDataFile) throws Exception {


            // Проверка на повторяющиеся задачи
        try{
            User User1 = getUserByToken(token);
            UserDto UserDto1 = new UserDto();
            UserDto1.setId(User1.getId());

            if (TaskId==null){
                if (taskService.taskExist(UserDto1, TaskName)){

                    if(CanNameDuplicate){
                        // Обновить сущ
                        return getResponseEntityUpdateTask(token, TaskName,TaskId, TaskSourcesFile, TaskDataFile);
                    }
                    else{
                        // Новая задача
                        return getResponseEntityNewTask(token, TaskName, TaskSourcesFile, TaskDataFile);
                    }
                }
                else{
                    // Новая задача
                    return getResponseEntityNewTask(token, TaskName, TaskSourcesFile, TaskDataFile);
                }
            }
            else{
                // Обновить существующую задачу
                return getResponseEntityUpdateTask(token, TaskName,TaskId, TaskSourcesFile, TaskDataFile);
            }

//            ResponseEntity<Map<Object, Object>> FORBIDDEN = checkDuplicationTasks(token, TaskName);
//            if (FORBIDDEN != null) return FORBIDDEN;
        }
        catch (Exception exc){
            throw exc;
        }




    }

    private ResponseEntity getResponseEntityUpdateTask(String token,
                                                       String taskName,
                                                       Long taskId,
                                                       MultipartFile taskSourcesFile,
                                                       MultipartFile taskDataFile) throws Exception {

        try{
            if(!taskSourcesFile.isEmpty()&& !taskDataFile.isEmpty()){
                File uploadDir = new File(taskUploadPath);
                if (!uploadDir.exists()){
                    uploadDir.mkdir();
                }

                NewTaskDto newTaskDto = new NewTaskDto();
                newTaskDto.setTask_name(taskName);
                newTaskDto.setId(taskId);
                newTaskDto.setUser_id(getUserByToken(token).getId());
                newTaskDto.setData_file_name(getWholeFilePath(taskDataFile));
                newTaskDto.setSource_file_name(getWholeFilePath(taskSourcesFile));


                // Добавить в очередь, диспетчер сам просканирует, что выполнять
                newTaskDto.setStatus(TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА);
//                Task task = null;
                if(taskId==null){
                    taskService.updateTaskByName(newTaskDto);
                }
                else{
                    taskService.updateTaskById(newTaskDto);
                }

                String msg = "Задача "+ taskName +" обновлена. ";
                Map<Object,Object> response = new HashMap<>();

                response.put("Описание",msg);
//                response.put("id_задачи",task.getId());

                return ResponseEntity.ok(response);

            }
            else{

                Map<Object,Object> response = new HashMap<>();
                response.put("Описание","Нет исходников и/или данных");

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

        } catch (Exception exception){
            throw exception;
        }
    }


    private ResponseEntity getResponseEntityNewTask(String token,
                                                    String TaskName,
                                                    MultipartFile TaskSourcesFile,
                                                    MultipartFile TaskDataFile) throws Exception {
        try{
            if(!TaskSourcesFile.isEmpty()&& !TaskDataFile.isEmpty()){
                File uploadDir = new File(taskUploadPath);
                if (!uploadDir.exists()){
                    uploadDir.mkdir();
                }

                NewTaskDto newTaskDto = new NewTaskDto();
                newTaskDto.setTask_name(TaskName);
                newTaskDto.setUser_id(getUserByToken(token).getId());
                newTaskDto.setData_file_name(getWholeFilePath(TaskDataFile));
                newTaskDto.setSource_file_name(getWholeFilePath(TaskSourcesFile));


                // Добавить в очередь, диспетчер сам просканирует, что выполнять
                newTaskDto.setStatus(TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА);
                Task task = taskService.saveTask(newTaskDto);

                String msg = "Задача "+ TaskName +" добавлена в обработку. " +
                        "Имя файла:"+ TaskSourcesFile.getOriginalFilename()+".";
                Map<Object,Object> response = new HashMap<>();

                response.put("Описание",msg);
                response.put("id_задачи",task.getId());

                return ResponseEntity.ok(response);

            }
            else{

                Map<Object,Object> response = new HashMap<>();
                response.put("Описание","Нет исходников и/или данных");

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

        }
        catch (TaskLimitException exc){
            Map<Object,Object> response = new HashMap<>();
            response.put("Описание",exc.getMessage());

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        catch (Exception exception){
            throw new Exception(exception);
        }
    }


    @PostMapping(value = "add_sources",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity
    addSources(@RequestHeader("Authorization") String token,
               @RequestParam("TaskName") String TaskName,
               @RequestParam("TaskSourcesFile")MultipartFile TaskSourcesFile) throws Exception {

        try{


            if(!TaskSourcesFile.isEmpty()){
                File uploadDir = new File(taskUploadPath);
                if (!uploadDir.exists()){
                    uploadDir.mkdir();
                }

                NewTaskDto newTaskDto = new NewTaskDto();
                newTaskDto.setTask_name(TaskName);
                newTaskDto.setUser_id(getUserByToken(token).getId());
//                newTaskDto.setData_file_name(getWholeFilePath(TaskDataFile));
                newTaskDto.setSource_file_name(getWholeFilePath(TaskSourcesFile));


                // Добавить в очередь, диспетчер сам просканирует, что выполнять
                newTaskDto.setStatus(TaskStatusEnum.В_ОЧЕРЕДИ);
                Task task = taskService.saveTask(newTaskDto);

                String msg = "Задача "+ TaskName+" добавлена в обработку. " +
                        "Имя файла:"+ TaskSourcesFile.getOriginalFilename()+".";
                Map<Object,Object> response = new HashMap<>();

                response.put("Описание",msg);
                response.put("id_задачи",task.getId());

                return ResponseEntity.ok(response);

            }
            else{

                Map<Object,Object> response = new HashMap<>();
                response.put("Описание","Нет исходников и/или данных");

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

        }
        catch (TaskLimitException exc){
            Map<Object,Object> response = new HashMap<>();
            response.put("Описание",exc.getMessage());

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        catch (Exception exception){
            throw new Exception(exception);
        }


    }

//    private ResponseEntity<Map<Object, Object>> checkDuplicationTasks(String token, String TaskName) {
//        User User1 = getUserByToken(token);
//        UserDto UserDto1 = new UserDto();
//        UserDto1.setId(User1.getId());
//
//        if (taskService.taskExist(UserDto1, TaskName)){
//            Map<Object,Object> response = new HashMap<>();
//            response.put("Описание","Задача "+ TaskName +" уже существует. В пределах пользователя имя задачи " +
//                    "должно быть уникальным.");
//
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
//        }
//        return null;
//    }

    private User getUserByToken(String token) {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider();
        token = token.replace ("Bearer_", "");
        String UserName = jwtTokenProvider.getUsername(token);
        User User1 = userService.findByUsername(UserName);
        return User1;
    }

    private String getWholeFilePath(MultipartFile TaskSourcesFile) throws IOException {
        String [] fileParts = TaskSourcesFile.getOriginalFilename().split("[.]");
        String uniqFileName = UUID.randomUUID().toString();
        String resFileName =  fileParts[0]+"."+ uniqFileName+"."+fileParts[1];
        String sourceFileAllPath = taskUploadPath +"\\"+ resFileName;
        TaskSourcesFile.transferTo(new File(sourceFileAllPath));
        return sourceFileAllPath;
    }




}
