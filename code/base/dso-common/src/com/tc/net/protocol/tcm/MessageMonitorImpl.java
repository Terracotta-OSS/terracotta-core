/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.text.StringFormatter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MessageMonitorImpl implements MessageMonitor {
  
  private final Map counters = new HashMap();
  private final StringFormatter formatter = new StringFormatter();
  
  public void newIncomingMessage(TCMessage message) {
    getOrCreateMessageCounter(message.getMessageType()).newIncomingMessage(message);
  }

  public void newOutgoingMessage(TCMessage message) {
    getOrCreateMessageCounter(message.getMessageType()).newOutgoingMessage(message);
  }
  
  private MessageCounter getOrCreateMessageCounter(Object key) {
    synchronized (counters) {
      MessageCounter rv = (MessageCounter) counters.get(key);
      if (rv == null) {
        rv = new MessageCounter(formatter, key.toString());
        counters.put(key, rv);
      }
      return rv;
    }
  }
  
  public String toString() {
    StringBuffer rv = new StringBuffer();
    String nl = System.getProperty("line.separator");
    rv.append("Message monitor").append(nl);
    synchronized (counters) {
      for (Iterator i=counters.values().iterator(); i.hasNext();) {
        rv.append(i.next()).append(nl);
      }
    }
    rv.append(nl);
    return rv.toString();
  }
  
  private static class MessageCounter {

    private final String           name;
    private final SynchronizedLong incomingCount = new SynchronizedLong(0);
    private final SynchronizedLong incomingData  = new SynchronizedLong(0);

    private final SynchronizedLong outgoingCount = new SynchronizedLong(0);
    private final SynchronizedLong outgoingData  = new SynchronizedLong(0);
    private final StringFormatter formatter;

    private MessageCounter(StringFormatter formatter, String name) {
      this.formatter = formatter;
      this.name = formatter.rightPad(25, name);
    }

    private synchronized void newIncomingMessage(TCMessage message) {
      incomingCount.increment();
      incomingData.add(message.getTotalLength());
    }

    private synchronized void newOutgoingMessage(TCMessage message) {
      outgoingCount.increment();
      outgoingData.add(message.getTotalLength());
    }
    
    public String toString() {
      return name + "| IN: " + formatter.leftPad(30, incomingCount) + ", " + formatter.leftPad(30, incomingData) + "b"
             + "| OUT: " + formatter.leftPad(30, outgoingCount) + ", " + formatter.leftPad(30, outgoingData) + "b";

    }
  }
}
