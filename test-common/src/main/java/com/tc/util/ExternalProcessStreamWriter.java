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
package com.tc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class ExternalProcessStreamWriter {

  private volatile IOException exception;
  
  public void printSys(InputStream in) {
    print(System.out, in);
  }
  
  public void printErr(InputStream in) {
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
      @Override
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
