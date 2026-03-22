package com.zoopzoop.zoopzoop.boundedcontext.user.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {

    @EventListener
    public void handle(String ignoredEvent) {
        // Placeholder listener for the initial project structure.
    }
}
