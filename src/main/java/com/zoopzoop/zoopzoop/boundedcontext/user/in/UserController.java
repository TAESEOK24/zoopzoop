package com.zoopzoop.zoopzoop.boundedcontext.user.in;

import com.zoopzoop.zoopzoop.boundedcontext.user.app.UserService;
import com.zoopzoop.zoopzoop.boundedcontext.user.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/sample")
    public User sample() {
        return userService.getSampleUser();
    }
}
