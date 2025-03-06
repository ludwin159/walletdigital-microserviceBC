package com.wallet.walletdigital.exceptions.handler;

import com.wallet.walletdigital.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ExceptionHandlers {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlers.class);

    @ExceptionHandler({
            ResourceNotFoundException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, String>> handleExceptions(RuntimeException exception) {
        HttpStatus status = getStatus(exception);
        return ResponseEntity.status(status).body(
                Map.of("message", exception.getMessage()));
    }

    private HttpStatus getStatus(RuntimeException exception) {
        if (exception instanceof ResourceNotFoundException) {
            log.warn(exception.getMessage());
            return HttpStatus.NOT_FOUND;
        } else if(exception instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        }else {
            log.error(exception.getMessage());
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException exception) {
        Map<String, String> errorsResponse = new HashMap<>();
        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errorsResponse.put(error.getField(), error.getDefaultMessage()));
        log.error(exception.getMessage());
        return errorsResponse;
    }

}
