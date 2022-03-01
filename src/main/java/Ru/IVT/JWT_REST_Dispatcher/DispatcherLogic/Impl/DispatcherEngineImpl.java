package Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.Impl;


import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.DispathcerEnginge;
import Ru.IVT.JWT_REST_Dispatcher.Model.InsideTaskStatusEnum;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskService;
import Ru.IVT.JWT_REST_Dispatcher.Tools.BashTools;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Диспетчеризация, алгоритмы приоритетов
 * @author Меньшиков Артём
 *
 * Соглашения: UUID - по папке исходников
 *
 * */
/*
Все дейстия в task_in_run делает этот DispatcherEngine класс
 */

@Slf4j
@Component
public class DispatcherEngineImpl implements DispathcerEnginge {

    private TaskService taskService;

    private Long dispatcherQuantumPeriodMS;
    private Integer quantumsAtRaundRobin;
    private Integer curQuantumAtRaundRobin;
    private LinkedList<Task> roundRobinTaskQueue;
    private HashMap<Long,LinkedList<Task>> UsersQueues;
    private Task roundRobinCurrenTask;

    private final Timer myTimer; // Создаем таймер

    @Value("${local.paths.save.taskSources}")
    private String taskUploadPath;

//    @Value("${local.paths.dockerTmpDir}")
    private String dockerDirPath;

    @Autowired
    public DispatcherEngineImpl(TaskService taskService){
        this.dispatcherQuantumPeriodMS = 10000L;
        this.quantumsAtRaundRobin = 7;
        this.curQuantumAtRaundRobin = 0;
        this.roundRobinTaskQueue = new LinkedList<>();
        this.UsersQueues = new HashMap<>();
        this.myTimer = new Timer();
        startMainTimer();

        this.taskService = taskService;

        // Отображение внешнего состояния на внутреннее с разрешением противоречий.

        mappingOutside2InsideTaskState();
        initTaskQueue();

       dockerDirPath = "/home/artem/Dispatcher_files/DockerTmp/";

    }

    private void initTaskQueue() {


        // На случай остановки сервера
        ArrayList<Task> taskQueue = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.ВЫПОЛНЕНИЕ);


        for (Task task:taskQueue){
            try {
                roundRobinTaskQueue.addLast(task);
            }
            catch (Exception e){
                log.warn(e.getMessage());
            }
        }

