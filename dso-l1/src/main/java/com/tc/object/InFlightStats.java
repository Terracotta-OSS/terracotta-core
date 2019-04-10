/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object;

import static com.tc.object.StatType.CLIENT_DECODED;
import static com.tc.object.StatType.CLIENT_ENCODE;
import static com.tc.object.StatType.CLIENT_GOT;
import static com.tc.object.StatType.CLIENT_SEND;
import static com.tc.object.StatType.CLIENT_SENT;
import com.tc.text.PrettyPrintable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import static com.tc.object.StatType.CLIENT_RECEIVED;
import static com.tc.object.StatType.CLIENT_COMPLETE;
import static com.tc.object.StatType.CLIENT_RETIRED;
import static com.tc.object.StatType.SERVER_ADD;
import static com.tc.object.StatType.SERVER_BEGININVOKE;
import static com.tc.object.StatType.SERVER_COMPLETE;
import static com.tc.object.StatType.SERVER_ENDINVOKE;
import static com.tc.object.StatType.SERVER_RECEIVED;
import static com.tc.object.StatType.SERVER_RETIRED;
import static com.tc.object.StatType.SERVER_SCHEDULE;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
class InFlightStats implements PrettyPrintable {
  
  private static final List<Combo> values = Arrays.asList(
    new Combo(CLIENT_ENCODE, CLIENT_SEND),
    new Combo(CLIENT_SEND, CLIENT_SENT),
    new Combo(CLIENT_SENT, CLIENT_RECEIVED),
    new Combo(CLIENT_RECEIVED, CLIENT_COMPLETE),
    new Combo(CLIENT_COMPLETE, CLIENT_GOT),
    new Combo(CLIENT_GOT, CLIENT_DECODED),
    new Combo(CLIENT_COMPLETE, CLIENT_RETIRED),
    new Combo(CLIENT_SENT, CLIENT_RETIRED),
    new Combo(CLIENT_ENCODE, CLIENT_DECODED),
    new Combo(SERVER_ADD, SERVER_SCHEDULE),
    new Combo(SERVER_SCHEDULE, SERVER_BEGININVOKE),
    new Combo(SERVER_BEGININVOKE, SERVER_ENDINVOKE),
    new Combo(SERVER_RECEIVED, SERVER_COMPLETE),
    new Combo(SERVER_COMPLETE, SERVER_RETIRED)
  );
  private final LongAdder totalCount = new LongAdder();
  
  public void collect(long[] input) {
    if (input != null) {
      values.forEach(c->c.add(input));
    }
    totalCount.increment();
  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    if (totalCount.sum() > 0) {
      values.forEach(c->map.put(c.toString(), c.value()/totalCount.sum()));
    }
    return map;
  } 
  
  static class Combo {
    Logger LOG = LoggerFactory.getLogger(Combo.class);
    private final StatType from;
    private final StatType to;
    private final LongAdder value = new LongAdder();

    Combo(StatType from, StatType to) {
      this.from = from;
      this.to = to;
    }
    
    Combo add(long[] vals) {
      try {
        value.add(vals[to.ordinal()] - vals[from.ordinal()]);
      } catch (Throwable t) {
        LOG.warn("error collecting stats", t);
      }
      return this;
    }
    
    long value() {
      return value.sum();
    }

    @Override
    public String toString() {
      return from.description() + "->" + to.description();
    }
  }
}
