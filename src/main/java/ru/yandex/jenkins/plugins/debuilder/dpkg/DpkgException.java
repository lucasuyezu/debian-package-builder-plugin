package ru.yandex.jenkins.plugins.debuilder.dpkg;

public class DpkgException extends Exception {
    private static final long serialVersionUID = 451421831733445286L;

    public DpkgException(String message) {
        super(message);
    }

    public DpkgException(String message, Throwable cause) {
        super(message, cause);
    }

}
