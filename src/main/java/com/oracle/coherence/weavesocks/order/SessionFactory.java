package com.oracle.coherence.weavesocks.order;

import com.tangosol.net.Session;

/**
 * SessionFactory is responsible for ...
 *
 * @author hr  2019.09.18
 */
// TODO: hack until coherence-cdi supports injection of Session
public class SessionFactory {

    private static volatile Session session = null;

    public static Session ensureSession() {
        if (session == null) {
            synchronized (SessionFactory.class) {
                if (session == null) {
                    session = Session.create();
                }
            }
        }
        return session;
    }
}
