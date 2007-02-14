/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.session.SessionID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MockTransactionManager for unit testing.
 */
public class MockTransactionManager implements ClientTransactionManager {

  private static final TCLogger logger = TCLogging.getLogger(MockTransactionManager.class);

  private int                   commitCount;
  private List                  begins = new ArrayList();

  public void clearBegins() {
    begins.clear();
  }

  public List getBegins() {
    List rv = new ArrayList();
    rv.addAll(begins);
    return rv;
  }

  public void begin(String lock, int type) {
    // System.err.println(this + ".begin(" + lock + ")");

    begins.add(new Begin(lock, type));
  }

  public void clearCommitCount() {
    this.commitCount = 0;
  }

  public int getCommitCount() {
    return this.commitCount;
  }

  public void createObject(TCObject source) {
    throw new ImplementMe();
  }

  public void createRoot(String name, ObjectID id) {
    throw new ImplementMe();
  }

  public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    throw new ImplementMe();
  }

  public void logicalInvoke(TCObject source, int name, String methodName, Object[] parameters) {
    throw new ImplementMe();
  }

  public static class Begin {
    public String lockName;
    public int    lockType;

    Begin(String name, int type) {
      this.lockName = name;
      this.lockType = type;
    }
  }

  public void wait(WaitInvocation call, Object object) {
    throw new ImplementMe();
  }

  public void notify(String lockName, boolean all, Object object) throws UnlockedSharedObjectException {
    throw new ImplementMe();
  }

  public void receivedBatchAcknowledgement(TxnBatchID batchID) {
    throw new ImplementMe();
  }

  public void apply(TxnType txType, LockID[] lockIDs, Collection objectChanges, Set lookupObjectIDs, Map newRoots) {
    throw new ImplementMe();
  }

  public void checkWriteAccess(Object context) {
    //
  }

  public void receivedAcknowledgement(SessionID sessionID, TransactionID requestID) {
    throw new ImplementMe();
  }

  public void addReference(TCObject tco) {
    throw new ImplementMe();
  }

  public ChannelIDProvider getChannelIDProvider() {
    return null;
  }

  public boolean isLocked(String lockName) {
    throw new ImplementMe();
  }

  public void commit(String lockName) throws UnlockedSharedObjectException {
    if (logger.isDebugEnabled()) {
      logger.debug("commit(lockName)");
    }
    this.commitCount++;
  }

  public void wait(String lockName, WaitInvocation call, Object object) throws UnlockedSharedObjectException {
    throw new ImplementMe();
  }

  public void lock(String lockName, int lockLevel) {
    throw new ImplementMe();
  }

  public void unlock(String lockName) {
    throw new ImplementMe();
  }

  public int heldCount(String lockName, int lockLevel) {
    throw new ImplementMe();
  }

  public int queueLength(String lockName) {
    throw new ImplementMe();
  }

  public int waitLength(String lockName) {
    throw new ImplementMe();
  }

  public ClientTransaction getTransaction() {
    throw new ImplementMe();
  }

  public void disableTransactionLogging() {
    return;
  }

  public void enableTransactionLogging() {
    return;
  }

  public boolean isTransactionLoggingDisabled() {
    throw new ImplementMe();
  }

  public boolean isHeldByCurrentThread(String lockName, int lockLevel) {
    throw new ImplementMe();
  }

  public boolean tryBegin(String lock, int lockLevel) {
    throw new ImplementMe();
  }

  public void literalValueChanged(TCObject source, Object newValue, Object oldValue) {
    throw new ImplementMe();
  }

  public void arrayChanged(TCObject src, int startPos, Object array, int length) {
    throw new ImplementMe();
  }

  public void addDmiDescriptor(DmiDescriptor d) {
    throw new ImplementMe();
  }
}
