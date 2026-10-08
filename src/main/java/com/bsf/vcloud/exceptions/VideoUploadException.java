package com.bsf.vcloud.exceptions;

public class VideoUploadException extends RuntimeException {
    public VideoUploadException(String message, Throwable e) {
        super(message, e);
    }
    public VideoUploadException(String message) {
        super(message);
    }
}
