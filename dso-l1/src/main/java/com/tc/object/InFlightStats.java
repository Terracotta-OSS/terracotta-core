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

import static com.tc.object.InFlightStats.Type.CLIENT_DECODED;
import static com.tc.object.InFlightStats.Type.CLIENT_ENCODE;
import static com.tc.object.InFlightStats.Type.CLIENT_GOT;
import static com.tc.object.InFlightStats.Type.CLIENT_SEND;
import static com.tc.object.InFlightStats.Type.CLIENT_SENT;
import static com.tc.object.InFlightStats.Type.SERVER_COMPLETE;
import static com.tc.object.InFlightStats.Type.SERVER_RECEIVED;
import static com.tc.object.InFlightStats.Type.SERVER_RETIRED;
import com.tc.text.PrettyPrintable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 */
class InFlightStats implements PrettyPrintable {
  enum Type {
    CLIENT_ENCODE("CLIENT:encode"),
    CLIENT_SEND("CLIENT:send"),
    CLIENT_SENT("CLIENT:sent"),
    SERVER_RECEIVED("SERVER:received"),
    SERVER_COMPLETE("SERVER:complete"),
    CLIENT_GOT("CLIENT:retrieved"),
    CLIENT_DECODED("CLIENT:decoded"),
    SERVER_RETIRED("SERVER:retired");
    private final String description;
    
    Type(String description) {
      this.description = description;
    }
    
    public String description() {
      return this.description;
    }
  }
  
  private final List<Combo> values = new ArrayList<>();
  private final LongAdder totalCount = new LongAdder();

  public InFlightStats() {
    values.add(new Combo(CLIENT_ENCODE, CLIENT_SEND));
    values.add(new Combo(CLIENT_SEND, CLIENT_SENT));
    values.add(new Combo(CLIENT_SENT, SERVER_RECEIVED));
    values.add(new Combo(SERVER_RECEIVED, SERVER_COMPLETE));
    values.add(new Combo(SERVER_COMPLETE, CLIENT_GOT));
    values.add(new Combo(CLIENT_GOT, CLIENT_DECODED));
    values.add(new Combo(SERVER_COMPLETE, SERVER_RETIRED));
    values.add(new Combo(CLIENT_SENT, SERVER_RETIRED));
    values.add(new Combo(CLIENT_ENCODE, CLIENT_DECODED));
  }
  
  public void collect(long[] input) {
    values.forEach(c->c.add(input));
    totalCount.increment();
  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    values.forEach(c->map.put(c.toString(), c.value()/totalCount.sum()));
    return map;
  } 
  
  private static class Combo {
    private final Type from;
    private final Type to;
    private final LongAdder value = new LongAdder();

    public Combo(Type from, Type to) {
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
