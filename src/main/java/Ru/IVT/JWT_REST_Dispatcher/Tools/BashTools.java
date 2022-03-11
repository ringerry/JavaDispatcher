package Ru.IVT.JWT_REST_Dispatcher.Tools;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Set;

@Slf4j
public class BashTools {

    private static String tmpFileDir = "/home/artem/tmp_dir/";

    private static boolean isFolderExist(String folderPath){

        Path path = Paths.get(folderPath);

        return Files.exists(path);
    }

    public static String getTmpFileDir(){return tmpFileDir;}

    public static ArrayList<String> bashCommand(String command, String dir) throws IOException, InterruptedException {


        try{


            if(!isFolderExist(tmpFileDir)){
                File file = new File(tmpFileDir);
                file.mkdir();
            }

            Path tmpBashPath = Paths.get(tmpFileDir+"tmpRun.bash");
            Path tmpCommandResult= Paths.get(tmpFileDir+"CommandResult.txt");

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

//            log.info("Успешный запуск!");

            return (ArrayList<String>) Files.readAllLines(tmpCommandResult);


        }
        catch (Exception e){
            log.error(e.getMessage());

            throw e;

        }
    }

}
