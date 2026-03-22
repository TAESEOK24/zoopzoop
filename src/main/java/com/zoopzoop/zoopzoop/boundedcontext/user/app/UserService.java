package com.zoopzoop.zoopzoop.boundedcontext.user.app;

import com.zoopzoop.zoopzoop.boundedcontext.user.entity.User;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public User getSampleUser() {
        return new User(1L, "sample-user");
    }
}
