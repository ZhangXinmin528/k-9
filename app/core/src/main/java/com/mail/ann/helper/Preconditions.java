package com.mail.ann.helper;


public class Preconditions {
    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }

        return reference;
    }
}
