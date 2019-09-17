package com.oracle.coherence.weavesocks.order;

import java.io.Serializable;

import com.oracle.io.pof.annotation.Portable;
import com.oracle.io.pof.annotation.PortableType;

@PortableType(id = 3)
public class Address implements Serializable {
    @Portable public String number;
    @Portable public String street;
    @Portable public String city;
    @Portable public String postcode;
    @Portable public String country;

    @Override
    public String toString() {
        return "Address{" +
                ", number='" + number + '\'' +
                ", street='" + street + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", postcode='" + postcode + '\'' +
                '}';
    }
}
