package Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.Impl;


import Ru.IVT.JWT_REST_Dispatcher.DTO.NewTaskDto;
import Ru.IVT.JWT_REST_Dispatcher.Model.InsideTaskStatusEnum;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskService;
import Ru.IVT.JWT_REST_Dispatcher.Tools.BashTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

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
 * */
/*
Все дейстия в task_in_run делает этот DispatcherEngine класс
 */

@Slf4j
public class DispatcherEngineImpl /*implements DispathcerEnginge*/ {

    private TaskService taskService;

    private Long dispatcherQuantumPeriodMS;
    private Integer quantumsAtRaundRobin;
    private Integer curQuantumAtRaundRobin;
    private LinkedList<Task> roundRobinTaskQueue;

    private final Timer myTimer; // Создаем таймер

    @Value("${local.paths.save.taskSources}")
    private String taskUploadPath;

//    @Value("${local.paths.dockerTmpDir}")
    private String dockerDirPath;

    public DispatcherEngineImpl(){
        this.dispatcherQuantumPeriodMS = 10000L;
        this.quantumsAtRaundRobin = 7;
        this.curQuantumAtRaundRobin = 0;
        this.myTimer = new Timer();
        startMainTimer();

        // Отображение внешнего состояния на внутреннее с разрешением противоречий.

        mappingOutside2InsideTaskState();
        initTaskQueue();


       dockerDirPath = "/home/artem/Dispatcher_files/DockerTmp/";

    }

    private void initTaskQueue() {


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



    }

    // Отображение внешнего состояния на внутреннее с разрешением противоречий.
    private void mappingOutside2InsideTaskState() {
        /*По умолчанию - не определено, если не определено то отобразить, иначе - главенство внутреннего состояния
        * */

        ArrayList<Task> allTasks = taskService.getAllTasks();

        for (Task task:allTasks){
            try {

                NewTaskDto newTaskDto = new NewTaskDto();


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


    private void startMainTimer() {
        myTimer.schedule(new TimerTask() { // Определяем задачу
            @Override
            public void run() {
               log.info("Квант диспетчера");
                try {
                    dispatcherQuantum();
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


        return true;

    }

    private boolean isTaskRun(Long id) {
        return false;
    }


    private void runTask(Long id) {
        /*
        * Запустить в докере
        *
        * */
    }

    private void checkTaskCompleteOrHaveTheErrors() {
        /* Удаление из очереди, если завершена или ошибки
        изменение внутреннего состояния
        если завершена добавить в бд путь к выходному файлу
        */
    }

    // Основная логика диспетчера
    private void dispatcherQuantum() throws Exception {


       // Переделать по нормальному: как каждые 10 секунд не доставать все задачи?


        ArrayList<Task> taskQueue = taskService.getTasksByInsideStatus(InsideTaskStatusEnum.В_ОЧЕРЕДИ);

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


        // Первый запуск или обошли круг
        if(this.curQuantumAtRaundRobin ==0){
            //Сердце диспетчера!

            Task firstTask = roundRobinTaskQueue.getFirst();
            roundRobinTaskQueue.removeFirst();
            roundRobinTaskQueue.addLast(firstTask);

            if(isDockerImageExist(firstTask.getId())){
                if(!isTaskRun(firstTask.getId())){
                    runTask(firstTask.getId());
                }

            }
            else{
                log.error("Образа задачи не существует");
            }

        }

        this.curQuantumAtRaundRobin = this.curQuantumAtRaundRobin % this.quantumsAtRaundRobin;

        checkTaskCompleteOrHaveTheErrors();

        if (taskQueue.size()!=0){
            log.info("Задача {} в очереди",taskQueue.get(0).getName());
        }

        mappingInside2OutsideTaskState();


    }




}
