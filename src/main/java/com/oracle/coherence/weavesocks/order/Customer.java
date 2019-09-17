package com.oracle.coherence.weavesocks.order;

import java.io.Serializable;

import com.oracle.io.pof.annotation.Portable;
import com.oracle.io.pof.annotation.PortableType;

@PortableType(id = 5)
public class Customer implements Serializable {
    @Portable public String id;
    @Portable public String firstName;
    @Portable public String lastName;

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}
