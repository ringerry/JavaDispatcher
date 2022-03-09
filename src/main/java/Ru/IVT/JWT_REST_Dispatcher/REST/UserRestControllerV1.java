package Ru.IVT.JWT_REST_Dispatcher.REST;

import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.DTO.UserDto;
import Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.DispathcerEnginge;
import Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.Impl.DispatcherEngineImpl;
import Ru.IVT.JWT_REST_Dispatcher.JwtDispatcherApplication;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.User;
import Ru.IVT.JWT_REST_Dispatcher.Security.Jwt.JwtTokenProvider;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskDoesNotExistException;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskLimitException;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskService;
import Ru.IVT.JWT_REST_Dispatcher.Service.UserService;
import Ru.IVT.JWT_REST_Dispatcher.Tools.BashTools;
import liquibase.util.file.FilenameUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;




/**
 * REST controller user connected requestst.
 *
 * @author Eugene Suleimanov
 * @author Меньшиков Артём
 * @version 1.0
 */

// TODO еще одну сущноть пользователия для отправки
@Slf4j
@RestController
@RequestMapping(value = "/api/")
public class UserRestControllerV1 {

    private final UserService userService;
    private final TaskService taskService;
    private DispathcerEnginge dispathcerEnginge;

    @Value("${local.paths.save.taskSources}")
    private String taskUploadPath;

    @Autowired
    public UserRestControllerV1(UserService userService, TaskService taskService,
                                DispathcerEnginge dispathcerEnginge) {
        this.userService = userService;
        this.taskService = taskService;

        this.dispathcerEnginge = dispathcerEnginge;
    }

    @GetMapping(value = "hello")
    public ResponseEntity<String> responseHello(){

        return new ResponseEntity<>("Здравствуйте!"+taskService.getTest(), HttpStatus.OK);
    }

    private Object getHeadersInfo(){

        HashMap<String, Object> method_headers = new HashMap<>(), meth_param = new HashMap<>();

        meth_param.put("Тип","Строка");
        meth_param.put("Обязательный","Истина");
        meth_param.put("Описание","Токен");
        meth_param.put("Шаблон","Bearer_<токен>");
        method_headers.put("Authorization",meth_param.clone());
        return method_headers;
    }

    private Object getParamDescription(String type, boolean required, String description){
        HashMap<String, Object>  meth_param = new HashMap<>();

        meth_param.put("Тип",type);
        meth_param.put("Обязательный",required ? "Истина":"Ложь");
        meth_param.put("Описание",description);
        return meth_param;
    }

    private Object getUserURLInfo(String type,Object headers, Object body,String outputType, String description){
        HashMap<String, Object> method_info= new HashMap<>();

        method_info.put("Заголовки",headers);
        method_info.put("Описание",description);
        method_info.put("Тип",type);
        method_info.put("Параметры тела",body==null?"Не требуются":body);

        method_info.put("Тип ответа" ,outputType);

        return method_info;
    }

    private Object getUserURLInfo(String type,Object headers, Object body, String description){
        HashMap<String, Object> method_info= new HashMap<>();

        method_info.put("Заголовки",headers);
        method_info.put("Описание",description);
        method_info.put("Тип",type);
        method_info.put("Параметры тела",body==null?"Не требуются":body);
        return method_info;
    }

