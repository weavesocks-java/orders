package com.oracle.coherence.weavesocks.order;

import java.io.Serializable;

import com.oracle.io.pof.annotation.Portable;
import com.oracle.io.pof.annotation.PortableType;

@PortableType(id = 4)
public class Card implements Serializable {
    @Portable public String longNum;
    @Portable public String expires;
    @Portable public String ccv;

    @Override
    public String toString() {
        return "Card{" +
                ", longNum='" + longNum + '\'' +
                ", expires='" + expires + '\'' +
                ", ccv='" + ccv + '\'' +
                '}';
    }
}
