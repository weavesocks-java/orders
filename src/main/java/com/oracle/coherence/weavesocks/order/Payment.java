package com.oracle.coherence.weavesocks.order;

import java.io.Serializable;

import com.oracle.io.pof.annotation.Portable;
import com.oracle.io.pof.annotation.PortableType;

@PortableType(id = 2)
public class Payment implements Serializable {
    @Portable boolean authorised;
    @Portable String  message;

    public Payment(boolean authorised, String message) {
        this.authorised = authorised;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "authorised=" + authorised +
                ", message=" + message +
                '}';
    }
}
