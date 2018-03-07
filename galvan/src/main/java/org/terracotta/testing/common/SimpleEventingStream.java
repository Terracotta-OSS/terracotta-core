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
package org.terracotta.testing.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.terracotta.ipceventbus.event.EventBus;


/**
 * Scans a stream, as it is produced, to produce events for an event bus (note that the events are triggered on the same
 * thread doing the stream processing).
 */
public class SimpleEventingStream extends OutputStream {
  private final EventBus outputBus;
  private final Map<String, String> eventMap;
  private final OutputStream nextConsumer;
  private final ByteArrayOutputStream stream;

  private final boolean twoByteLineSeparator;
  private final byte eol;
  private final byte eolLeader;

  private boolean haveLeader = false;

  public SimpleEventingStream(EventBus outputBus, Map<String, String> eventMap, OutputStream nextConsumer) {
    Assert.assertNotNull(nextConsumer);
    this.outputBus = outputBus;
    this.eventMap = eventMap;
    this.nextConsumer = nextConsumer;
    this.stream = new ByteArrayOutputStream();

    String lineSeparator = System.lineSeparator();
    this.twoByteLineSeparator = lineSeparator.length() == 2;
    this.eol = (byte)lineSeparator.charAt(lineSeparator.length() - 1);
    this.eolLeader = (byte)(this.twoByteLineSeparator ? lineSeparator.charAt(0) : '\0');
  }

  @Override
  public void write(int b) throws IOException {
    if (twoByteLineSeparator && eolLeader == (byte)b) {
      haveLeader = true;
    } else {
      if (eol == (byte)b) {
        // End of line so we process the bytes to find matches and then replace it.
        // NOTE:  This will use the platform's default encoding.
        String oneLine = this.stream.toString();
        // Determine what events to trigger by scraping the string for keys.
        for (Map.Entry<String, String> pair : this.eventMap.entrySet()) {
          if (-1 != oneLine.indexOf(pair.getKey())) {
            this.outputBus.trigger(pair.getValue(), oneLine);
          }
        }
        // Start the next line.
        this.stream.reset();
      } else {
        if (haveLeader) {
          this.stream.write(eolLeader);
        }
        this.stream.write(b);
      }
      haveLeader = false;
      this.nextConsumer.write(b);
    }
  }

  @Override
  public void close() throws IOException {
    this.nextConsumer.close();
  }
}
