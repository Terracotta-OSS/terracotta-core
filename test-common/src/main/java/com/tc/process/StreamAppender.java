/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
