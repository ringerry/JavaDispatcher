package Ru.IVT.JWT_REST_Dispatcher.REST;


import Ru.IVT.JWT_REST_Dispatcher.DTO.RegisterRequestDto;
import Ru.IVT.JWT_REST_Dispatcher.DTO.UserDto;
import Ru.IVT.JWT_REST_Dispatcher.Model.User;
import Ru.IVT.JWT_REST_Dispatcher.Security.Jwt.JwtTokenProvider;
import Ru.IVT.JWT_REST_Dispatcher.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/auth/")
public class RegistrationController {

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider jwtTokenProvider;

    private final UserService userService;

    @Autowired
    public RegistrationController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @PostMapping("registration")
    public ResponseEntity registerUserAccount(@RequestBody RegisterRequestDto requestDto) throws Exception {

        try {

            UserDto userDto = new UserDto();
            userDto.setUsername(requestDto.getUsername());
            userDto.setFirstName(requestDto.getFirstName());
            userDto.setLastName(requestDto.getLastName());
            userDto.setPassword(requestDto.getPassword());
            userDto.setEmail(requestDto.getEmail());

            User registered = userService.registerNewUserAccount(userDto);

            String username = requestDto.getUsername();
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, requestDto.getPassword()));
            User user = userService.findByUsername(username);

            String token = jwtTokenProvider.createToken(username, user.getRoles());

            Map<Object, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("token", token);

            return ResponseEntity.ok(response);

        } catch (Exception uaeEx) {
            throw new Exception("Не удалось зарегистрировать");
//            mav.addObject("message", "An account for that username/email already exists.");
//            return mav;
        }

//        return new ModelAndView("successRegister", "user", userDto);
    }



}
