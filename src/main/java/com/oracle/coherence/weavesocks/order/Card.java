package com.oracle.coherence.weavesocks.order;

import java.io.Serializable;

import com.oracle.io.pof.annotation.Portable;
import com.oracle.io.pof.annotation.PortableType;

@PortableType(id = 4)
public class Card implements Serializable {

    @Portable private String id;

    @Portable private String longNum;
    @Portable private String expires;
    @Portable private String ccv;

    public Card() {
    }

    public Card(String id, String longNum, String expires, String ccv) {
        this.id = id;
        this.longNum = longNum;
        this.expires = expires;
        this.ccv = ccv;
    }

    public Card(String longNum, String expires, String ccv) {
        this(null, longNum, expires, ccv);
    }

    @Override
    public String toString() {
        return "Card{" +
                "id=" + id +
                ", longNum='" + longNum + '\'' +
                ", expires='" + expires + '\'' +
                ", ccv='" + ccv + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Card card = (Card) o;

        return getId().equals(card.getId());

    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLongNum() {
        return longNum;
    }

    public void setLongNum(String longNum) {
        this.longNum = longNum;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public String getCcv() {
        return ccv;
    }

    public void setCcv(String ccv) {
        this.ccv = ccv;
    }
}
