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

    public DispatcherEngineImpl(){
        this.dispatcherPeriodMS = 1000L;
        this.myTimer = new Timer();
        startMainTimer();
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

    private void checkAndPrepareFolders(Long taskId) throws Exception {
        Task task = taskService.getTaskById(taskId);

        String sourcesPath = task.getSource_file_name();
        String dataPath = task.getData_file_name();

        String dir2UpZip = sourcesPath.replace(".zip","");

        if(!isFolderExist(dir2UpZip)){


//            Process p = new ProcessBuilder("unzip", sourcesPath+" -d "+dir2UpZip).start();

            Process proc = Runtime.getRuntime().exec("unzip "+ sourcesPath+" -d "+dir2UpZip);
            int a = 1;
//            unzip(sourcesPath,dir2UpZip);
//
//            File destDir = new File(dir2UpZip);
//            byte[] buffer = new byte[1024];
//            ZipInputStream zis = new ZipInputStream(new FileInputStream(sourcesPath));
//            ZipEntry zipEntry = zis.getNextEntry();
//            while (zipEntry != null) {
//                // ...
//
//                File newFile = newFile(destDir, zipEntry);
//                if (zipEntry.isDirectory()) {
//                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
//                        throw new IOException("Не удалось создать папку" + newFile);
//                    }
//                } else {
//                    // fix for Windows-created archives
//                    File parent = newFile.getParentFile();
//                    if (!parent.isDirectory() && !parent.mkdirs()) {
//                        throw new IOException("Не удалось создать папку " + parent);
//                    }
//
//                    // write file content
//                    FileOutputStream fos = new FileOutputStream(newFile);
//                    int len;
//                    while ((len = zis.read(buffer)) > 0) {
//                        fos.write(buffer, 0, len);
//                    }
//                    fos.close();
//                }
//                zipEntry = zis.getNextEntry();
//
//            }
//            zis.closeEntry();
//            zis.close();
        }


    }


    public static void unzip(final String zipFilePath, final String unzipLocation) throws IOException {

        if (!(Files.exists(Paths.get(unzipLocation)))) {
            Files.createDirectories(Paths.get(unzipLocation));
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                Path filePath = Paths.get(unzipLocation, entry.getName());
                if (!entry.isDirectory()) {
                    unzipFiles(zipInputStream, filePath);
                } else {
                    Files.createDirectories(filePath);
                }

                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        }
    }

    public static void unzipFiles(final ZipInputStream zipInputStream, final Path unzipFilePath) throws IOException {

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(unzipFilePath.toAbsolutePath().toString()))) {
            byte[] bytesIn = new byte[1024];
            int read = 0;
            while ((read = zipInputStream.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }

    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Точка входа вне папки назначения: " + zipEntry.getName());
        }

        return destFile;
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



        if (taskQueue.size()!=0){
            log.info("Задача {} в очереди",taskQueue.get(0).getName());
        }


    }

    public void setTaskService(TaskService taskService){
        this.taskService = taskService;
    }


}
