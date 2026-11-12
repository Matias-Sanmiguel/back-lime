package com.uade.lime.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.uade.lime.common.exception.ArgumentInvalidException;
import com.uade.lime.common.exception.ConflictoException;
import com.uade.lime.common.exception.NoAutorizadoException;
import com.uade.lime.common.exception.ProhibidoException;
import com.uade.lime.common.exception.RecursoNoEncontradoException;

import jakarta.validation.ConstraintViolationException;


// Con spring.mvc.problemdetails.enabled=true, Spring Boot registra su propio
// @ControllerAdvice para las excepciones de MVC (body inválido, JSON ilegible, tipos de
// parámetro). Sin el @Order, ese handler gana y responde con sus textos genéricos en
// inglés ("Invalid request content.") en vez de los mensajes de acá.
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Revisá los datos enviados");
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request body", detail);
    }

    // El path de la violación es "metodo.parametro" (ej. "list.size"); al usuario le
    // sirve solo el nombre del parámetro.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException exception) {
        String detail = exception.getConstraintViolations().stream()
                .map(violation -> lastNode(violation.getPropertyPath().toString()) + ": " + violation.getMessage())
                .sorted()
                .reduce((a, b) -> a + "; " + b)
                .orElse("Revisá los parámetros de la búsqueda");
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleNotReadable(HttpMessageNotReadableException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request body",
                "No pudimos leer los datos enviados: revisá el formato y los valores");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParameter(MissingServletRequestParameterException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters",
                "Falta el parámetro '" + exception.getParameterName() + "'");
    }

    // Ej.: subir una foto sin la parte "file" en el multipart.
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Object> handleMissingPart(MissingServletRequestPartException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request body",
                "Falta el archivo '" + exception.getRequestPartName() + "' en la solicitud");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request parameters",
                "El valor de '" + exception.getName() + "' no es válido: " + exception.getValue());
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
                "El archivo supera el tamaño máximo permitido (5 MB)");
    }

    private static String lastNode(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }

    private ResponseEntity<Object> buildResponse(HttpStatus status, String title, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("title", title);
        body.put("detail", detail);
        return ResponseEntity.status(status).body(body);
    }
}
