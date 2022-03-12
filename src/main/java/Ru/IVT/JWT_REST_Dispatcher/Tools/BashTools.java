package Ru.IVT.JWT_REST_Dispatcher.Tools;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class BashTools {

    private static ReentrantLock mutex = new ReentrantLock();

    private static String tmpFileDir = "/home/artem/tmp_dir/";

    private static boolean isFolderExist(String folderPath){

        Path path = Paths.get(folderPath);

        return Files.exists(path);
    }

    public static String getTmpFileDir(){return tmpFileDir;}


    public static ArrayList<String> cmdCommand(String command) throws IOException {

        try {
//            mutex.lock();




            Process proc = Runtime.getRuntime().exec(command);

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

// Read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                log.info(s);
            }

// Read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                log.info(s);
            }
//
//            BufferedReader stdInput = new BufferedReader(new
//                    OutputStream (proc.getOutputStream()));
//
            ArrayList<String> commandResult = new ArrayList<>();
//
//            String s = null;
//            while ((s = stdInput.readLine()) != null) {
//                commandResult.add(s);
//            }
//
            return commandResult;
        }
        catch (Exception e){
            e.printStackTrace();
            throw e;
        }
//        finally {
////            mutex.unlock();
//        }


    }

    public static ArrayList<String> bashCommand(String command, String dir) throws IOException, InterruptedException {



        try{

            mutex.lock();


            if(!isFolderExist(tmpFileDir)){
                File file = new File(tmpFileDir);
                file.mkdir();
            }

            Path tmpBashPath = Paths.get(tmpFileDir+"tmpRun.bash");
            Path tmpCommandResult= Paths.get(tmpFileDir+"CommandResult.txt");

            FileWriter writer = new FileWriter(tmpBashPath.toString(), false);
            writer.write("#!/bin/bash"+"\n\n");
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

//            Writer w = new OutputStreamWriter(process.getOutputStream(), "UTF-8");
//            String str = w.toString();

//            log.info("Успешный запуск!");

            return (ArrayList<String>) Files.readAllLines(tmpCommandResult);


        }
        catch (Exception e){
            log.error(e.getMessage());

            throw e;

        }
        finally {
            mutex.unlock();
        }
    }

}
