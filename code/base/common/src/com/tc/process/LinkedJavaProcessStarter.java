/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.process;

import java.lang.reflect.Method;

/**
 * Runs another main class, with full arguments, but first establishes a socket heartbeat protocol with a parent process
 * on a specified port &mdash; and kills itself if this ping protocol is broken. This prevents runaway Java processes.
 */
public class LinkedJavaProcessStarter {

  public static void main(String args[], boolean useSystemClassLoader) throws Exception {
    int pingPort = Integer.parseInt(args[0]);
    String childClass = args[1];

    String[] realArgs = new String[args.length - 2];
    if (realArgs.length > 0) System.arraycopy(args, 2, realArgs, 0, realArgs.length);

    HeartBeatService.registerForHeartBeat(pingPort, childClass);

    final Class mainClass;
    if (useSystemClassLoader) {
      mainClass = ClassLoader.getSystemClassLoader().loadClass(childClass);
    } else {
      mainClass = Class.forName(childClass);
    }

    Method mainMethod = mainClass.getMethod("main", new Class[] { String[].class });
    mainMethod.invoke(null, new Object[] { realArgs });

  }

  public static void main(String[] args) throws Exception {
    main(args, false);
  }

}