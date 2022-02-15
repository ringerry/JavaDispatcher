package Ru.IVT.JWT_REST_Dispatcher.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "task")
@Data

public class Task {

   @ToString.Exclude
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ToString.Exclude
    @CreatedDate
    @Column(name = "created")
    private Date created;

    @ToString.Exclude
    @LastModifiedDate
    @Column(name = "updated")
    private Date updated;

    @ToString.Exclude
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TaskStatusEnum status;


    @ToString.Exclude
    @Column(name = "name")
    private String name;

   @ToString.Exclude
    @Column(name = "source_file_name")
    private String source_file_name;

    @ToString.Exclude
    @Column(name = "data_file_name")
    private String data_file_name;


    /*  @ManyToOne  не работает с типом Long почему?*/
    // у одного пользователия несолько задач
//    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @Column(name = "User_Id")

    @ToString.Exclude
    private Long user_id;


    @ToString.Exclude
    // одни задача снимается и загружается на выполнение
    @OneToMany(mappedBy = "id")
//    @JsonManagedReference
    @JsonIgnore
    protected List<TaskInRun> task_in_runs;


    @ToString.Exclude
    @Enumerated(EnumType.STRING)
    private InsideTaskStatusEnum inside_status;

    @ToString.Exclude
    private String result_file;

    @ToString.Exclude
    private Integer task_order;



//    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @JoinColumn(name="User_Id")

//    @Column(name = "User_Id")
//    @JsonBackReference
//    private String User_Id;


}