package Ru.IVT.JWT_REST_Dispatcher.REST;

import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.DTO.UserDto;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.User;
import Ru.IVT.JWT_REST_Dispatcher.Security.Jwt.JwtTokenProvider;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskDoesNotExistException;
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
import java.util.List;
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
                @RequestParam(value = "CanNameDuplicate") boolean CanNameDuplicate,
                @RequestParam("TaskSourcesFile")MultipartFile TaskSourcesFile,
                @RequestParam("TaskDataFile")MultipartFile TaskDataFile) throws Exception {


        try{
            User User1 = getUserByToken(token);
            UserDto UserDto1 = new UserDto();
            UserDto1.setId(User1.getId());

            if(CanNameDuplicate){
                return getResponseEntityNewTask(token, TaskName, TaskSourcesFile, TaskDataFile);
            }
            else{
                // Проверка на повторяющиеся задачи
                if(!taskService.taskExist(UserDto1, TaskName)){
                    return getResponseEntityNewTask(token, TaskName, TaskSourcesFile, TaskDataFile);
                }
                else {
                    String msg = "Задача "+ TaskName +" Уже существует. " +
                            " Имя задачи должно быть уникальным";
                    Map<Object,Object> response = new HashMap<>();

                    response.put("Описание",msg);
//                response.put("id_задачи",task.getId());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                }
            }
        }
        catch (Exception exc){
            throw exc;
        }

    }

    @PostMapping(value = "add_sources",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity
    addSources(@RequestHeader("Authorization") String token,
               @RequestParam(value = "TaskName") String TaskName,
               @RequestParam(value = "TaskId",required = false) Long TaskId,
               @RequestParam(value = "CanNameDuplicate") boolean CanNameDuplicate,
               @RequestParam("TaskSourcesFile")MultipartFile TaskSourcesFile) throws Exception {


        // Проверка на повторяющиеся задачи
        try{

            User User1 = getUserByToken(token);
            UserDto UserDto1 = new UserDto();
            UserDto1.setId(User1.getId());

            if (TaskId==null){

                // Новые исходники
                if(CanNameDuplicate){
                    return getResponseEntityNewTask(token, TaskName, TaskSourcesFile, null);
                }
                else{
                    // Проверка на повторяющиеся задачи
                    if(!taskService.taskExist(UserDto1, TaskName)){
                        return getResponseEntityNewTask(token, TaskName, TaskSourcesFile, null);
                    }
                    else {
                        String msg = "Задача "+ TaskName +" Уже существует. " +
                                " Имя задачи должно быть уникальным";
                        Map<Object,Object> response = new HashMap<>();

                        response.put("Описание",msg);

                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                    }
                }

            }
            else{
                // Обновить существующую задачу

                Task task = taskService.getTaskById(TaskId);

                if (isCanUpdateTask(task)){

                    return getResponseEntityUpdateTask(token, TaskName,TaskId, TaskSourcesFile,
                            null,User1.getId());
                }
                else {

                    String msg = "Изменять исходники разрешено, если задача находится в одном из состояний:"+
                                 getWhiteTaskUpdateList();

                    Map<Object,Object> response = new HashMap<>();
                    response.put("Описание",msg);

                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }


            }

        }
        catch (TaskDoesNotExistException e){
            Map<Object,Object> response = new HashMap<>();
            response.put("Описание",e.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        catch (Exception exc){
            throw exc;
        }

    }

    private String getWhiteTaskUpdateList() {
       return TaskStatusEnum.ОЖИДАНИЕ_ИСХОДНИКОВ+", или "+TaskStatusEnum.ОЖИДАНИЕ_ДАННЫХ+", или "+
                TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА+", или "+TaskStatusEnum.ОШИБКА_КОМПИЛЯЦИИ+", или "+
               TaskStatusEnum.ОШИБКА_ВЫПОЛНЕНИЯ+".";
    }

    private boolean isCanUpdateTask(Task task) {
        return task.getStatus() == TaskStatusEnum.ОЖИДАНИЕ_ИСХОДНИКОВ ||
                task.getStatus() == TaskStatusEnum.ОЖИДАНИЕ_ДАННЫХ ||
                task.getStatus() == TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА||
                task.getStatus() == TaskStatusEnum.ОШИБКА_ВЫПОЛНЕНИЯ||
                task.getStatus() == TaskStatusEnum.ОШИБКА_КОМПИЛЯЦИИ;
    }

    @PostMapping(value = "add_data",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity
    addData(@RequestHeader("Authorization") String token,
            @RequestParam(value = "TaskName") String TaskName,
            @RequestParam(value = "TaskId",required = false) Long TaskId,
            @RequestParam(value = "CanNameDuplicate") boolean CanNameDuplicate,
            @RequestParam("TaskDataFile")MultipartFile TaskDataFile) throws Exception {


        // Проверка на повторяющиеся задачи
        try{

            User User1 = getUserByToken(token);
            UserDto UserDto1 = new UserDto();
            UserDto1.setId(User1.getId());

            if (TaskId==null){

                // Новые исходники
                if(CanNameDuplicate){
                    return getResponseEntityNewTask(token, TaskName, null, TaskDataFile);
                }
                else{
                    // Проверка на повторяющиеся задачи
                    if(!taskService.taskExist(UserDto1, TaskName)){
                        return getResponseEntityNewTask(token, TaskName, null, TaskDataFile);
                    }
                    else {
                        String msg = "Задача "+ TaskName +" Уже существует. " +
                                " Имя задачи должно быть уникальным";
                        Map<Object,Object> response = new HashMap<>();

                        response.put("Описание",msg);
                        //                response.put("id_задачи",task.getId());
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                    }
                }

            }
            else{
                // Обновить существующую задачу

                Task task = taskService.getTaskById(TaskId);

                if (isCanUpdateTask(task)){

                    return getResponseEntityUpdateTask(token, TaskName,TaskId, null,
                            TaskDataFile,User1.getId());
                }
                else {

                    String msg = "Изменять данные разрешено, если задача находится в одном из состояний:"+
                            getWhiteTaskUpdateList();

                    Map<Object,Object> response = new HashMap<>();
                    response.put("Описание",msg);

                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }


            }

        }
        catch (Exception exc){
            throw exc;
        }

    }



    @PostMapping(value = "update_task",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity
    updateTask(@RequestHeader("Authorization") String token,
            @RequestParam(value = "TaskName") String TaskName,
            @RequestParam(value = "TaskId") Long TaskId,
            @RequestParam("TaskSourcesFile")MultipartFile TaskSourcesFile,
            @RequestParam("TaskDataFile")MultipartFile TaskDataFile) throws Exception {


        // Проверка на повторяющиеся задачи
        try{

            User User1 = getUserByToken(token);

            Task task = taskService.getTaskById(TaskId);

            if (isCanUpdateTask(task)){

                return getResponseEntityUpdateTask(token, TaskName,TaskId, TaskSourcesFile,
                        TaskDataFile,User1.getId());
            }
            else {

                String msg = "Изменять задачу разрешено, если она находится в одном из состояний:"+
                                getWhiteTaskUpdateList();

                Map<Object,Object> response = new HashMap<>();
                response.put("Описание",msg);

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }


        }
        catch (Exception exc){
            throw exc;
        }

    }


    @PostMapping(value = "update_sources",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity
    updateSources(@RequestHeader("Authorization") String token,
               @RequestParam(value = "TaskName") String TaskName,
               @RequestParam(value = "TaskId") Long TaskId,
               @RequestParam("TaskSourcesFile")MultipartFile TaskSourcesFile) throws Exception {


        // Проверка на повторяющиеся задачи
        try{
            User User1 = getUserByToken(token);
            return getResponseEntityUpdateTask(token, TaskName,TaskId, TaskSourcesFile, null, User1.getId());
        }
        catch (Exception exc){
            throw exc;
        }

    }

    @PostMapping(value = "update_data",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity
    updateData(@RequestHeader("Authorization") String token,
                  @RequestParam(value = "TaskName") String TaskName,
                  @RequestParam(value = "TaskId") Long TaskId,
                  @RequestParam("TaskDataFile")MultipartFile TaskDataFile) throws Exception {


        // Проверка на повторяющиеся задачи
        try{
            User User1 = getUserByToken(token);
            return getResponseEntityUpdateTask(token, TaskName,TaskId, null, TaskDataFile, User1.getId());
        }
        catch (Exception exc){
            throw exc;
        }

    }

    @PostMapping(value = "task_list",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity
    taskList(@RequestHeader("Authorization") String token) throws Exception {


        // Проверка на повторяющиеся задачи
        try{
            User User1 = getUserByToken(token);

            List<Task> taskList = taskService.getUserTasks(User1.getId());

            // подготовка в json

            String msg = "Список всех задач ";
            Map<Object,Object> response = new HashMap<>();

            response.put("Описание",msg);
            response.put("Задачи",taskList);
//                response.put("id_задачи",task.getId());

            return ResponseEntity.ok(response);

        }
        catch (Exception exc){
            throw exc;
        }

    }

    @PostMapping(value = "task_status",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity
    getTaskStatus(@RequestHeader("Authorization") String token,
                  @RequestParam(value = "TaskName") String TaskName,
                  @RequestParam(value = "TaskId") Long TaskId) throws Exception {


        // Проверка на повторяющиеся задачи
        try{
            User User1 = getUserByToken(token);

            Task task = taskService.getTaskById(TaskId,User1.getId());

            // подготовка в json

            String msg = "Состояние задачи ";
            Map<Object,Object> response = new HashMap<>();

            response.put("Описание",msg);
            response.put("Состояние",task.getStatus().toString());
            response.put("Id_задачи",task.getId());
//                response.put("id_задачи",task.getId());

            return ResponseEntity.ok(response);

        }
        catch (TaskDoesNotExistException e ){
            Map<Object,Object> response = new HashMap<>();
            response.put("Описание",e.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        catch (Exception exc){
            throw exc;
        }

    }

    @PostMapping(value = "run_task",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity
    runTask(@RequestHeader("Authorization") String token,
                  @RequestParam(value = "TaskName") String TaskName,
                  @RequestParam(value = "TaskId") Long TaskId) throws Exception {


        // Проверка на повторяющиеся задачи
        try{

            Task runTask = taskService.getTaskById(TaskId);

            if(runTask.getStatus()==TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА){
                User User1 = getUserByToken(token);

                NewTaskDto newTaskDto = new NewTaskDto();
                newTaskDto.setId(TaskId);
                newTaskDto.setStatus(TaskStatusEnum.В_ОЧЕРЕДИ);

                taskService.updateTaskStatus(newTaskDto,User1.getId());


                String msg = "Задача добавленая в очередь выполнения.";
                Map<Object,Object> response = new HashMap<>();

                response.put("Описание",msg);
                response.put("Состояние",TaskStatusEnum.В_ОЧЕРЕДИ);
                response.put("Id_задачи",TaskId);

                return ResponseEntity.ok(response);
            }
            else if(runTask.getStatus()==TaskStatusEnum.В_ОЧЕРЕДИ){
                String msg = "Задача уже запущена и находится в очереди на выополнение.";

                Map<Object, Object> response = getBasicResponseBody(runTask, msg);

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            else if(runTask.getStatus()==TaskStatusEnum.ВЫПОЛНЕНИЕ){
                String msg = "Задача выполняется.";

                Map<Object, Object> response = getBasicResponseBody(runTask, msg);

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            else{

                String msg = "Невозоможно запустить задачу. Отстствуют исходники или данные, или ошибка компилятора";

                Map<Object, Object> response = getBasicResponseBody(runTask, msg);

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }



        }
        catch (TaskDoesNotExistException e ){
            Map<Object,Object> response = new HashMap<>();
            response.put("Описание",e.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        catch (Exception exc){
            throw exc;
        }

    }

    private Map<Object, Object> getBasicResponseBody(Task runTask, String msg) {
        Map<Object,Object> response = new HashMap<>();
        response.put("Описание", msg);
        response.put("Состояние", runTask.getStatus());
        response.put("Id_задачи", runTask.getId());
        return response;
    }

    private ResponseEntity getResponseEntityUpdateTask(String token,
                                                       String taskName,
                                                       Long taskId,
                                                       MultipartFile taskSourcesFile,
                                                       MultipartFile taskDataFile, Long UserId) throws Exception {

        try{

            createFolderIfNotExist(taskUploadPath);

            if (taskId==null){
                // Оба файла пуcты
                Map<Object,Object> response = new HashMap<>();
                response.put("Описание","Пустой id");

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }


            User User1 = getUserByToken(token);
            UserDto UserDto1 = new UserDto();
            UserDto1.setId(User1.getId());

            if((taskSourcesFile != null && taskDataFile!=null)&&
                    (!taskSourcesFile.isEmpty()&&!taskDataFile.isEmpty())){

                NewTaskDto newTaskDto = getNewTaskDto(token, taskName, taskId, taskSourcesFile, taskDataFile);
                newTaskDto.setStatus(TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА);


                taskService.updateTaskFilesById(newTaskDto,UserId);

                Task task = taskService.getTaskById(newTaskDto.getId());

                String msg = "Задача "+ task.getName()+"(id = "+task.getId()+")" +" обновлена. ";
                Map<Object,Object> response = new HashMap<>();

                response.put("Описание",msg);
                response.put("Id_задачи",task.getId());
                response.put("Состояние",TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА);
//                response.put("id_задачи",task.getId());

                return ResponseEntity.ok(response);

            }
            else if(taskSourcesFile != null && taskDataFile==null&&
                    (!taskSourcesFile.isEmpty()))
            {
                // Обновить исходники
                NewTaskDto newTaskDto = getNewTaskDto(token, taskName, taskId, taskSourcesFile, taskDataFile);


                // Добавить в очередь, диспетчер сам просканирует, что выполнять


                newTaskDto.setData_file_name(taskService.getTaskById(taskId).getData_file_name());
                newTaskDto.setStatus(taskService.getTaskById(taskId).getStatus());

                String oldDataFileName = taskService.getTaskById(taskId).getData_file_name();
                TaskStatusEnum TaskState = taskService.getTaskById(taskId).getStatus();

                taskService.updateTaskFilesById(newTaskDto,UserId);

                Task retTask = taskService.getTaskById(taskId);
                if (oldDataFileName != null){
                    newTaskDto.setStatus(TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА);
                    TaskState = TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА;
                }
                else {
                    newTaskDto.setStatus(TaskStatusEnum.ОЖИДАНИЕ_ДАННЫХ);
                    TaskState = TaskStatusEnum.ОЖИДАНИЕ_ДАННЫХ;
                }

                taskService.updateTaskStatus(newTaskDto,UserId);


                String msg = "Исходники к задаче "+ retTask.getName()+"(id = "+retTask.getId()+")" +
                        " добавлены/обновлены. ";
                Map<Object,Object> response = new HashMap<>();

                response.put("Описание",msg);
                response.put("Id_задачи",retTask.getId());
                response.put("Состояние",TaskState);

                return ResponseEntity.ok(response);

            }
            else if (taskSourcesFile == null && taskDataFile!=null&&
                    (!taskDataFile.isEmpty())){
                //Обновить данные

                NewTaskDto newTaskDto = getNewTaskDto(token, taskName, taskId, taskSourcesFile, taskDataFile);
                // Добавить в очередь, диспетчер сам просканирует, что выполнять


                newTaskDto.setSource_file_name(taskService.getTaskById(taskId).getSource_file_name());
                newTaskDto.setStatus(taskService.getTaskById(taskId).getStatus());

                String oldSourceFileName = taskService.getTaskById(taskId).getSource_file_name();
                TaskStatusEnum TaskState = null;

                taskService.updateTaskFilesById(newTaskDto,UserId);


                Task retTask = taskService.getTaskById(taskId);


                if (oldSourceFileName != null){
                    newTaskDto.setStatus(TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА);
                    TaskState = TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА;
                }
                else {
                    newTaskDto.setStatus(TaskStatusEnum.ОЖИДАНИЕ_ИСХОДНИКОВ);
                    TaskState = TaskStatusEnum.ОЖИДАНИЕ_ИСХОДНИКОВ;
                }

                taskService.updateTaskStatus(newTaskDto,UserId);

                String msg = "Данные к задаче "+ retTask.getName()+"(id = "+retTask.getId()+")" +
                        " добавлены/обновлены. ";
                Map<Object,Object> response = new HashMap<>();

                response.put("Описание",msg);
                response.put("Id_задачи",retTask.getId());
                response.put("Состояние",TaskState);

                return ResponseEntity.ok(response);
            }
            else{
                // Оба файла пуcты
                Map<Object,Object> response = new HashMap<>();
                response.put("Описание","Нет исходников и/или данных");

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

        }
        catch (TaskDoesNotExistException e){
            Map<Object,Object> response = new HashMap<>();
            response.put("Описание",e.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

    }

    private NewTaskDto getNewTaskDto(String token, String taskName, Long taskId, MultipartFile taskSourcesFile, MultipartFile taskDataFile) throws IOException {
        NewTaskDto newTaskDto = new NewTaskDto();
        newTaskDto.setTask_name(taskName);
        newTaskDto.setId(taskId);
        newTaskDto.setUser_id(getUserByToken(token).getId());
        newTaskDto.setData_file_name(getWholeFilePath(taskDataFile));
        newTaskDto.setSource_file_name(getWholeFilePath(taskSourcesFile));
        return newTaskDto;
    }

    private NewTaskDto getNewTaskDtoName(String token, String taskName, MultipartFile taskSourcesFile, MultipartFile taskDataFile) throws IOException {
        NewTaskDto newTaskDto = new NewTaskDto();
        newTaskDto.setTask_name(taskName);
        newTaskDto.setUser_id(getUserByToken(token).getId());
        newTaskDto.setData_file_name(getWholeFilePath(taskDataFile));
        newTaskDto.setSource_file_name(getWholeFilePath(taskSourcesFile));
        return newTaskDto;
    }


    private static void createFolderIfNotExist(String taskUploadPath) {
        File uploadDir = new File(taskUploadPath);
        if (!uploadDir.exists()){
            uploadDir.mkdir();
        }
    }

    private ResponseEntity getResponseEntityNewTask(String token,
                                                    String TaskName,
                                                    MultipartFile TaskSourcesFile,
                                                    MultipartFile TaskDataFile) throws Exception {
        try{

            createFolderIfNotExist(taskUploadPath);

            if (TaskName==null||TaskName.isEmpty()){
                // Оба файла пуcты
                Map<Object,Object> response = new HashMap<>();
                response.put("Описание","Пустое имя");

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            if(TaskSourcesFile != null && TaskDataFile!=null&&
                    (!TaskSourcesFile.isEmpty()&&!TaskDataFile.isEmpty())){

                NewTaskDto newTaskDto = getNewTaskDtoName(token, TaskName, TaskSourcesFile, TaskDataFile);
                // Добавить в очередь, диспетчер сам просканирует, что выполнять
                newTaskDto.setStatus(TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА);
                Task task = taskService.saveTask(newTaskDto);

                String msg = "Задача "+ TaskName +" загружены на сервер. ";
                Map<Object,Object> response = new HashMap<>();

                response.put("Описание",msg);
                response.put("Id_задачи",task.getId());
                response.put("Состояние",TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА);

                return ResponseEntity.ok(response);

            }
            else if(TaskSourcesFile != null && TaskDataFile==null&&
                    (!TaskSourcesFile.isEmpty())){

                // Новые исходники
                NewTaskDto newTaskDto = getNewTaskDtoName(token, TaskName, TaskSourcesFile, TaskDataFile);
                newTaskDto.setStatus(TaskStatusEnum.ОЖИДАНИЕ_ДАННЫХ);
                Task task = taskService.saveTask(newTaskDto);

                String msg = "Исходники к задаче "+ TaskName +" загружены на сервер. ";
                Map<Object,Object> response = new HashMap<>();

                response.put("Описание",msg);
                response.put("Id_задачи",task.getId());
                response.put("Состояние",TaskStatusEnum.ОЖИДАНИЕ_ДАННЫХ);

                return ResponseEntity.ok(response);
            }
            else if (TaskSourcesFile == null && TaskDataFile!=null&&
                    (!TaskDataFile.isEmpty())){

                // Новые данные
                NewTaskDto newTaskDto = getNewTaskDtoName(token, TaskName, TaskSourcesFile, TaskDataFile);
                newTaskDto.setStatus(TaskStatusEnum.ОЖИДАНИЕ_ИСХОДНИКОВ);
                Task task = taskService.saveTask(newTaskDto);

                String msg = "Данные к задаче "+ TaskName +" загружены на сервер. ";
                Map<Object,Object> response = new HashMap<>();

                response.put("Описание",msg);
                response.put("Id_задачи",task.getId());
                response.put("Состояние",TaskStatusEnum.ОЖИДАНИЕ_ИСХОДНИКОВ);

                return ResponseEntity.ok(response);
            }
            else
            {
                // Пустые файлы

                Map<Object,Object> response = new HashMap<>();
                response.put("Описание","Нет исходников и/или данных");

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

        }
        catch (TaskLimitException exc){
            Map<Object,Object> response = new HashMap<>();
            response.put("Описание",exc.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        catch (Exception exception){
            throw new Exception(exception);
        }
    }


    private User getUserByToken(String token) {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider();
        token = token.replace ("Bearer_", "");
        String UserName = jwtTokenProvider.getUsername(token);
        User User1 = userService.findByUsername(UserName);
        return User1;
    }

    private String getWholeFilePath(MultipartFile TaskFile) throws IOException {

        if(TaskFile==null){
            return null;
        }

        String [] fileParts = TaskFile.getOriginalFilename().split("[.]");
        String uniqFileName = UUID.randomUUID().toString();
        String resFileName =  fileParts[0]+"."+ uniqFileName+"."+fileParts[1];
        String sourceFileAllPath = taskUploadPath +"\\"+ resFileName;
        TaskFile.transferTo(new File(sourceFileAllPath));
        return sourceFileAllPath;
    }




}
