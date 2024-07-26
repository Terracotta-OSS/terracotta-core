/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.terracotta.ipceventbus.event.EventBus;


/**
 * Scans a stream, as it is produced, to produce events for an event bus (note that the events are triggered on the same
 * thread doing the stream processing).
 */
public class MultiplexedEventingStream extends OutputStream {
  private final Map<String, EventManager> serverEventMap = new ConcurrentHashMap<>();
  private final OutputStream nextConsumer;
  private final ByteArrayOutputStream stream;

  private final boolean twoByteLineSeparator;
  private final byte eol;
  private final byte eolLeader;

  private boolean haveLeader = false;

  private final Pattern serverPull = Pattern.compile("<<(.*)>>(.*)");

  public MultiplexedEventingStream(OutputStream nextConsumer) {
    Assert.assertNotNull(nextConsumer);
    this.nextConsumer = nextConsumer;
    this.stream = new ByteArrayOutputStream();

    String lineSeparator = System.lineSeparator();
    this.twoByteLineSeparator = lineSeparator.length() == 2;
    this.eol = (byte)lineSeparator.charAt(lineSeparator.length() - 1);
    this.eolLeader = (byte)(this.twoByteLineSeparator ? lineSeparator.charAt(0) : '\0');
  }

  public void addEventMapForServer(String server, EventBus bus, Map<String, String> events) {
    serverEventMap.put(server, new EventManager(events, bus));
  }

  @Override
  public void write(int b) throws IOException {
    if (twoByteLineSeparator && eolLeader == (byte)b) {
      haveLeader = true;
    } else {
      boolean flush = false;
      if (eol == (byte)b) {
        // End of line so we process the bytes to find matches and then replace it.
        // NOTE:  This will use the platform's default encoding.
        String oneLine = this.stream.toString();
        Matcher matcher = serverPull.matcher(oneLine);
        if (matcher.find()) {
          serverEventMap.getOrDefault(matcher.group(1), new EventManager(Collections.emptyMap(), null)).handleMessage(matcher.group(2));
        }
        flush = true;
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
      if (flush) {
        this.nextConsumer.flush();
      }
    }
  }

  @Override
  public void close() throws IOException {
    this.nextConsumer.close();
  }

  private static class EventManager {
    private final Map<String, String> events;
    private final EventBus bus;

    public EventManager(Map<String, String> events, EventBus bus) {
      this.events = events;
      this.bus = bus;
    }

    public void handleMessage(String msg) {
      // Determine what events to trigger by scraping the string for keys.
      for (Map.Entry<String, String> pair : events.entrySet()) {
        if (-1 != msg.indexOf(pair.getKey())) {
          this.bus.trigger(pair.getValue(), msg);
        }
      }
    }
  }
}
