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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import static com.tc.object.StatType.CLIENT_RECEIVED;
import static com.tc.object.StatType.CLIENT_COMPLETE;
import static com.tc.object.StatType.CLIENT_RETIRED;
import static com.tc.object.StatType.SERVER_ADD;
import static com.tc.object.StatType.SERVER_BEGININVOKE;
import static com.tc.object.StatType.SERVER_ENDINVOKE;
import static com.tc.object.StatType.SERVER_SCHEDULE;

/**
 *
 */
class InFlightStats implements PrettyPrintable {
  
  private final List<Combo> values = new ArrayList<>();
  private final LongAdder totalCount = new LongAdder();

  public InFlightStats() {
    values.add(new Combo(CLIENT_ENCODE, CLIENT_SEND));
    values.add(new Combo(CLIENT_SEND, CLIENT_SENT));
    values.add(new Combo(CLIENT_SENT, CLIENT_RECEIVED));
    values.add(new Combo(CLIENT_RECEIVED, CLIENT_COMPLETE));
    values.add(new Combo(CLIENT_COMPLETE, CLIENT_GOT));
    values.add(new Combo(CLIENT_GOT, CLIENT_DECODED));
    values.add(new Combo(CLIENT_COMPLETE, CLIENT_RETIRED));
    values.add(new Combo(CLIENT_SENT, CLIENT_RETIRED));
    values.add(new Combo(CLIENT_ENCODE, CLIENT_DECODED));
    values.add(new Combo(SERVER_ADD, SERVER_SCHEDULE));
    values.add(new Combo(SERVER_SCHEDULE, SERVER_BEGININVOKE));
    values.add(new Combo(SERVER_BEGININVOKE, SERVER_ENDINVOKE));
  }
  
  public void collect(long[] input) {
    values.forEach(c->c.add(input));
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
  
  private static class Combo {
    private final StatType from;
    private final StatType to;
    private final LongAdder value = new LongAdder();

    public Combo(StatType from, StatType to) {
      this.from = from;
      this.to = to;
    }
    
    private void add(long[] vals) {
      value.add(vals[to.ordinal()] - vals[from.ordinal()]);
    }
    
    private long value() {
      return value.sum();
    }

    @Override
    public String toString() {
      return from.description() + "->" + to.description();
    }
  }
}