    @GetMapping(value = "documentation")
    public ResponseEntity
    documentation() throws Exception {

        try{

            String msg = "Данное удаленное приложение поддерживает контейнерную обработку задач на Python и " +
                    "TypeScript. Исходники и данные могут поступать в обработку независимо.";
            Map<Object,Object> response = new HashMap<>();

            response.put("Описание сервиса",msg);

            msg="Приложение на Python должно содержать точку входа - файл 'Main.py'; приложение на TypeScript " +
                    "должно содержать точку входа - файл 'Main.ts', регистр важен. " +
                    "Формат исходников и данных - zip архив." +
                    "Данное приложение для каждой запущенной задачи распаковывает zip архив с исходниками в определённую " +
                    "папку, в корне этой папки как раз и должен находится файл Main.py(Main.ts); а также в эту же папку," +
                    " т.е. в одну папку с Main.py(Main.ts) распаковываются данные. Таким образом, перед запуском " +
                    "пользовательской задачи в некоторой папке находится распакованный zip архив с исходниками, " +
                    "содержащий Main.py(Main.ts) и входные данные. Выходные файлы программы должны помещаться в папку " +
                    "./Выход. По успешному завершению задачи пользователь получает zip архив папки ./Выход. " +
                    "Аргументы программы должны быть записаны в файл Аргументы.txt, каждый аргумент с новой строки." +
                    "Файл Аргументы.txt помещается в исходную папку. Имена файлов, папок, аргументов командной строки " +
                    "не должны содержать пробелов.";
            response.put("Требования",msg);

            msg="Доступные методы";


            HashMap<String, Object> methods = new HashMap<>(), method_params=new HashMap<>();
                     ;


            methods.put("/api/documentation",getUserURLInfo("GET",getHeadersInfo(),null,"JSON" ,
                    "Справка."));



            methods.put("/api/task_list",getUserURLInfo("GET",getHeadersInfo(),null,"JSON",
                    "Список всех задач."));


            method_params = new HashMap<>();
            method_params.put("TaskName",getParamDescription(
                    "Строка",
                    true,
                    "Имя задачи"
            ));
            method_params.put("TaskId",getParamDescription(
                    "Целое",
                    true,
                    "Id задачи"
            ));
            methods.put("/api/task_output",getUserURLInfo("GET",getHeadersInfo(),method_params,
                    "Zip-архив",
                    "Выходные файлы задачи. Zip-архив."));




            method_params.put("TaskName",getParamDescription(
                    "Строка",
                    true,
                    "Имя задачи"
            ));
            method_params.put("CanNameDuplicate",getParamDescription(
                    "Логический",
                    true,
                    "Истина - имена задач могут повторятся, ложь - имя задачи должно быть уникальным."
            ));
            method_params.put("TaskSourcesFile",getParamDescription(
                    "Zip-архив",
                    true,
                    "Zip-архив с исходниками на Python или TypeScript, корень архива содерижт точку " +
                            "входа Main.py(Main.ts)."
            ));
            method_params.put("TaskDataFile",getParamDescription(
                    "Zip-архив",
                    true,
                    "Zip-архив с данными для задачи"
            ));
            methods.put("/api/add_task",getUserURLInfo("POST",getHeadersInfo(),method_params,
                    "Добавление задачи"));



            method_params = new HashMap<>();
            method_params.put("TaskName",getParamDescription(
                    "Строка",
                    true,
                    "Имя задачи"
            ));
            method_params.put("TaskId",getParamDescription(
                    "Целое",
                    false,
                    "Если не указывать id задачи - создастся новая, иначе, если указать её id, " +
                            "то к существующей задаче добавятся(обновятся) исходники."
            ));
            method_params.put("CanNameDuplicate",getParamDescription(
                    "Логический",
                    true,
                    "Истина - имена задач могут повторятся, ложь - имя задачи должно быть уникальным."
            ));
            method_params.put("TaskSourcesFile",getParamDescription(
                    "Zip-архив",
                    true,
                    "Zip-архив с исходниками на Python или TypeScript, корень архива содерижт точку " +
                            "входа Main.py(Main.ts)."
            ));
            methods.put("/api/add_sources",getUserURLInfo("POST",getHeadersInfo(),method_params,
                    "Добавление или обновление исходников"));




            method_params = new HashMap<>();
            method_params.put("TaskName",getParamDescription(
                    "Строка",
                    true,
                    "Имя задачи"
            ));
            method_params.put("TaskId",getParamDescription(
                    "Целое",
                    false,
                    "Если не указывать id задачи - создастся новая, иначе, если указать её id, " +
                            "то к существующей задаче добавятся(обновятся) исходники."
            ));
            method_params.put("CanNameDuplicate",getParamDescription(
                    "Логический",
                    true,
                    "Истина - имена задач могут повторятся, ложь - имя задачи должно быть уникальным."
            ));
            method_params.put("TaskDataFile",getParamDescription(
                    "Zip-архив",
                    true,
                    "Zip-архив с данными для задачи"
            ));
            methods.put("/api/add_data",getUserURLInfo("POST",getHeadersInfo(),method_params,
                    "Добавление или обновление данных к задаче"));



            method_params = new HashMap<>();
            method_params.put("TaskName",getParamDescription(
                    "Строка",
                    true,
                    "Имя задачи"
            ));
            method_params.put("TaskId",getParamDescription(
                    "Целое",
                    true,
                    "Id задачи"
            ));
            methods.put("/api/task_status",getUserURLInfo("POST",getHeadersInfo(),method_params,
                    "Получение статуса задачи."));



            method_params = new HashMap<>();
            method_params.put("TaskName",getParamDescription(
                    "Строка",
                    true,
                    "Имя задачи"
            ));
            method_params.put("TaskId",getParamDescription(
                    "Целое",
                    true,
                    "Id задачи"
            ));
            methods.put("/api/run_task",getUserURLInfo("POST",getHeadersInfo(),method_params,
                    "Запуск задачи. Пока задача не запущена, она не будет выполнятся, даже если " +
                            "на сервере будут и исходника, и данные."));



            method_params = new HashMap<>();
            method_params.put("TaskName",getParamDescription(
                    "Строка",
                    true,
                    "Имя задачи"
            ));
            method_params.put("TaskId",getParamDescription(
                    "Целое",
                    true,
                    "Id задачи"
            ));
            methods.put("/api/delete_task",getUserURLInfo("POST",getHeadersInfo(),method_params,
                    "Удаление задачи."));



            response.put("Доступные методы",methods);
            return ResponseEntity.status(HttpStatus.OK).body(response);

        }
        catch (Exception exc){
            throw exc;
        }

    }

