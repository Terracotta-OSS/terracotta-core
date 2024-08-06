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

  @Override
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
