/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.process;

/**
 * A simple child process for {@link LinkedJavaProcessTest} that prints something simple and exits.
 */
public class LinkedJavaProcessTestMain1 {

  public static void main(String[] args) {
    System.out.println("DATA: Hi there!");
    System.err.println("DATA: Ho there!");
  }

}
