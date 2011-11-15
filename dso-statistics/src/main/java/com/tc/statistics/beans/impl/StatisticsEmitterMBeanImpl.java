/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.beans.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.statistics.StatisticData;
import com.tc.statistics.beans.StatisticsEmitterMBean;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.StatisticsBufferListener;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.buffer.exceptions.StatisticsBufferException;
import com.tc.statistics.config.DSOStatisticsConfig;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

public class StatisticsEmitterMBeanImpl extends AbstractTerracottaMBean implements StatisticsEmitterMBean,
    StatisticsBufferListener {
  public final static String                  STATISTICS_EMITTER_DATA_TYPE = "tc.statistics.emitter.data";

  public final static MBeanNotificationInfo[] NOTIFICATION_INFO;

  private final static TCLogger               LOGGER                       = TCLogging
                                                                               .getLogger(StatisticsEmitterMBeanImpl.class);

  static {
    final String[] notifTypes = new String[] { STATISTICS_EMITTER_DATA_TYPE };
    final String name = Notification.class.getName();
    final String description = "Each notification sent contains a Terracotta statistics event";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  private final AtomicLong                    sequenceNumber;

  private final DSOStatisticsConfig           config;
  private final StatisticsBuffer              buffer;
  private final Set                           activeSessionIds;

  private Timer                               timer                        = null;
  private SendStatsTask                       task                         = null;

  public StatisticsEmitterMBeanImpl(final DSOStatisticsConfig config, final StatisticsBuffer buffer)
      throws NotCompliantMBeanException {
    super(StatisticsEmitterMBean.class, true, false);
    Assert.assertNotNull("config", config);
    Assert.assertNotNull("buffer", buffer);
    sequenceNumber = new AtomicLong(0L);
    activeSessionIds = new CopyOnWriteArraySet();
    this.config = config;
    this.buffer = buffer;

    // keep at the end of the constructor to make sure that all the initialization
    // is done before registering this instance as a listener
    this.buffer.addListener(this);
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return NOTIFICATION_INFO;
  }

  @Override
  protected synchronized void enabledStateChanged() {
    if (isEnabled()) {
      enableTimer();
    } else {
      disableTimer();
    }
  }

  private synchronized void enableTimer() {
    if (timer != null || task != null) {
      disableTimer();
    }

    timer = new Timer("Statistics Emitter Timer", true);
    task = new SendStatsTask();
    timer.scheduleAtFixedRate(task, 0, config.getParamLong(DSOStatisticsConfig.KEY_EMITTER_SCHEDULE_INTERVAL));
  }

  private synchronized void disableTimer() {
    if (task != null) {
      task.shutdown();
      task = null;
    }
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
  }

  public void reset() {
    //
  }

  public void capturingStarted(final String sessionId) {
    activeSessionIds.add(sessionId);
  }

  public void capturingStopped(final String sessionId) {
    //
  }

  public void opened() {
    //
  }

  public void closing() {
    disableTimer();
  }

  public void closed() {
    //
  }

  private class SendStatsTask extends TimerTask {
    private boolean shutdown = false;

    public void shutdown() {
      synchronized (StatisticsEmitterMBeanImpl.this) {
        shutdown = true;
      }
    }

    @Override
    public void run() {
      synchronized (StatisticsEmitterMBeanImpl.this) {
        if (shutdown) {
          cancel();
          return;
        }
      }

      boolean has_listeners = hasListeners();
      if (has_listeners && !activeSessionIds.isEmpty()) {
        for (Iterator it = activeSessionIds.iterator(); it.hasNext();) {
          try {
            // todo: needs to support deferring sending until the capturing session shutdown
            final List notification_data = new ArrayList();
            buffer.consumeStatistics((String) it.next(), new StatisticsConsumer() {
              public long getMaximumConsumedDataCount() {
                return config.getParamLong(DSOStatisticsConfig.KEY_EMITTER_BATCH_SIZE);
              }

              public boolean consumeStatisticData(StatisticData data) {
                notification_data.add(data);

                // detect when a capture session has shut down and remove it
                // from the list of active sessions
                if (SRAShutdownTimestamp.ACTION_NAME.equals(data.getName())) {
                  activeSessionIds.remove(data.getSessionId());
                }

                return true;
              }
            });

            // create the notification event
            final Notification notification = new Notification(STATISTICS_EMITTER_DATA_TYPE,
                                                               StatisticsEmitterMBeanImpl.this,
                                                               sequenceNumber.incrementAndGet(),
                                                               System.currentTimeMillis());
            notification.setUserData(notification_data);
            sendNotification(notification);
          } catch (StatisticsBufferException e) {
            LOGGER.error("Unexpected error while emitting buffered statistics.", e);
          }
        }
      }
    }
  }
}