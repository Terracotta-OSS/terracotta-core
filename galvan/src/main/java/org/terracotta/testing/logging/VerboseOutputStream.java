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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class VerboseOutputStream extends OutputStream {
  private final OutputStream nextStream;
  private final ContextualLogger logger;
  private final boolean useError;
  private final ByteArrayOutputStream stream;

  public VerboseOutputStream(OutputStream nextStream, ContextualLogger logger, boolean useError) {
    this.nextStream = nextStream;
    this.logger = logger;
    this.useError = useError;
    this.stream = new ByteArrayOutputStream();
  }

  @Override
  public void write(int b) throws IOException {
    if ('\n' == (byte)b) {
      // At the end of the line, pass this on to the appropriate logger.
      String oneLine = this.stream.toString();
      this.stream.reset();
      if (this.useError) {
        this.logger.error(oneLine);
      } else {
        this.logger.output(oneLine);
      }
    } else {
      this.stream.write(b);
    }
    this.nextStream.write(b);
  }

  @Override
  public void close() throws IOException {
    this.nextStream.close();
  }
}
