package Ru.IVT.JWT_REST_Dispatcher.Model;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "task_in_run")
@Data

public class TaskInRun extends BaseEntity {

//    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @Column(name = "User_Id")
    private Long task_id;

}
