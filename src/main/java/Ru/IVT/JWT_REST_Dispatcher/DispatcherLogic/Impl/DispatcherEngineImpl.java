package Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.Impl;


import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;
import Ru.IVT.JWT_REST_Dispatcher.Repository.TaskRepositoryNT;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    private Long dispatcherPeriodMS;

    private final Timer myTimer; // Создаем таймер

    @Value("${local.paths.save.taskSources}")
    private String taskUploadPath;

//    @Value("${local.paths.dockerTmpDir}")
    private String dockerDirPath;

    public DispatcherEngineImpl(){
        this.dispatcherPeriodMS = 10000L;
        this.myTimer = new Timer();
        startMainTimer();

       dockerDirPath = "/home/artem/Dispatcher_files/DockerTmp/";

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
        }, 10000L, dispatcherPeriodMS);
    }

    private void scanDataBase(){



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
            unzip(dataPath,dir2UpZip+"/Input");

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

    private String bashCommand(String command,String dir) throws IOException, InterruptedException {

        String fileOut = "/Вывод.txt";

        File file = new File(dir+fileOut);

        file.createNewFile();

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        pb.directory(new File(dir));
        pb.redirectOutput(file);
        pb.start();



        return dir+fileOut;

    }

    private void dockerCreateImage(String dir2UpZip, Long taskId) throws Exception {

//        Path path = Paths.get("${local.paths.save.taskSources}"+"/DockerTmp/Dockerfile");
//
//        Files.createFile(path);

//        File dockerDir = new File(dockerDirPath);
//
//        if(!dockerDir.exists()) {dockerDir.mkdir();}

        String taskSourceFile = taskService.getTaskById(taskId).getSource_file_name();


        try(FileWriter writer = new FileWriter(dir2UpZip+"/Dockerfile", false))
        {
            // запись всей строки
//            String text = "Доброе утро!";
            writer.write("FROM python\n");
            writer.write("WORKDIR /code\n");
            writer.write("COPY . .\n");
            writer.write("CMD [\"python3\",\"Main.py\"]\n");

            writer.flush();
        }
        catch (Exception e ){log.error(e.getMessage());}


        String dockerCommand = "echo 'q' | sudo -S docker build -t "+
                getUUIDFromFileName(taskSourceFile)+" "+dir2UpZip;

        String bashRes = bashCommand(dockerCommand,dir2UpZip);

        Process proc = Runtime.getRuntime().exec(dockerCommand);
        proc = Runtime.getRuntime().exec("sudo docker images --format \"{{json . }}\"");

//        proc = Runtime.getRuntime().exec("sudo docker run "+taskSourceFile+dir2UpZip);

//
//        if(isFolderExist(dir2UpZip)){
//
//        }
    }


    public static void unzip(final String zipFilePath, final String unzipLocation) throws IOException {
        Process proc = Runtime.getRuntime().exec("unzip "+ zipFilePath+" -d "+unzipLocation);
    }



    // Основная логика диспетчера
    private void dispatcherQuantum() throws Exception {


        ArrayList<Task> taskQueue = taskService.getTasksByStatus(TaskStatusEnum.В_ОЧЕРЕДИ);

        for (Task task:taskQueue){
            try {
                checkAndPrepareFolders(task.getId());
            }
            catch (Exception e){
                log.error(e.getMessage());
            }
        }

        for (Task task:taskQueue) {
            try {

                if(isFolderExist(getTaskUnZipDir(task.getId()))){
                    dockerCreateImage(getTaskUnZipDir(task.getId()),task.getId());
                }

            }catch (Exception e){
                log.error(e.getMessage());
            }
        }



        if (taskQueue.size()!=0){
            log.info("Задача {} в очереди",taskQueue.get(0).getName());
        }


    }

    public void setTaskService(TaskService taskService){
        this.taskService = taskService;
    }


}
