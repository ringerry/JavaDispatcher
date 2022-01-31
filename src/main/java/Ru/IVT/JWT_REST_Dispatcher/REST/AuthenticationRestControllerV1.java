package Ru.IVT.JWT_REST_Dispatcher.REST;

import Ru.IVT.JWT_REST_Dispatcher.DTO.AuthenticationRequestDto;
import Ru.IVT.JWT_REST_Dispatcher.DTO.RegisterRequestDto;
import Ru.IVT.JWT_REST_Dispatcher.DTO.UserDto;
import Ru.IVT.JWT_REST_Dispatcher.Security.Jwt.JwtTokenProvider;
import Ru.IVT.JWT_REST_Dispatcher.Model.User;
import Ru.IVT.JWT_REST_Dispatcher.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for authentication requests (login, logout, register, etc.)
 *
 * @author Eugene Suleimanov
 * @author Меньшиков Артём
 * @version 1.0
 */

@RestController
@RequestMapping(value = "/api/auth/")
public class AuthenticationRestControllerV1 {

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider jwtTokenProvider;

    private final UserService userService;

    @Autowired
    public AuthenticationRestControllerV1(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }


    @GetMapping("login")
    public String greeting(@RequestParam(name="name", required=false, defaultValue="World") String name, Model model) {
        model.addAttribute("name", name);
        return "greeting";
    }


    @PostMapping("login")
    public ResponseEntity login(@RequestBody AuthenticationRequestDto requestDto) {
        try {
            String username = requestDto.getUsername();
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, requestDto.getPassword()));
            User user = userService.findByUsername(username);

            if (user == null) {
                throw new UsernameNotFoundException("User with username: " + username + " not found");
            }

            String token = jwtTokenProvider.createToken(username, user.getRoles());

            Map<Object, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("token", token);

            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }

//    @PostMapping("registration")
//    public ModelAndView registerUserAccount(
//            @ModelAttribute("user") @Valid UserDto userDto,
//            HttpServletRequest request,
//            Errors errors) throws Exception {
//
//        try {
//            User registered = userService.registerNewUserAccount(userDto);
//        } catch (Exception uaeEx) {
//            throw new Exception("Не удалось зарегистрировать");
////            mav.addObject("message", "An account for that username/email already exists.");
////            return mav;
//        }
//
//        return new ModelAndView("successRegister", "user", userDto);
//    }

//    @PostMapping("registration")
//    public ModelAndView registerUserAccount(@RequestBody RegisterRequestDto requestDto) throws Exception {
//
//        UserDto userDto = new UserDto();
//        userDto.setUsername(requestDto.getUsername());
//        userDto.setFirstName(requestDto.getFirstName());
//        userDto.setLastName(requestDto.getLastName());
//        userDto.setPassword(requestDto.getPassword());
//        userDto.setEmail(requestDto.getEmail());
//
//        try {
//
//
//
//            User registered = userService.registerNewUserAccount(userDto);
//        } catch (Exception uaeEx) {
//            throw new Exception("Не удалось зарегистрировать");
////            mav.addObject("message", "An account for that username/email already exists.");
////            return mav;
//        }
//
//        return new ModelAndView("successRegister", "user", userDto);
//    }

}
