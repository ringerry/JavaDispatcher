package Ru.IVT.JWT_REST_Dispatcher.Model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;


/**
 * Simple domain object that represents application user.
 *
 * @author Eugene Suleimanov
 * @version 1.0
 */

@Entity
@Table(name = "users")
@Data
public class User extends BaseEntity {

    @ToString.Exclude
    @Column(name = "username")
    private String username;

    @ToString.Exclude
    @Column(name = "first_name")
    private String firstName;

    @ToString.Exclude
    @Column(name = "last_name")
    private String lastName;

    @ToString.Exclude
    @Column(name = "email")
    private String email;

    @ToString.Exclude
    @Column(name = "password")
    private String password;


    @ToString.Exclude
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "role_id", referencedColumnName = "id")})
    private List<Role> roles;

    @ToString.Exclude
    @OneToMany(mappedBy = "name")
    @JsonManagedReference
    protected List<Task> user_tasks;

}
