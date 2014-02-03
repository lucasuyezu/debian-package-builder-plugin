package ru.yandex.jenkins.plugins.debuilder.dpkg;

public class VersionFormatException extends Exception {
    private static final long serialVersionUID = -9165210853108326594L;

    public VersionFormatException(String message) {
        super(message);
    }

    public VersionFormatException(String message, Throwable cause) {
        super(message, cause);
    }

}
