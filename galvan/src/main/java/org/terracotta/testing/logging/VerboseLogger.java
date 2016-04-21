/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.logging;

import java.io.PrintStream;


/**
 * Meant to provide a very simple mechanism for logging verbose data, throughout the system.
 * If the instance is initialized with a null output PrintStream, it will assume that it is to run in "silent" mode and will
 * log nothing to output.
 */
public class VerboseLogger {
  private final PrintStream output;
  private final PrintStream error;
  
  public VerboseLogger(PrintStream output, PrintStream error) {
    this.output = output;
    this.error = error;
  }

  public void output(String message) {
    if (null != this.output) {
      this.output.println(message);
    }
  }

  public void error(String message) {
    if (null != this.error) {
      this.error.println(message);
    }
  }
}
