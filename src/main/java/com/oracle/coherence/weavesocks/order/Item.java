package com.oracle.coherence.weavesocks.order;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.oracle.io.pof.annotation.Portable;
import com.oracle.io.pof.annotation.PortableType;

@PortableType(id = 1)
public class Item implements Serializable {
    @Portable public String itemId;
    @Portable public int quantity;
    @Portable public float unitPrice;

    @Override
    public String toString() {
        return "Item{" +
                ", itemId='" + itemId + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                '}';
    }
}
