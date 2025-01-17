package com.oracle.coherence.weavesocks.order;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;

@ApplicationScoped
@Provider
public class OrderExceptionMapper
        implements ExceptionMapper<OrderResource.OrderException> {
    @Override
    public Response toResponse(OrderResource.OrderException exception)
    {
        return Response
                .status(NOT_ACCEPTABLE)
                .entity(Collections.singletonMap("message", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}