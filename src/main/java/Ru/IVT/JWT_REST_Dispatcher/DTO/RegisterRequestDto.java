package Ru.IVT.JWT_REST_Dispatcher.DTO;

import lombok.Data;

@Data
public class RegisterRequestDto {
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
}
