/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.process;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * A simple program for the {@link LinkedJavaProcessTest}that simply echoes a single line of input.
 */
public class LinkedJavaProcessTestMain2 {

  public static void main(String[] args) throws Exception {
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    String line = reader.readLine();
    System.out.println("DATA: out: <" + line + ">");
    System.err.println("DATA: err: <" + line + ">");
  }

}
