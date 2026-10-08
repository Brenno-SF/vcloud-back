package com.bsf.vcloud.exceptions.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ExceptionDTO(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        List<FieldErrorDTO> fieldErrors
) {}
