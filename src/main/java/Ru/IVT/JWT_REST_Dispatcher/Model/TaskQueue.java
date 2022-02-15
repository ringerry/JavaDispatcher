package Ru.IVT.JWT_REST_Dispatcher.Model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "task_queue")
@Data
public class TaskQueue extends BaseEntity {

    @ToString.Exclude
    @JsonIgnore
    private Long task_id;


    @ToString.Exclude
//    @JsonIgnore
    private Integer order;


}
