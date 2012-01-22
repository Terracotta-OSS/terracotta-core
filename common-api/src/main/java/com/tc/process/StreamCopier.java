/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * A simple thread that copies one stream to another. Useful for copying a process's output/error streams to this
 * process's output/error streams.
 */
public class StreamCopier extends Thread {

  protected final OutputStream out;
  private final BufferedReader reader;

  private final String         identifier;

  public StreamCopier(InputStream stream, OutputStream out) {
    this(stream, out, null);
  }

  public StreamCopier(InputStream stream, OutputStream out, String identifier) {
    if ((stream == null) || (out == null)) { throw new AssertionError("null streams not allowed"); }

    reader = new BufferedReader(new InputStreamReader(stream));
    this.out = out;

    this.identifier = identifier;

    setName("Stream Copier");
    setDaemon(true);
  }

  public void run() {
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        if (identifier != null) {
          line = identifier + line;
        }
        line += System.getProperty("line.separator", "\n");
        out.write(line.getBytes());
        out.flush();
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

}
