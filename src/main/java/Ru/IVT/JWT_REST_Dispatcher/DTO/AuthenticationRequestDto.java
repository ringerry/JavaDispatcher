package Ru.IVT.JWT_REST_Dispatcher.DTO;

import lombok.Data;

/**
 * DTO class for authentication (login) request.
 *
 * @author Eugene Suleimanov
 * @version 1.0
 */

@Data
public class AuthenticationRequestDto {
    private String username;
    private String password;
}
