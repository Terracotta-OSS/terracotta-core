/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.tcm;

import com.tc.bytes.TCReferenceSupport;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import org.slf4j.Logger;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.text.StringFormatter;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class MessageMonitorImpl implements MessageMonitor {

  private final Map<TCMessageType, MessageCounter> counters     = new TreeMap<TCMessageType, MessageCounter>(
                                                                                                             new TCMessageTypeComparator());
  private final TCConnectionManager                connections;
  private final StringFormatter                    formatter    = new StringFormatter();
  private final ScheduledExecutorService                              timer;
  private final ScheduledFuture<?> currentTask;
  private int                                      maxTypeWidth = 0;

  public static MessageMonitor createMonitor(TCProperties tcProps, Logger logger, ThreadGroup group, TCConnectionManager mgr) {
    final MessageMonitor mm;
    if (tcProps.getBoolean(TCPropertiesConsts.TCM_MONITOR_ENABLED, false)) {
      mm = new MessageMonitorImpl(group, logger, mgr, tcProps.getInt(TCPropertiesConsts.TCM_MONITOR_DELAY));
    } else {
      mm = new NullMessageMonitor();
    }

    return mm;
  }

  public MessageMonitorImpl(ThreadGroup grp, Logger logger, TCConnectionManager mgr, int delay) {
    this.timer = Executors.newSingleThreadScheduledExecutor(r->{
      Thread t = new Thread(grp, r, "MessageMonitor logger");
      t.setDaemon(true);
      return t;
    });
    this.connections = mgr;
    this.currentTask = startLogging(logger, delay);
  }

  private ScheduledFuture<?> startLogging(final Logger logger, int intervalSeconds) {
    if (intervalSeconds < 1) { throw new IllegalArgumentException("invalid interval: " + intervalSeconds); }
    TCReferenceSupport.startMonitoringReferences();
    return this.timer.scheduleAtFixedRate(()->{
      logger.info(MessageMonitorImpl.this.toString());
      int count = TCReferenceSupport.checkReferences();
      if (count > 0) {
        logger.info("found {} memory references that were not closed", count);
      }
      TCConnection[] list = connections.getAllConnections();
      
      for (TCConnection c : list) {
        Map<String, ?> state = c.getState();
        long sent = (Long)state.get("messageBatch");
        long write = (Long)state.get("messageWritten");
        long recv = (Long)state.get("messageRead");
        
        logger.info("\n" + formatter.rightPad(60, state.get("localAddress") + " -> " + state.get("remoteAddress")) + formatter.leftPad(20, "batch:" + sent)  + formatter.leftPad(20, "write:" + write) + formatter.leftPad(20, "read:" + recv));
      }
    }, 0, intervalSeconds, TimeUnit.SECONDS);
  }

  @Override
  public void shutdown() {
    currentTask.cancel(true);
    timer.shutdownNow();
  }

  @Override
  public void newIncomingMessage(TCAction message) {
    getOrCreateMessageCounter(message.getMessageType()).newIncomingMessage(message);
  }

  @Override
  public void newOutgoingMessage(TCAction message) {
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
    private final LongAdder      incomingCount = new LongAdder();
    private final LongAdder      incomingData  = new LongAdder();

    private final LongAdder      outgoingCount = new LongAdder();
    private final LongAdder      outgoingData  = new LongAdder();
    private final StringFormatter formatter;
    private final String          name;

    private MessageCounter(StringFormatter formatter, String name) {
      this.formatter = formatter;
      this.name = name;
    }

    private void newIncomingMessage(TCAction message) {
      incomingCount.increment();
      incomingData.add(message.getMessageLength());
    }

    private void newOutgoingMessage(TCAction message) {
      outgoingCount.increment();
      outgoingData.add(message.getMessageLength());
    }

    public String toString(int nameWidth) {
      return formatter.rightPad(nameWidth, name) + " | IN: " + formatter.leftPad(15, incomingCount) + ", "
             + formatter.leftPad(30, incomingData) + " bytes " + "| OUT: " + formatter.leftPad(15, outgoingCount)
             + ", " + formatter.leftPad(30, outgoingData) + " bytes";

    }

    public long getIncomingCount() {
      return incomingCount.sum();
    }

    public long getIncomingData() {
      return incomingData.sum();
    }

    public long getOutgoingCount() {
      return outgoingCount.sum();
    }

    public long getOutgoingData() {
      return outgoingData.sum();
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
