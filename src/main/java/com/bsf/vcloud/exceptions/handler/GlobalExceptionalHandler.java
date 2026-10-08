package com.bsf.vcloud.exceptions.handler;

import com.bsf.vcloud.exceptions.BusinessRuleException;
import com.bsf.vcloud.exceptions.ResourceNotFoundException;
import com.bsf.vcloud.exceptions.dto.ExceptionDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice

public class GlobalExceptionalHandler {
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ExceptionDTO> handleBusinessRuleException (BusinessRuleException ex, HttpServletRequest request){
        ExceptionDTO exception = new ExceptionDTO(
                HttpStatus.BAD_REQUEST.value(), //se pá mudar
                "Broken Business Rule",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ExceptionDTO> handleNotFound (ResourceNotFoundException ex, HttpServletRequest request){
        ExceptionDTO exception = new ExceptionDTO(
                HttpStatus.NOT_FOUND.value(),
                "Resource not found!",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception);
    }
//    @ExceptionHandler(UnauthorizedActionException.class)
//    public ResponseEntity<ExceptionDTO> handleUnauthorizedAction (UnauthorizedActionException ex, HttpServletRequest request){
//        ExceptionDTO exception = new ExceptionDTO(
//                HttpStatus.UNAUTHORIZED.value(),
//                "Unauthorized",
//                ex.getMessage(),
//                request.getRequestURI(),
//                LocalDateTime.now(),
//                null
//        );
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(exception);
//    }
}
