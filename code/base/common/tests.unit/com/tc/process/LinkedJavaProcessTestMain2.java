/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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