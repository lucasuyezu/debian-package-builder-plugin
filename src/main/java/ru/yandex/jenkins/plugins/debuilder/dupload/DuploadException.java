package ru.yandex.jenkins.plugins.debuilder.dupload;

public class DuploadException extends Exception {
    private static final long serialVersionUID = 451421831733445286L;

    public DuploadException(String message) {
        super(message);
    }

    public DuploadException(String message, Throwable cause) {
        super(message, cause);
    }

}