        taskQueue = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.В_ОЧЕРЕДИ);


        for (Task task:taskQueue){
            try {
                roundRobinTaskQueue.addLast(task);
            }
            catch (Exception e){
                log.warn(e.getMessage());
            }
        }

        roundRobinCurrenTask = null;


    }

    // Отображение внешнего состояния на внутреннее с разрешением противоречий.
    private void mappingOutside2InsideTaskState() {
        /*По умолчанию - не определено, если не определено то отобразить, иначе - главенство внутреннего состояния
        * */

        ArrayList<Task> allTasks = taskService.getAllTasks();

        for (Task task:allTasks){
            try {

                NewTaskDto newTaskDto = new NewTaskDto();
                newTaskDto.setId(task.getId());


                if(task.getStatus()==TaskStatusEnum.ОЖИДАНИЕ_ЗАПУСКА||
                        task.getStatus()==TaskStatusEnum.ОЖИДАНИЕ_ДАННЫХ||
                        task.getStatus()==TaskStatusEnum.ОЖИДАНИЕ_ИСХОДНИКОВ)
                {
                    newTaskDto.setInside_status(InsideTaskStatusEnum.НЕ_ОПРЕДЕЛЕНО);
                    taskService.updateInsideTaskStatus(newTaskDto);
                }

                if(task.getStatus() == TaskStatusEnum.В_ОЧЕРЕДИ){
                    newTaskDto.setInside_status(InsideTaskStatusEnum.В_ОЧЕРЕДИ);
                    taskService.updateInsideTaskStatus(newTaskDto);
                }

                if(task.getStatus() == TaskStatusEnum.УДАЛЕНА){
                    newTaskDto.setInside_status(InsideTaskStatusEnum.УДАЛЕНА);
                    taskService.updateInsideTaskStatus(newTaskDto);
                }
            }
            catch (Exception e){
                log.error(e.getMessage());
            }
        }


    }

    private void mappingInside2OutsideTaskState() {
        /*По умолчанию - не определено, если не определено то отобразить, иначе - главенство внутреннего состояния
         * */

        ArrayList<Task> allTasks = taskService.getAllTasks();

        for (Task task:allTasks){
            try {

                NewTaskDto newTaskDto = new NewTaskDto();
                newTaskDto.setId(task.getId());

                if(task.getInside_status()==InsideTaskStatusEnum.ВЫПОЛНЕНИЕ||
                        task.getInside_status()==InsideTaskStatusEnum.ПРИОСТАНОВЛЕНА||
                        task.getInside_status()==InsideTaskStatusEnum.СОЗДАН_ОБРАЗ)

                {
                    newTaskDto.setStatus(TaskStatusEnum.ВЫПОЛНЕНИЕ);
                    taskService.updateTaskStatusByTaskId(newTaskDto);
                }

                if(task.getInside_status()==InsideTaskStatusEnum.ЗАВЕРШЕНА){
                    newTaskDto.setStatus(TaskStatusEnum.ЗАВЕРШЕНА);
                    taskService.updateTaskStatusByTaskId(newTaskDto);
                }

                if(task.getInside_status()==InsideTaskStatusEnum.УДАЛЕНА){
                    newTaskDto.setStatus(TaskStatusEnum.УДАЛЕНА);
                    taskService.updateTaskStatusByTaskId(newTaskDto);
                }

                if(task.getInside_status()==InsideTaskStatusEnum.ОШИБКА_ВЫПОЛНЕНИЯ){
                    newTaskDto.setStatus(TaskStatusEnum.ОШИБКА_ВЫПОЛНЕНИЯ);
                    taskService.updateTaskStatusByTaskId(newTaskDto);
                }

                if(task.getInside_status()==InsideTaskStatusEnum.ОШИБКА_КОМПИЛЯЦИИ){
                    newTaskDto.setStatus(TaskStatusEnum.ОШИБКА_КОМПИЛЯЦИИ);
                    taskService.updateTaskStatusByTaskId(newTaskDto);
                }

            }
            catch (Exception e){
                log.error(e.getMessage());
            }
        }

    }

    public void setTaskService(TaskService taskService){
        this.taskService = taskService;
    }


    private Long counter = Long.valueOf(0);

    private void startMainTimer() {
        myTimer.schedule(new TimerTask() { // Определяем задачу
            @Override
            public void run() {
               log.info("Квант диспетчера");
//               taskService.setTest((counter=counter+1).toString());
                try {
//                    dispatcherQuantum();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
        }, 10000L, dispatcherQuantumPeriodMS);
    }


    private boolean isFolderExist(String folderPath){

        Path path = Paths.get(folderPath);

        return Files.exists(path);
    }

    private String getTaskUnZipDir(Long taskId) throws Exception {

        String str = taskService.getTaskById(taskId).getSource_file_name().replace(".zip","");

         return taskService.getTaskById(taskId).getSource_file_name().replace(".zip","");
    }

    private void checkAndPrepareFolders(Long taskId) throws Exception {
        Task task = taskService.getTaskById(taskId);

        String sourcesPath = task.getSource_file_name();
        String dataPath = task.getData_file_name();

        String dir2UpZip = getTaskUnZipDir(taskId);

        if(!isFolderExist(dir2UpZip)){

            File file = new File(dir2UpZip);
            file.mkdir();


            file = new File(dir2UpZip+"/Выход");
            file.mkdir();

            unzip(sourcesPath,dir2UpZip);
            unzip(dataPath,dir2UpZip/*+"/Input"*/);

            int a = 1;
        }




    }

    private String getUUIDFromFileName(String fileName){

        fileName = fileName.replace(".zip","");

        Pattern pattern = Pattern.compile("\\..*$");
        Matcher matcher = pattern.matcher(fileName);
        String str = null;
        if(matcher.find()){
            str =  fileName.substring(matcher.start(), matcher.end());
        }

//        assert str != null;
        str = str.replace(".","");

        return str;
    }

    private void unzip(final String zipFilePath, final String unzipLocation) throws IOException {
        Process proc = Runtime.getRuntime().exec("unzip "+ zipFilePath+" -d "+unzipLocation);
    }


    // Создаётся автоматически, при заверешении задачи
    private void zip(final String zipFilePath, final String folderPath) throws IOException {
        Process proc = Runtime.getRuntime().exec("zip -r "+ zipFilePath+" "+folderPath);
    }


    private void dockerCreateImage(String dir2UpZip, Long taskId) throws Exception {


        String taskSourceFile = taskService.getTaskById(taskId).getSource_file_name();


        try(FileWriter writer = new FileWriter(dir2UpZip+"/Dockerfile", false))
        {
            writer.write("FROM python\n");
            writer.write("WORKDIR /code\n");
            writer.write("COPY . .\n");
            writer.write("CMD [\"python3\",\"Main.py\"]\n");

            writer.flush();
        }
        catch (Exception e ){log.error(e.getMessage());}


        String dockerCommand = "echo 'q' | sudo -S docker build -t "+
                getUUIDFromFileName(taskSourceFile)+" "+dir2UpZip;

        try{
            ArrayList<String> bashRes = BashTools.bashCommand(dockerCommand,dir2UpZip);
        }
        catch (Exception e){
            throw e;
        }

        int a  = 1;

    }





    private boolean isDockerImageExist(Long taskId) throws Exception {
        Task task = taskService.getTaskById(taskId);

        ArrayList<String> commandResult = BashTools.bashCommand("echo 'q'|sudo -S docker inspect --format='{{json .Config}}' $INSTANCE_ID "+
                getUUIDFromFileName(task.getSource_file_name()),"");

        if(!commandResult.get(0).equals("")){
            return true;
        }

        return false;

    }

    private boolean isTaskRun(Long taskId) throws Exception {

        Task task = taskService.getTaskById(taskId);

        ArrayList<String> commandResult = BashTools.bashCommand("echo 'q'|sudo -S docker inspect --format='{{json .Config}}' $INSTANCE_ID "+
                getUUIDFromFileName(task.getSource_file_name()),"");

        return false;
    }


    private void runTask(Long taskId) throws Exception {
        /*
        * Запустить в докере
        * или снять с паузы
        * */

        Task task = taskService.getTaskById(taskId);

        ArrayList<String> commandResult = BashTools.bashCommand("echo 'q'|sudo -S docker run "+
                getUUIDFromFileName(task.getSource_file_name()),"");




    }

    private void checkTaskCompleteOrHaveTheErrors() {
        /* Удаление из очереди, если завершена или ошибки
        изменение внутреннего состояния
        если завершена добавить в бд путь к выходному файлу
        */
    }

    private void pauseTask(Long taskId) {
    }


    // Основная логика диспетчера
    private void dispatcherQuantum() throws Exception {



        // Переделать по нормальному: как каждые 10 секунд не доставать все задачи?


        ArrayList<Task> taskQueue = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.В_ОЧЕРЕДИ);

//        if(taskQueue.size()!=0){
//
//            isDockerImageExist(14L);
//        }


        for (Task task:taskQueue){
            try {
                checkAndPrepareFolders(task.getId());
            }
            catch (Exception e){
                log.warn(e.getMessage());
            }
        }

        for (Task task:taskQueue) {
            try {

                if(isFolderExist(getTaskUnZipDir(task.getId()))){
                    dockerCreateImage(getTaskUnZipDir(task.getId()),task.getId());
                    roundRobinTaskQueue.addLast(task);
                    //
                }

            }catch (Exception e){
                log.warn(e.getMessage());
            }
        }

        checkTaskCompleteOrHaveTheErrors();

        // Первый запуск или обошли круг
        if(this.curQuantumAtRaundRobin ==0&&roundRobinTaskQueue.size()!=0){
            //Сердце диспетчера!

            Task firstTask = roundRobinTaskQueue.getFirst();

//            roundRobinTaskQueue.addLast(firstTask);

            if(isDockerImageExist(firstTask.getId())){
                if(!isTaskRun(firstTask.getId())){
                    if(roundRobinCurrenTask==null){
                        roundRobinCurrenTask=firstTask;
                    }
                    else{
                        pauseTask(roundRobinCurrenTask.getId());
                        roundRobinTaskQueue.addLast(roundRobinCurrenTask);
                        roundRobinCurrenTask = firstTask;
                    }
                    roundRobinTaskQueue.removeFirst();
                    runTask(firstTask.getId());
                }
                else {
                    log.error("Задача уже запущена");
                }

            }
            else{
                log.error("Образа задачи не существует");
            }

        }

        this.curQuantumAtRaundRobin = this.curQuantumAtRaundRobin % this.quantumsAtRaundRobin;



        if (taskQueue.size()!=0){
            log.info("Задача {} в очереди",taskQueue.get(0).getName());
        }

        mappingInside2OutsideTaskState();
        mappingOutside2InsideTaskState();


    }




    @Override
    public Timer getMyTimer() {
        return null;
    }

    @Override
    public void init() {

    }

    @Override
    public String getConsoleOutput(Long taskId) throws Exception {

        Task task = taskService.getTaskById(taskId);

        ArrayList<String> commandResult = BashTools.bashCommand("echo 'q'|sudo -S docker logs "+
                getUUIDFromFileName(task.getSource_file_name()),"");


        return null;
    }

    @Override
    public boolean delTask(Long taskId) {

        try{
            BashTools.bashCommand("echo 'q'|sudo -S docker rmi ","");
            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