    @GetMapping(value = "task_list")
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


    @GetMapping(
            value = "task_output",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    @ResponseBody
    public ResponseEntity<Resource> getTaskOutput(@RequestHeader("Authorization") String token,
                                                  @RequestParam(value = "TaskName") String TaskName,
                                                  @RequestParam(value = "TaskId") Long TaskId) throws IOException {
        // Проверка на повторяющиеся задачи
        try{

            String filename = "/home/artem/Dispatcher_files/Данные_задача_1.39f3d1fc-3202-42e4-8671-0d9b0db597ed.zip";

            Path file = Paths.get(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
//                return resource;
            }
            else {
                throw new FileNotFoundException(
                        "Could not read file: " + filename);
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

//            Path path = Paths.get("/home/artem/Dispatcher_files/Данные_задача_1.39f3d1fc-3202-42e4-8671-0d9b0db597ed.zip");


//            File initialFile = new File("/home/artem/Dispatcher_files/Данные_задача_1.39f3d1fc-3202-42e4-8671-0d9b0db597ed.zip");
//            InputStream targetStream = new FileInputStream(initialFile);
//
////            InputStream in = UserRestControllerV1.class.getResourceAsStream("/home/artem/Dispatcher_files/Данные_задача_1.39f3d1fc-3202-42e4-8671-0d9b0db597ed.zip");
//            return IOUtils.toByteArray(targetStream);

//            return ResponseEntity.ok(null);

        }
        catch (Exception exc){
            throw exc;
        }
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

    @PostMapping(value = "delete_task",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity
    deleteTask(@RequestHeader("Authorization") String token,
                  @RequestParam(value = "TaskName") String TaskName,
                  @RequestParam(value = "TaskId") Long TaskId) throws Exception {


        // Проверка на повторяющиеся задачи
        try{

            User User1 = getUserByToken(token);

            if(dispathcerEnginge.delTask(getUserByToken(token).getId(),TaskId)){

                Map<Object,Object> response = new HashMap<>();
                String msg = "Задача удалена";
                response.put("Описание",msg);
                response.put("Id_задачи",TaskId);

                return ResponseEntity.ok(response);
            }
            else{
                Map<Object,Object> response = new HashMap<>();
                String msg = "Задачу невозоможно удалить";
                response.put("Описание",msg);
                response.put("Id_задачи",TaskId);

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
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

    private String getFileExtention(String filename){
        String extension = Optional.of(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1))
                .orElse("");

        return extension.toLowerCase(Locale.ROOT);
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

                if(!isZipFile(taskSourcesFile)){
                    Map<Object,Object> response = new HashMap<>();
                    response.put("Описание","Формат файла: *.zip");

                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }

                if(!isZipFile(taskDataFile)){
                    Map<Object,Object> response = new HashMap<>();
                    response.put("Описание","Формат файла: *.zip");

                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }

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

                if(!isZipFile(taskSourcesFile)){
                    Map<Object,Object> response = new HashMap<>();
                    response.put("Описание","Формат файла: *.zip");

                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }

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


                if(!isZipFile(taskDataFile)){
                    Map<Object,Object> response = new HashMap<>();
                    response.put("Описание","Формат файла: *.zip");

                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }

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

                if(!isZipFile(TaskSourcesFile)){
                    Map<Object,Object> response = new HashMap<>();
                    response.put("Описание","Формат файла: *.zip");

                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }

                if(!isZipFile(TaskDataFile)){
                    Map<Object,Object> response = new HashMap<>();
                    response.put("Описание","Формат файла: *.zip");

                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }

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

                if(!isZipFile(TaskSourcesFile)){
                    Map<Object,Object> response = new HashMap<>();
                    response.put("Описание","Формат файла: *.zip");
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }

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

                if(!isZipFile(TaskDataFile)){
                    Map<Object,Object> response = new HashMap<>();
                    response.put("Описание","Формат файла: *.zip");

                    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                }

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

    private boolean isZipFile(MultipartFile file) throws IOException {

        String file_type = file.getContentType();

        byte [] byteArr=file.getBytes();

        int a = 1;

        if(byteArr[0]==80&&byteArr[1]==75&&byteArr[2]==3&&byteArr[3]==4) {

            if(getFileExtention(file.getOriginalFilename()).equals("zip"))
            return true;
        }
        return false;

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
        String sourceFileAllPath = taskUploadPath +"/"+ resFileName;
        TaskFile.transferTo(new File(sourceFileAllPath));
        return sourceFileAllPath;
    }




}
