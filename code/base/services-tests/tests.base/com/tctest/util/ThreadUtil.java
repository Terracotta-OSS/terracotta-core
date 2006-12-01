/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.util;

public class ThreadUtil
{
    public static void startDaemonThread(Runnable r)
    {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }
}
