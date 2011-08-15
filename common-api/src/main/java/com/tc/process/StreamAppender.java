/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.process;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class StreamAppender {

  private final PrintWriter output;
  private Thread      outWriter;
  private Thread      errWriter;

  public StreamAppender(OutputStream output) {
    this.output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)), true);
  }

  public void writeInput(final InputStream err, final InputStream out) {
    errWriter = new Thread() {
      BufferedReader reader = new BufferedReader(new InputStreamReader(err));

      @Override
      public void run() {
        try {
          String line;
          while ((line = reader.readLine()) != null) {
            output.println(line);
          }
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          output.flush();
        }
      }
    };

    outWriter = new Thread() {
      BufferedReader reader = new BufferedReader(new InputStreamReader(out));

      @Override
      public void run() {
        try {
          String line;
          while ((line = reader.readLine()) != null) {
            output.println(line);
          }
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          output.flush();
        }
      }
    };

    errWriter.setDaemon(true);
    outWriter.setDaemon(true);

    errWriter.start();
    outWriter.start();
  }

  public void finish() throws Exception {
    outWriter.join();
    errWriter.join();
  }

}
