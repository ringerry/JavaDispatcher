package Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.Impl;


import Ru.IVT.JWT_REST_Dispatcher.DispatcherLogic.StreamGobbler;
import Ru.IVT.JWT_REST_Dispatcher.Model.Task;
import Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;
import Ru.IVT.JWT_REST_Dispatcher.Repository.TaskRepositoryNT;
import Ru.IVT.JWT_REST_Dispatcher.Service.TaskService;
import Ru.IVT.JWT_REST_Dispatcher.Tools.BashTools;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.concurrent.Executors;
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

        // Отображение внешнего состояния на внутреннее с разрешением противоречий.

        mappingOutside2InsideTaskState();


       dockerDirPath = "/home/artem/Dispatcher_files/DockerTmp/";

    }

    // Отображение внешнего состояния на внутреннее с разрешением противоречий.
    private void mappingOutside2InsideTaskState() {
        /*По умолчанию - не определено, если не определено то отобразить, иначе - главенство внутреннего состояния
        * */



    }

    private void mappingInside2OutsideTaskState() {
        /*По умолчанию - не определено, если не определено то отобразить, иначе - главенство внутреннего состояния
         * */
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

    private ArrayList<String> bashCommand(String command, String dir) throws IOException, InterruptedException {


        try{



            Path tmpBashPath = Paths.get(dockerDirPath+"tmpRun.bash");
            Path tmpCommandResult= Paths.get(dockerDirPath+"CommandResult.txt");

            FileWriter writer = new FileWriter(tmpBashPath.toString(), false);
            writer.write(command+"\n");
            writer.flush();
            writer.close();

            writer = new FileWriter(tmpCommandResult.toString(), false);
            writer.flush();
            writer.close();

            Set<PosixFilePermission> ownerWritable = PosixFilePermissions.fromString("rwxrwxrwx");
            FileAttribute<?> permissions = PosixFilePermissions.asFileAttribute(ownerWritable);


            Files.setPosixFilePermissions(tmpBashPath,ownerWritable);


            ProcessBuilder pr = new ProcessBuilder();
            pr.command(tmpBashPath.toString());

            pr.redirectOutput(new File(tmpCommandResult.toString()));

            Process process = pr.start();
            process.waitFor();



            log.info("Успешный запуск!");

            return (ArrayList<String>) Files.readAllLines(tmpCommandResult);


        }
        catch (Exception e){
            log.error(e.getMessage());

            throw e;

        }
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


    private void unzip(final String zipFilePath, final String unzipLocation) throws IOException {
        Process proc = Runtime.getRuntime().exec("unzip "+ zipFilePath+" -d "+unzipLocation);
    }


    private boolean isDockerImageExist(Long taskId) throws Exception {
        Task task = taskService.getTaskById(taskId);


        return true;

    }


    // Основная логика диспетчера
    private void dispatcherQuantum() throws Exception {


        ArrayList<Task> taskQueue = taskService.getTasksByStatus(TaskStatusEnum.В_ОЧЕРЕДИ);

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
                }

            }catch (Exception e){
                log.warn(e.getMessage());
            }
        }



        if (taskQueue.size()!=0){
            log.info("Задача {} в очереди",taskQueue.get(0).getName());
        }


        // Функция отображения внутренего состояния задачи на внешнее


    }




}
