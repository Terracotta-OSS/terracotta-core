/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.events;

import org.terracotta.toolkit.events.ToolkitNotificationListener;
import org.terracotta.toolkit.events.ToolkitNotifier;

import com.tc.object.SerializationUtil;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.locks.LockLevel;
import com.terracotta.toolkit.TerracottaLogger;
import com.terracotta.toolkit.cluster.TerracottaNode;
import com.terracotta.toolkit.object.AbstractTCToolkitObject;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ToolkitNotifierImpl<T> extends AbstractTCToolkitObject implements ToolkitNotifier<T> {

  private static final TerracottaLogger                                        LOGGER    = new TerracottaLogger(
                                                                                                                ToolkitNotifierImpl.class);
  private final transient CopyOnWriteArrayList<ToolkitNotificationListener<T>> listeners = new CopyOnWriteArrayList<ToolkitNotificationListener<T>>();
  private final String                                                         currentNodeIdStringForm;
  private volatile String                                                      lockid;

  public ToolkitNotifierImpl() {
    this.currentNodeIdStringForm = strategy.serializeToString(new TerracottaNode(ManagerUtil.getManager()
        .getDsoCluster().getCurrentNode()));
  }

  @Override
  public String getName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addNotificationListener(ToolkitNotificationListener<T> listener) {
    listeners.addIfAbsent(listener);
  }

  @Override
  public void removeNotificationListener(ToolkitNotificationListener<T> listener) {
    listeners.remove(listener);
  }

  @Override
  public void notifyListeners(T msg) {
    begin();
    try {
      unlockedNotifyListeners(msg);
    } finally {
      commit();
    }
  }

  private void unlockedNotifyListeners(T msg) {
    String stringMsg = null;
    stringMsg = strategy.serializeToString(msg);
    ManagerUtil.logicalInvoke(this, SerializationUtil.CLUSTERED_NOTIFIER_SIGNATURE, new Object[] { stringMsg,
        currentNodeIdStringForm });
  }

  /**
   * Called by applicator on receiving a remote msg
   */
  protected void onNotification(String remoteMsg, String remoteNodeID) {
    ToolkitNotificationEventImpl<T> event = new ToolkitNotificationEventImpl<T>(strategy, remoteNodeID, remoteMsg);
    for (ToolkitNotificationListener<T> listener : listeners) {
      try {
        listener.onNotification(event);
      } catch (Throwable t) {
        // ignore any exception happening on listeners
        LOGGER.warn("Exception while trying to notify listener ", t);
      }
    }
  }

  private void begin() {
    String lockID = getLockID();
    ManagerUtil.beginLock(lockID, LockLevel.WRITE_LEVEL);
  }

  private void commit() {
    String lockID = getLockID();
    ManagerUtil.commitLock(lockID, LockLevel.WRITE_LEVEL);
  }

  private String getLockID() {
    if (lockid != null) { return lockid; }

    lockid = "__tc_clusteredNotifier_" + tcObject.getObjectID();
    return lockid;
  }

  @Override
  public List<ToolkitNotificationListener<T>> getNotificationListeners() {
    return Collections.unmodifiableList(this.listeners);
  }

  @Override
  public void cleanupOnDestroy() {
    //
  }
}
