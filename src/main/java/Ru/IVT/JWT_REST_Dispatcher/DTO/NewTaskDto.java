package Ru.IVT.JWT_REST_Dispatcher.DTO;

import lombok.Data;
import   Ru.IVT.JWT_REST_Dispatcher.Model.TaskStatusEnum;

@Data
public class NewTaskDto {
    private Long user_id;
    private Long id;
    private String task_name;
    private TaskStatusEnum status;
    private String source_file_name;
    private String data_file_name;

}
