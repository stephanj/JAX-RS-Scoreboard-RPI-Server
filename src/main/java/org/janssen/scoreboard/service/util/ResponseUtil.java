package org.janssen.scoreboard.service.util;

import org.janssen.scoreboard.model.Token;

import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.lang.reflect.Type;
import java.net.URI;

/**
 *
 * @author Stephan Janssen
 */
public class ResponseUtil {


    public static Response conflict(final String msg) {
        return Response.status(Response.Status.CONFLICT).entity(msg).build();
    }

    public static Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    public static Response unauthorized(final String msg) {
        return Response.status(Response.Status.UNAUTHORIZED).entity(msg).build();
    }


    public static Response badRequest() {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    public static Response badRequest(final String msg) {
        return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
    }

    public static Response gone() {
        return Response.status(Response.Status.GONE).build();
    }

    public static Response created(final Object entity) {
        return Response.status(Response.Status.CREATED).entity(entity).build();
    }

    public static Response ok() {
        return Response.status(Response.Status.OK).build();
    }

    public static Response ok(final String infoMessage, boolean info) {
        if (info) {
            return Response.status(Response.Status.OK).entity(infoMessage).build();
        } else {
            return Response.status(Response.Status.OK).build();
        }
    }

    public static Response ok(final Object entity) {
        return Response.ok(entity).build();
    }

    public static Response ok(final Object entity, final Type type) {
        return Response.ok(entity).build();
    }
}

