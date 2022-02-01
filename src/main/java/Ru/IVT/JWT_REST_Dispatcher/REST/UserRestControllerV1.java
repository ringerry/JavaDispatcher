package Ru.IVT.JWT_REST_Dispatcher.REST;

import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.DispathcerProvider;
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
    public ResponseEntity<String>
        addTask(@RequestHeader("Authorization") String token,
                @RequestParam("TaskName") String TaskName,
                @RequestParam("TaskSourcesFile")MultipartFile TaskSourcesFile,
                @RequestParam("TaskDatfFile")MultipartFile TaskDataFile) throws Exception {

        try{


            if(TaskSourcesFile != null){
                File uploadDir = new File(taskUploadPath);
                if (!uploadDir.exists()){
                    uploadDir.mkdir();
                }

                // Сохранение исходников
                String [] fileParts = TaskSourcesFile.getOriginalFilename().split("[.]");
                String uniqFileName = UUID.randomUUID().toString();
                String resFileName =  fileParts[0]+"."+ uniqFileName+"."+fileParts[1];
                String sourceFileAllPath = taskUploadPath +"\\"+ resFileName;
                TaskSourcesFile.transferTo(new File(sourceFileAllPath));

                // Сохранение данных
                fileParts = TaskDataFile.getOriginalFilename().split("[.]");
                uniqFileName = UUID.randomUUID().toString();
                resFileName =  fileParts[0]+"."+ uniqFileName+"."+fileParts[1];
                String dataFileAllPath = taskUploadPath +"\\"+ resFileName;
                TaskDataFile.transferTo(new File(dataFileAllPath));

                //Добавление записи в БД==


                // Получени имени по токену=
                JwtTokenProvider jwtTokenProvider = new JwtTokenProvider();
                token = token.replace ("Bearer_", "");
                String UserName = jwtTokenProvider.getUsername(token);
                User User1 = userService.findByUsername(UserName);



                NewTaskDto newTaskDto = new NewTaskDto();
                newTaskDto.setTask_name(TaskName);
                newTaskDto.setUser_id(User1.getId());
                newTaskDto.setData_file_name(dataFileAllPath);
                newTaskDto.setSource_file_name(sourceFileAllPath);





                // Добавить в очередь, диспетчер сам просканирует, что выполнять
                newTaskDto.setStatus(TaskStatusEnum.В_ОЧЕРЕДИ);
                Task task = taskService.saveTask(newTaskDto);

                //Если диспетчер свободен сразу запустить, иначе в очередь(по сути ничего не делать: диспетчер сам
                // знает очередь по таблицам )

//                if(!dispathcerProvider.isDispatcherComputing()){
//                    newTaskDto.setStatus(TaskStatusEnum.ВЫПОЛНЕНИЕ);
//                    Task task = taskService.saveTask(newTaskDto);
//
//                    Long a = task.getId();
//                    Long b = Long.valueOf(1);// отладка
//
//                    dispathcerProvider.run(task.getId());
//
//                }
//                else {
//                    newTaskDto.setStatus(TaskStatusEnum.В_ОЧЕРЕДИ);
//                    Task task = taskService.saveTask(newTaskDto);
//                }


                return new ResponseEntity<>("Задача "+ TaskName+" добавлена в обработку. " +
                        "Имя файла:"+ TaskSourcesFile.getOriginalFilename()+".",
                        HttpStatus.OK);
            }
            else{
                return new ResponseEntity<>("Нет имени задачи и/или файла",
                        HttpStatus.NO_CONTENT);
            }


        }
        catch (TaskLimitException exc){
            return new ResponseEntity<>(exc.toString(),
                    HttpStatus.NOT_ACCEPTABLE);
        }
        catch (Exception exception){
            throw new Exception(exception);
        }


    }

//    @PostMapping(value = "add_task",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<String>
//    addSources(@RequestHeader("Authorization") String token,
//            @RequestParam("TaskName") String TaskName,
//            @RequestParam("TaskSourcesFile")MultipartFile TaskSourcesFile,
//            @RequestParam("TaskDatfFile")MultipartFile TaskDataFile) throws Exception {
//
//        try{
//
//
//            if(TaskSourcesFile != null){
//                File uploadDir = new File(taskUploadPath);
//                if (!uploadDir.exists()){
//                    uploadDir.mkdir();
//                }
//
//                // Сохранение исходников
//                String [] fileParts = TaskSourcesFile.getOriginalFilename().split("[.]");
//                String uniqFileName = UUID.randomUUID().toString();
//                String resFileName =  fileParts[0]+"."+ uniqFileName+"."+fileParts[1];
//                String sourceFileAllPath = taskUploadPath +"\\"+ resFileName;
//                TaskSourcesFile.transferTo(new File(sourceFileAllPath));
//
//                // Сохранение данных
//                fileParts = TaskDataFile.getOriginalFilename().split("[.]");
//                uniqFileName = UUID.randomUUID().toString();
//                resFileName =  fileParts[0]+"."+ uniqFileName+"."+fileParts[1];
//                String dataFileAllPath = taskUploadPath +"\\"+ resFileName;
//                TaskDataFile.transferTo(new File(dataFileAllPath));
//
//                //Добавление записи в БД==
//
//
//                // Получени имени по токену=
//                JwtTokenProvider jwtTokenProvider = new JwtTokenProvider();
//                token = token.replace ("Bearer_", "");
//                String UserName = jwtTokenProvider.getUsername(token);
//                User User1 = userService.findByUsername(UserName);
//
//
//
//                NewTaskDto newTaskDto = new NewTaskDto();
//                newTaskDto.setTask_name(TaskName);
//                newTaskDto.setUser_id(User1.getId());
//                newTaskDto.setData_file_name(dataFileAllPath);
//                newTaskDto.setSource_file_name(sourceFileAllPath);
//
//
//
//
//
//                // Добавить в очередь, диспетчер сам просканирует, что выполнять
//                newTaskDto.setStatus(TaskStatusEnum.В_ОЧЕРЕДИ);
//                Task task = taskService.saveTask(newTaskDto);
//
//                //Если диспетчер свободен сразу запустить, иначе в очередь(по сути ничего не делать: диспетчер сам
//                // знает очередь по таблицам )
//
////                if(!dispathcerProvider.isDispatcherComputing()){
////                    newTaskDto.setStatus(TaskStatusEnum.ВЫПОЛНЕНИЕ);
////                    Task task = taskService.saveTask(newTaskDto);
////
////                    Long a = task.getId();
////                    Long b = Long.valueOf(1);// отладка
////
////                    dispathcerProvider.run(task.getId());
////
////                }
////                else {
////                    newTaskDto.setStatus(TaskStatusEnum.В_ОЧЕРЕДИ);
////                    Task task = taskService.saveTask(newTaskDto);
////                }
//
//
//                return new ResponseEntity<>("Задача "+ TaskName+" добавлена в обработку. " +
//                        "Имя файла:"+ TaskSourcesFile.getOriginalFilename()+".",
//                        HttpStatus.OK);
//            }
//            else{
//                return new ResponseEntity<>("Нет имени задачи и/или файла",
//                        HttpStatus.NO_CONTENT);
//            }
//
//
//        }
//        catch (TaskLimitException exc){
//            return new ResponseEntity<>(exc.toString(),
//                    HttpStatus.NOT_ACCEPTABLE);
//        }
//        catch (Exception exception){
//            throw new Exception(exception);
//        }
//
//
//    }

}
