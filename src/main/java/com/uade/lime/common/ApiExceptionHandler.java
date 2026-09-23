package com.uade.lime.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.uade.lime.common.exception.ArgumentInvalidException;
import com.uade.lime.common.exception.ConflictoException;
import com.uade.lime.common.exception.NoAutorizadoException;
import com.uade.lime.common.exception.ProhibidoException;
import com.uade.lime.common.exception.RecursoNoEncontradoException;

import jakarta.validation.ConstraintViolationException;


@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid request body");
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request body", detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", exception.getMessage());
    }

    @ExceptionHandler(ArgumentInvalidException.class)
    public ResponseEntity<Object> handleArgumentInvalid(ArgumentInvalidException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid argument", exception.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Object> handleRecursoNoEncontrado(RecursoNoEncontradoException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), exception.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Object> handleConflicto(ConflictoException exception) {
        return buildResponse(HttpStatus.CONFLICT, HttpStatus.CONFLICT.getReasonPhrase(), exception.getMessage());
    }

    @ExceptionHandler(NoAutorizadoException.class)
    public ResponseEntity<Object> handleNoAutorizado(NoAutorizadoException exception) {
        return buildResponse(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.getReasonPhrase(), exception.getMessage());
    }

    @ExceptionHandler(ProhibidoException.class)
    public ResponseEntity<Object> handleProhibido(ProhibidoException exception) {
        return buildResponse(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.getReasonPhrase(), exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El archivo supera el tamano maximo permitido");
    }

    private ResponseEntity<Object> buildResponse(HttpStatus status, String title, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("title", title);
        body.put("detail", detail);
        return ResponseEntity.status(status).body(body);
    }
}
