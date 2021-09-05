package org.janssen.scoreboard.service.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Type;

/**
 *
 * @author Stephan Janssen
 */
public class ResponseUtil {

    public static ResponseEntity<?> conflict(final String msg) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(msg);
    }

    public static ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    public static ResponseEntity<?> unauthorized(final String msg) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(msg);
    }


    public static ResponseEntity<?> badRequest() {
        return ResponseEntity.badRequest().build();
    }

    public static ResponseEntity<Object> badRequest(final String msg) {
        return ResponseEntity.badRequest().body(msg);
    }

    public static ResponseEntity<?> gone() {
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    public static ResponseEntity<?> created(final Object entity) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entity);
    }

    public static ResponseEntity<Object> ok() {
        return ResponseEntity.ok().build();
    }

    public static ResponseEntity<?> ok(final String infoMessage, boolean info) {
        if (info) {
            return ResponseEntity.ok().body(infoMessage);
        } else {
            return ResponseEntity.ok().build();
        }
    }

    public static ResponseEntity ok(final Object entity) {
        return ResponseEntity.ok().body(entity);
    }

    public static ResponseEntity<?> ok(final Object entity, final Type type) {
        return ResponseEntity.ok(entity);
    }
}

