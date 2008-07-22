/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.actions.SRAShutdownTimestamp;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationListener;

public class CollectingNotificationListener implements NotificationListener {
  private boolean shutdown = false;
  private int nodesToShutdown;

  public CollectingNotificationListener(final int nodesToShutdown) {
    this.nodesToShutdown = nodesToShutdown;
  }

  public boolean getShutdown() {
    return shutdown;
  }

  public void handleNotification(Notification notification, Object o) {
    Assert.assertTrue("Expecting notification data to be a collection", o instanceof Collection);

    List data = (List)notification.getUserData();
    ((Collection)o).addAll(data);
    for (Iterator it = data.iterator(); it.hasNext(); ) {
      // System.out.println(data);
      if (SRAShutdownTimestamp.ACTION_NAME.equals(((StatisticData)it.next()).getName())) {
        synchronized (this) {
          nodesToShutdown--;
          // System.out.println(">>> nodesToShutdown = "+nodesToShutdown);
          if (0 == nodesToShutdown) {
            shutdown = true;
            this.notifyAll();
          }
        }
      }
    }
  }
}