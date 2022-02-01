package Ru.IVT.JWT_REST_Dispatcher.Model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "task")
@Data

public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @CreatedDate
    @Column(name = "created")
    private Date created;

    @LastModifiedDate
    @Column(name = "updated")
    private Date updated;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TaskStatusEnum status;



    @Column(name = "name")
    private String name;

    @Column(name = "source_file_name")
    private String source_file_name;

    @Column(name = "data_file_name")
    private String data_file_name;


    /*  @ManyToOne  не работает с типом Long почему?*/
    // у одного пользователия несолько задач
//    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @Column(name = "User_Id")
    private Long user_id;

    // одни задача снимается и загружается на выполнение
    @OneToMany(mappedBy = "id")
    @JsonManagedReference
    protected List<TaskInRun> task_in_runs;


//    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @JoinColumn(name="User_Id")

//    @Column(name = "User_Id")
//    @JsonBackReference
//    private String User_Id;


}