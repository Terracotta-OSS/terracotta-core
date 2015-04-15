/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
