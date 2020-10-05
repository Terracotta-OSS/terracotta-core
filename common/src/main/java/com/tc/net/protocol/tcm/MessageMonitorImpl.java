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
package com.tc.net.protocol.tcm;

import org.slf4j.Logger;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.text.StringFormatter;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

public class MessageMonitorImpl implements MessageMonitor {

  private final Map<TCMessageType, MessageCounter> counters     = new TreeMap<TCMessageType, MessageCounter>(
                                                                                                             new TCMessageTypeComparator());
  private final StringFormatter                    formatter    = new StringFormatter();
  private final Timer                              timer;
  private int                                      maxTypeWidth = 0;

  public static MessageMonitor createMonitor(TCProperties tcProps, Logger logger) {
    final MessageMonitor mm;
    if (tcProps.getBoolean(TCPropertiesConsts.TCM_MONITOR_ENABLED, false)) {
      mm = new MessageMonitorImpl();
      ((MessageMonitorImpl) mm).startLogging(logger, tcProps.getInt(TCPropertiesConsts.TCM_MONITOR_DELAY));
    } else {
      mm = new NullMessageMonitor();
    }

    return mm;
  }

  public MessageMonitorImpl() {
    this.timer = new Timer("MessageMonitor logger", true);
  }

  public void startLogging(final Logger logger, int intervalSeconds) {
    if (intervalSeconds < 1) { throw new IllegalArgumentException("invalid interval: " + intervalSeconds); }
    TimerTask task = new TimerTask() {
      @Override
      public void run() {
        logger.info(MessageMonitorImpl.this.toString());
      }
    };

    this.timer.scheduleAtFixedRate(task, 0, intervalSeconds * 1000);
  }

  @Override
  public void newIncomingMessage(TCMessage message) {
    getOrCreateMessageCounter(message.getMessageType()).newIncomingMessage(message);
  }

  @Override
  public void newOutgoingMessage(TCMessage message) {
    getOrCreateMessageCounter(message.getMessageType()).newOutgoingMessage(message);
  }

  private MessageCounter getOrCreateMessageCounter(TCMessageType type) {
    synchronized (counters) {
      MessageCounter rv = counters.get(type);
      if (rv == null) {
        maxTypeWidth = Math.max(maxTypeWidth, type.getTypeName().length());
        rv = new MessageCounter(formatter, type.getTypeName());
        counters.put(type, rv);
      }
      return rv;
    }
  }

  public Map<TCMessageType, MessageCounter> getCounters() {
    return counters;
  }

  @Override
  public String toString() {
    String nl = System.getProperty("line.separator");
    StringBuffer rv = new StringBuffer(nl);

    synchronized (counters) {
      for (MessageCounter counter : counters.values()) {
        rv.append(counter.toString(maxTypeWidth)).append(nl);
      }
    }

    return rv.toString();
  }

  public static class MessageCounter {
    private final AtomicLong      incomingCount = new AtomicLong(0);
    private final AtomicLong      incomingData  = new AtomicLong(0);

    private final AtomicLong      outgoingCount = new AtomicLong(0);
    private final AtomicLong      outgoingData  = new AtomicLong(0);
    private final StringFormatter formatter;
    private final String          name;

    private MessageCounter(StringFormatter formatter, String name) {
      this.formatter = formatter;
      this.name = name;
    }

    private synchronized void newIncomingMessage(TCMessage message) {
      incomingCount.incrementAndGet();
      incomingData.addAndGet(message.getTotalLength());
    }

    private synchronized void newOutgoingMessage(TCMessage message) {
      outgoingCount.incrementAndGet();
      outgoingData.addAndGet(message.getTotalLength());
    }

    public String toString(int nameWidth) {
      return formatter.rightPad(nameWidth, name) + " | IN: " + formatter.leftPad(15, incomingCount) + ", "
             + formatter.leftPad(30, incomingData) + " bytes " + "| OUT: " + formatter.leftPad(15, outgoingCount)
             + ", " + formatter.leftPad(30, outgoingData) + " bytes";

    }

    public AtomicLong getIncomingCount() {
      return incomingCount;
    }

    public AtomicLong getIncomingData() {
      return incomingData;
    }

    public AtomicLong getOutgoingCount() {
      return outgoingCount;
    }

    public AtomicLong getOutgoingData() {
      return outgoingData;
    }

    public String getName() {
      return name;
    }
  }

  private static class TCMessageTypeComparator implements Comparator<TCMessageType>, Serializable {

    @Override
    public int compare(TCMessageType t1, TCMessageType t2) {
      return t1.getTypeName().compareTo(t2.getTypeName());
    }

  }

}
