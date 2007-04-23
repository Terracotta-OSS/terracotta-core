/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * A test program for {@link LinkedJavaProcessTest}that, in turn, spawns some of its own children as linked processes.
 * The test then kills this process, and makes sure that the children die, too.
 */
public class LinkedJavaProcessTestMain5 {

  public static void main(String[] args) throws Exception {
    File destFile = new File(args[0]);
    boolean spawnChildren = new Boolean(args[1]).booleanValue();

    if (spawnChildren) {
      LinkedJavaProcess child1 = new LinkedJavaProcess(LinkedJavaProcessTestMain5.class.getName(),
                                                       new String[] { args[0] + "-child-1", "false" });
      LinkedJavaProcess child2 = new LinkedJavaProcess(LinkedJavaProcessTestMain5.class.getName(),
                                                       new String[] { args[0] + "-child-2", "false" });
      child1.start();
      child2.start();
    }
    
    File stdoutFile = new File(destFile + ".stdout.log");
    System.setOut(new PrintStream(new FileOutputStream(stdoutFile)));
    
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < 3 * 60 * 1000) {
      FileOutputStream out = new FileOutputStream(destFile, true);
      out.write("DATA: Just a line of text.\n".getBytes());
      out.flush();
      out.close();
      
      Thread.sleep(100);
    }
  }

}
