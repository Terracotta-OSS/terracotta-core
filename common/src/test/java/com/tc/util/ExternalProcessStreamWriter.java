/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class ExternalProcessStreamWriter {

  private volatile IOException exception;
  
  public void printSys(final InputStream in) {
    print(System.out, in);
  }
  
  public void printErr(final InputStream in) {
    print(System.err, in);
  }
  
  public boolean hasException() {
    return (exception != null);
  }
  
  public IOException getException() {
    return exception;
  }
  
  private void print(final PrintStream stream, final InputStream in) {
    Thread writer = new Thread() {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      public void run() {
        try {
          String line;
          while ((line = reader.readLine()) != null) {
            stream.println(line);
          }
        } catch (IOException e) {
          // connection closed
        } finally {
          try {
            reader.close();
          } catch (IOException e) {
            exception = e;
          }
        }
      }
    };
    writer.start();
  }
}
