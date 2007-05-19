/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.session.SessionID;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Threadlocal based transaction manager interface. Changes go through here to the transaction for the current thread.
 *
 * @author steve
 */
public interface ClientTransactionManager {

  /**
   * begin a thread local transaction Probably change this from class Object to something more specific but it is still
   * early and I'm not sure what I want here
   */
  public boolean begin(String lock, int lockLevel);

  public boolean tryBegin(String lock, int lockLevel);

  /**
   * commit a thread local current transaction
   */
  public void commit(String lockName) throws UnlockedSharedObjectException;

  /**
   * When transactions come in from the L2 we use this method to apply them. We will have to get a bit fancier because
   * we can't apply any changes while we are in any transaction. The first version will not allow apply to happen while
   * ANY tx is in progress. This is probably not exceptable. We will probably figure something out with the lock manager
   * where we can accuire a read lock if a field is accessed in a transaction
   */
  public void apply(TxnType txType, LockID[] lockIDs, Collection objectChanges, Set lookupObjectIDs, Map newRoots);

  public void createObject(TCObject source);

  public void createRoot(String name, ObjectID id);

  public void literalValueChanged(TCObject source, Object newValue, Object oldValue);

  public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index);

  public void logicalInvoke(TCObject source, int method, String methodName, Object[] parameters);

  public void wait(String lockName, WaitInvocation call, Object object) throws UnlockedSharedObjectException,  InterruptedException;

  public void notify(String lockName, boolean all, Object object) throws UnlockedSharedObjectException;

  // For optimistic stuff
  public ClientTransaction getTransaction() throws UnlockedSharedObjectException;

  public void receivedAcknowledgement(SessionID sessionID, TransactionID requestID);

  public void receivedBatchAcknowledgement(TxnBatchID batchID);

  public void checkWriteAccess(Object context);

  public void addReference(TCObject tco);

  public ChannelIDProvider getChannelIDProvider();

  public boolean isLocked(String lockName);

  public void lock(String lockName, int lockLevel);

  public void unlock(String lockName);

  public boolean isHeldByCurrentThread(String lockName, int lockLevel);

  public int queueLength(String lockName);

  public int waitLength(String lockName);

  public void enableTransactionLogging();

  public void disableTransactionLogging();

  public boolean isTransactionLoggingDisabled();

  public void arrayChanged(TCObject src, int startPos, Object array, int length);
  
  public void addDmiDescriptor(DmiDescriptor d);

}
