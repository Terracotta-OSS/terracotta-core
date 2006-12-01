/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import java.util.Date;

import javax.swing.Icon;

import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XTreeNode;

import com.tc.objectserver.lockmanager.api.LockMBean;
import com.tc.objectserver.lockmanager.api.LockHolder;
import com.tc.objectserver.lockmanager.api.ServerLockRequest;
import com.tc.objectserver.lockmanager.api.Waiter;

public class LockTreeNode extends XTreeNode {
  private XTreeNode m_holders;
  private XTreeNode m_pendingRequests;
  private XTreeNode m_pendingUpgrades;
  private XTreeNode m_waiters;

  public LockTreeNode(ConnectionContext cc, LockMBean lock) {
    super(lock);

    LockHolder[] holders = lock.getHolders();
    if(holders != null && holders.length > 0) {
      add(m_holders = new XTreeNode("Holders"));
      addHolders(holders);
    }

    ServerLockRequest[] pendingRequests = lock.getPendingRequests();
    if(pendingRequests != null && pendingRequests.length > 0) {
      add(m_pendingRequests = new XTreeNode("Pending Requests"));
      addPendingRequests(pendingRequests);
    }

    ServerLockRequest[] pendingUpgrades = lock.getPendingUpgrades();
    if(pendingUpgrades != null && pendingUpgrades.length > 0) {
      add(m_pendingUpgrades = new XTreeNode("Pending Upgrades"));
      addPendingUpgrades(pendingUpgrades);
    }

    Waiter[] waiters = lock.getWaiters();
    if(waiters != null && waiters.length > 0) {
      add(m_waiters = new XTreeNode("Waiters"));
      addWaiters(waiters);
    }
  }

  public LockMBean getLock() {
    return (LockMBean)getUserObject();
  }

  public Icon getIcon() {
    return LocksHelper.getHelper().getLockIcon();
  }

  public String toString() {
    return getLock().getLockName();
  }

  public void addHolders(LockHolder[] holders) {
    for(int i = 0; i < holders.length; i++) {
      m_holders.add(new HolderNode(holders[i]));
    }
  }

  class HolderNode extends XTreeNode {
    private Date acquiredDate;

    HolderNode(LockHolder holder) {
      super(holder);
      acquiredDate = new Date(holder.getTimeAcquired());
    }

    public LockHolder getHolder() {
      return (LockHolder)getUserObject();
    }

    public String toString() {
      LockHolder holder = getHolder();
      return holder.getChannelID() + "," + holder.getThreadID() +
        ",level=" + holder.getLockLevel() + ",acquired=" + acquiredDate;
    }
  }

  public void addPendingRequests(ServerLockRequest[] requests) {
    for(int i = 0; i < requests.length; i++) {
      m_pendingRequests.add(new RequestNode(requests[i]));
    }
  }

  public void addPendingUpgrades(ServerLockRequest[] upgrades) {
    for(int i = 0; i < upgrades.length; i++) {
      m_pendingUpgrades.add(new RequestNode(upgrades[i]));
    }
  }

  class RequestNode extends XTreeNode {
    private Date requestDate;

    RequestNode(ServerLockRequest request) {
      super(request);
      requestDate = new Date(request.getRequestTime());
    }

    public ServerLockRequest getRequest() {
      return (ServerLockRequest)getUserObject();
    }

    public String toString() {
      ServerLockRequest request = getRequest();
      return request.getChannelID() + "," + request.getThreadID() +
        ",level=" + request.getLockLevel() + ",requested=" + requestDate;
    }
  }

  public void addWaiters(Waiter[] waiters) {
    for(int i = 0; i < waiters.length; i++) {
      m_waiters.add(new WaiterNode(waiters[i]));
    }
  }

  class WaiterNode extends XTreeNode {
    private Date startDate;

    WaiterNode(Waiter waiter) {
      super(waiter);
      startDate = new Date(waiter.getStartTime());
   }

    public Waiter getWaiter() {
      return (Waiter)getUserObject();
    }

    public String toString() {
      Waiter waiter = getWaiter();
      return waiter.getChannelID() + "," + waiter.getThreadID() +
        ",invocation=" + waiter.getWaitInvocation() + ",startTime=" + startDate;
    }
  }
}
