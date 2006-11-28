/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.process;


/**
 * Simple main class used by {@link LinkedJavaProcessTest} that prints out some environment.
 */
public class LinkedJavaProcessTestMain4 {

  public static void main(String[] args) {
    System.out.println("DATA: ljpt.foo=" + System.getProperty("ljpt.foo"));
    System.out.println("DATA: " + System.getProperty("user.dir"));
    
    System.out.flush();
  }

}
