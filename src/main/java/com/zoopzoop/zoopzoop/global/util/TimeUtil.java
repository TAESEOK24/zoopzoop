package com.zoopzoop.zoopzoop.global.util;

import java.time.LocalDateTime;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
