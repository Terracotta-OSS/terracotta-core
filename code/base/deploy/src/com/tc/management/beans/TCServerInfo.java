/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.config.schema.L2Info;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateChangeListener;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.server.TCServer;
import com.tc.util.ProductInfo;
import com.tc.util.State;

import java.util.Timer;
import java.util.TimerTask;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;

public class TCServerInfo extends AbstractTerracottaMBean implements TCServerInfoMBean, StateChangeListener {
  private static final boolean                 DEBUG = false;

  private static final MBeanNotificationInfo[] NOTIFICATION_INFO;
  static {
    final String[] notifTypes = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
    final String name = AttributeChangeNotification.class.getName();
    final String description = "An attribute of this MBean has changed";
    NOTIFICATION_INFO = new MBeanNotificationInfo[] { new MBeanNotificationInfo(notifTypes, name, description) };
  }

  private final TCServer                       server;
  private final ProductInfo                    productInfo;
  private final String                         buildID;
  private final L2State                        l2State;
  private final StateChangeNotificationInfo    stateChangeNotificationInfo;
  private long                                 nextSequenceNumber;

  public TCServerInfo(final TCServer server, final L2State l2State) throws NotCompliantMBeanException {
    super(TCServerInfoMBean.class, true);
    this.server = server;
    this.l2State = l2State;
    this.l2State.registerStateChangeListener(this);
    productInfo = ProductInfo.getInstance();
    buildID = makeBuildID(productInfo);
    nextSequenceNumber = 1;
    stateChangeNotificationInfo = new StateChangeNotificationInfo();
  }

  public void reset() {
    // nothing to reset
  }

  public boolean isStarted() {
    return l2State.isStartState();
  }

  public boolean isActive() {
    return l2State.isActiveCoordinator();
  }

  public boolean isPassiveUninitialized() {
    return l2State.isPassiveUninitialized();
  }

  public boolean isPassiveStandby() {
    return l2State.isPassiveStandby();
  }

  public long getStartTime() {
    return server.getStartTime();
  }

  public long getActivateTime() {
    return server.getActivateTime();
  }

  public void stop() {
    server.stop();
    _sendNotification("TCServer stopped", "Started", "jmx.terracotta.L2.stopped", Boolean.TRUE, Boolean.FALSE);
  }

  /**
   * This schedules the shutdown to occur one second after we return from this call because otherwise JMX will be
   * shutdown and we'll get all sorts of other errors trying to return from this call.
   */
  public void shutdown() {
    final Timer timer = new Timer();
    final TimerTask task = new TimerTask() {
      public void run() {
        server.shutdown();
      }
    };
    timer.schedule(task, 1000);
  }

  public MBeanNotificationInfo[] getNotificationInfo() {
    return NOTIFICATION_INFO;
  }

  public String toString() {
    if (isStarted()) {
      return "starting, startTime(" + getStartTime() + ")";
    } else if (isActive()) {
      return "active, activateTime(" + getActivateTime() + ")";
    } else {
      return "stopped";
    }
  }

  public String getVersion() {
    return productInfo.toShortString();
  }

  public String getBuildID() {
    return buildID;
  }

  public String getCopyright() {
    return productInfo.copyright();
  }

  public String getDescriptionOfCapabilities() {
    return server.getDescriptionOfCapabilities();
  }

  public L2Info[] getL2Info() {
    return server.infoForAllL2s();
  }

  private static String makeBuildID(final ProductInfo productInfo) {
    String timeStamp = productInfo.buildTimestampAsString();
    String revision = productInfo.buildRevision();
    String user = productInfo.buildUser();
    String host = productInfo.buildHost();
    String branch = productInfo.buildBranch();
    return timeStamp + " (" + revision + " by " + user + "@" + host + " from "
           + branch + ")";
  }

  public String getHealthStatus() {
    // FIXME: the returned value should eventually contain a true representative status of L2 server.
    // for now just return 'OK' to indicate that the process is up-and-running..
    return "OK";
  }

  public void l2StateChanged(StateChangedEvent sce) {
    State state = sce.getCurrentState();

    debugPrintln("*****  msg=[" + stateChangeNotificationInfo.getMsg(state) + "] attrName=["
                 + stateChangeNotificationInfo.getAttributeName(state) + "] attrType=["
                 + stateChangeNotificationInfo.getAttributeType(state) + "] stateName=[" + state.getName() + "]");

    _sendNotification(stateChangeNotificationInfo.getMsg(state), stateChangeNotificationInfo.getAttributeName(state),
                      stateChangeNotificationInfo.getAttributeType(state), Boolean.FALSE, Boolean.TRUE);
  }

  private synchronized void _sendNotification(String msg, String attr, String type, Object oldVal, Object newVal) {
    sendNotification(new AttributeChangeNotification(this, nextSequenceNumber++, System.currentTimeMillis(), msg, attr,
                                                     type, oldVal, newVal));
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }
}
