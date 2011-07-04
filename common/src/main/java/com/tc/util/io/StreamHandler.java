/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.io;

import com.tc.exception.TCRuntimeException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamHandler implements Runnable {
  InputStream  in;
  OutputStream out;

  public StreamHandler(InputStream in, OutputStream out) {
    this.in = in;
    this.out = out;
  }

  public void run() {
    BufferedInputStream bin = new BufferedInputStream(in);
    BufferedOutputStream bout = new BufferedOutputStream(out);
    int i;
    try {
      while ((i = bin.read()) != -1) {
        bout.write(i);
      }
      bout.flush();
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }
}