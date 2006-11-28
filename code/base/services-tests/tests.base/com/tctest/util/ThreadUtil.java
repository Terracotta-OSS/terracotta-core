/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
