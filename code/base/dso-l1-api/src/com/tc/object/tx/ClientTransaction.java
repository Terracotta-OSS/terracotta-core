/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.management.beans.tx.ClientTxMonitorMBean;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.util.SequenceID;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Hangs on to a grouping of changes to be sent as a batch to the server
 *
 * @author steve
 */
public interface ClientTransaction {

  public void setTransactionContext(ITransactionContext transactionContext);

  public Map getChangeBuffers();

  public Map getNewRoots();

  public LockID getLockID();

  public LockID[] getAllLockIDs();

  public TransactionID getTransactionID();

  public void createObject(TCObject source);

  public void createRoot(String name, ObjectID rootID);

  public void fieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index);

  public void literalValueChanged(TCObject source, Object newValue, Object oldValue);

  public void arrayChanged(TCObject source, int startPos, Object array, int length);

  public void logicalInvoke(TCObject source, int method, Object[] parameters, String methodName);

  public boolean hasChangesOrNotifies();

  public boolean isNull();

  public TxnType getTransactionType();

  public List addNotifiesTo(List notifies);

  public void addNotify(Notify notify);

  public void setSequenceID(SequenceID sequenceID);

  public SequenceID getSequenceID();

  public boolean isConcurrent();

  public void setAlreadyCommitted();

  public boolean hasChanges();

  public int getNotifiesCount();

  public void updateMBean(ClientTxMonitorMBean txMBean);

  public Collection getReferencesOfObjectsInTxn();
  
  public void addDmiDescritor(DmiDescriptor dd);
  
  public List getDmiDescriptors();

}
