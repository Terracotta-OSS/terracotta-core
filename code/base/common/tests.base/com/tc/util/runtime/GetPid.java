/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.runtime;

class GetPid {

  private GetPid() {
    // nothing here
  }

  public static void main(String args[]) {
    System.out.println("PID is " + getPID());
  }

  public static int getPID() {
    if (libOK) { return new GetPid().getPid(); }
    throw new RuntimeException("The JNI library did not load correctly, the stack was printed to stderr earlier");
  }

  private native int getPid();

  private static final boolean libOK;

  static {
    boolean ok = false;
    try {
      System.loadLibrary("GetPid");
      ok = true;
    } catch (Throwable t) {
      t.printStackTrace(System.err);
      System.err.println("\n***************************\njava.library.path is ["
                         + System.getProperty("java.library.path") + "]\n***************************\n");
      System.err.flush();
    }

    libOK = ok;
  }

}
