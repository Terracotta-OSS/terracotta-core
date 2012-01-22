/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * A small app that does a simple substring search and replace. It takes its input from the stdin and sends the result
 * to stdout. The strings to search and replaced for is passed as a parameter. Sets the exit code to 1 if any error
 * occurs, otherwise the exit code is set to 0.
 */
public final class StringReplace extends Thread {

  private final PrintStream     ps;
  private final DataInputStream dis;
  private final String          search;
  private final String          replace;

  /**
   */
  private StringReplace(PrintStream ps, DataInputStream dis, String search, String replace) {
    this.ps = ps;
    this.dis = dis;
    this.search = search;
    this.replace = replace;
  }

  /**
   */
  @Override
  public void run() {
    if (ps != null && dis != null) {
      try {
        String source = null;
        while ((source = dis.readLine()) != null) {
          ps.println(StringUtil.replaceAll(source, search, replace, false));
          ps.flush();
        }
        ps.close();
      } catch (IOException e) {
        System.err.println(e.getMessage());
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  /**
   */
  @Override
  protected void finalize() {
    try {
      if (ps != null) {
        ps.close();
      }
      if (dis != null) {
        dis.close();
      }
    } catch (IOException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   */
  public static void main(String[] args) {
    String search = args.length >= 1 ? args[0] : "";
    String replace = args.length >= 2 ? args[1] : "";

    try {
      (new StringReplace(System.out, new DataInputStream(System.in), search, replace)).start();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
}
