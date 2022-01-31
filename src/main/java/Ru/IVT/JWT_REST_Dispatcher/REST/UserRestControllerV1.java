package Ru.IVT.JWT_REST_Dispatcher.REST;

import Ru.IVT.JWT_REST_Dispatcher.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * REST controller user connected requestst.
 *
 * @author Eugene Suleimanov
 * @version 1.0
 */

// TODO еще одну сущноть пользователия для отправки
@RestController
@RequestMapping(value = "/api/")
public class UserRestControllerV1 {

    private final UserService userService;

    @Value("${local.paths.save.taskSources}")
    private String taskUploadPath;

    @Autowired
    public UserRestControllerV1(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = "hello")
    public ResponseEntity<String> responseHello(){

        return new ResponseEntity<>("Здравствуйте!", HttpStatus.OK);
    }

    @PostMapping(value = "add_task",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String>
        addTask(@RequestParam("task_name") String TaskName,
                @RequestParam("TaskSourcesFile")MultipartFile TaskSourcesFile) throws Exception {


        try{
            if(TaskSourcesFile != null){
                File uploadDir = new File(taskUploadPath);
                if (!uploadDir.exists()){
                    uploadDir.mkdir();
                }

                String [] fileParts = TaskSourcesFile.getOriginalFilename().split("[.]");

                String uniqFileName = UUID.randomUUID().toString();

                String resFileName =  fileParts[0]+"."+ uniqFileName+"."+fileParts[1];

                TaskSourcesFile.transferTo(new File(taskUploadPath +"\\"+ resFileName));

                return new ResponseEntity<>("Задача "+ TaskName+" добавлена в обработку. " +
                        "Имя файла:"+ TaskSourcesFile.getOriginalFilename()+".",
                        HttpStatus.OK);
            }
            else{
                return new ResponseEntity<>("Нет имени задачи и/или файла",
                        HttpStatus.NO_CONTENT);
            }


        }
        catch (Exception exception){
            throw new Exception(exception);
        }


    }

}
