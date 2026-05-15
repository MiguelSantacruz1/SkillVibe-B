package com.skillvibe.tutoring.exception;

/**
 * Lanzada cuando las credenciales de autenticación son inválidas.
 * El GlobalExceptionHandler la mapea a HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
