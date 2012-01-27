package com.tc.admin.model;

import com.tc.operatorevent.TerracottaOperatorEvent;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.swing.event.EventListenerList;

public class OperatorEventsListener implements NotificationListener {

  private final EventListenerList listenerList;

  public OperatorEventsListener(EventListenerList listenerList) {
    this.listenerList = listenerList;
  }

  public void handleNotification(Notification notification, Object handback) {
    TerracottaOperatorEvent tcOperatorEvent = (TerracottaOperatorEvent) notification.getUserData();
    fireOperatorEvent(tcOperatorEvent);
  }

  private void fireOperatorEvent(TerracottaOperatorEvent tcOperatorEvent) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TerracottaOperatorEventsListener.class) {
        ((TerracottaOperatorEventsListener) listeners[i + 1]).statusUpdate(tcOperatorEvent);
      }
    }
  }

}