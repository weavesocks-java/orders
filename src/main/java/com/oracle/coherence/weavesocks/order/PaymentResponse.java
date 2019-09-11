package com.oracle.coherence.weavesocks.order;

import com.oracle.io.pof.annotation.Portable;
import com.oracle.io.pof.annotation.PortableType;

@PortableType(id = 2)
public class PaymentResponse {
    @Portable private boolean authorised = false;
    @Portable private String  message;

    public PaymentResponse() {
    }

    public PaymentResponse(boolean authorised, String message) {
        this.authorised = authorised;
        this.message = message;
    }

    @Override
    public String toString() {
        return "PaymentResponse{" +
                "authorised=" + authorised +
                ", message=" + message +
                '}';
    }

    public boolean isAuthorised() {
        return authorised;
    }

    public void setAuthorised(boolean authorised) {
        this.authorised = authorised;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
