package com.yeoro.twogether.global.util;

import java.util.regex.Pattern;

public class PasswordValidator {

    private static final String REGEX =
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}\\[\\]|:;\"'<>,.?/]).{8,}$";

    private static final Pattern PATTERN = Pattern.compile(REGEX);

    public static boolean isValid(String password) {
        return password != null && PATTERN.matcher(password).matches();
    }
}