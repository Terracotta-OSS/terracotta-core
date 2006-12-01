/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.process;

/**
 * A process starter variant that will use the system classloader to instaniate the desired target main class
 */
public class DSOLinkedJavaProcessStarter {

  public static void main(String args[]) throws Exception {
    LinkedJavaProcessStarter.main(args, true);    
  }
  
}
