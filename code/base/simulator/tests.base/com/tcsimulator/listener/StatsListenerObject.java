/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator.listener;

import com.tc.simulator.listener.StatsListener;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

public final class StatsListenerObject implements StatsListener {
  private static final String TOKEN = "<app-perf>";
  private final LinkedQueue   outputQueue;

  private final String        label;

  public StatsListenerObject(Properties properties, LinkedQueue outputQueue) {
    StringBuffer buf = new StringBuffer();
    for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Entry) i.next();
      buf.append(entry.getKey() + "=" + entry.getValue());
      if (i.hasNext()) {
        buf.append(",");
      }
    }
    this.label = buf.toString();
    this.outputQueue = outputQueue;
  }

  public void sample(long sampleValue, String desc) {
    if(desc == null) desc = "";
    try {
      outputQueue.put(TOKEN + label + desc + ": " + sampleValue);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}