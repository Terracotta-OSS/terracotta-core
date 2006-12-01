/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.management.beans.tx.ClientTxMonitorMBean;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.change.TCChangeBuffer;
import com.tc.object.change.TCChangeBufferImpl;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.logging.RuntimeLogger;
import com.tc.util.Assert;

import gnu.trove.TIntArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author steve
 */
public class ClientTransactionImpl extends AbstractClientTransaction {
  private final RuntimeLogger runtimeLogger;
  private final Map           objectChanges = new HashMap();
  private final Map           newRoots      = new HashMap();
  private final List          notifies      = new LinkedList();

  // used to keep things referenced until the transaction is completely ACKED
  private final Map           referenced    = new IdentityHashMap();

  public ClientTransactionImpl(TransactionID txID, RuntimeLogger logger, ChannelIDProvider cidProvider) {
    super(txID, cidProvider);
    this.runtimeLogger = logger;
  }

  public boolean isConcurrent() {
    return this.getTransactionType().isConcurrent();
  }

  public boolean hasChangesOrNotifies() {
    return !(objectChanges.isEmpty() && newRoots.isEmpty() && notifies.isEmpty());
  }

  public boolean hasChanges() {
    return !(objectChanges.isEmpty() && newRoots.isEmpty());
  }

  public Map getNewRoots() {
    return newRoots;
  }

  public Map getChangeBuffers() {
    return this.objectChanges;
  }

  protected void basicLiteralValueChanged(TCObject source, Object newValue, Object oldValue) {
    if (runtimeLogger.fieldChangeDebug()) {
      runtimeLogger.literalValueChanged(source, newValue);
    }

    getOrCreateChangeBuffer(source).literalValueChanged(newValue);
    // To prevent it gcing on us.
    addReferenced(oldValue);
    addReferenced(newValue);
  }

  protected void basicFieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    if (runtimeLogger.fieldChangeDebug()) {
      runtimeLogger.fieldChanged(source, classname, fieldname, newValue, index);
    }

    getOrCreateChangeBuffer(source).fieldChanged(classname, fieldname, newValue, index);
  }

  protected void basicArrayChanged(TCObject source, int startPos, Object array, int length) {
    if (runtimeLogger.arrayChangeDebug()) {
      runtimeLogger.arrayChanged(source, startPos, array);
    }

    getOrCreateChangeBuffer(source).arrayChanged(startPos, array, length);
  }

  protected void basicLogicalInvoke(TCObject source, int method, Object[] parameters) {
    getOrCreateChangeBuffer(source).logicalInvoke(method, parameters);
  }

  protected void basicCreate(TCObject object) {
    if (runtimeLogger.newManagedObjectDebug()) {
      runtimeLogger.newManagedObject(object);
    }

    getOrCreateChangeBuffer(object);
  }

  protected void basicCreateRoot(String name, ObjectID root) {
    newRoots.put(name, root);
  }

  private TCChangeBuffer getOrCreateChangeBuffer(TCObject object) {
    addReferenced(object.getPeerObject());

    TCChangeBuffer cb = (TCChangeBuffer) objectChanges.get(object);
    if (cb == null) {
      cb = new TCChangeBufferImpl(object);
      objectChanges.put(object, cb);
    }

    return cb;
  }

  private void addReferenced(Object pojo) {
    Assert.assertNotNull("pojo", pojo);
    referenced.put(pojo, null);
  }

  public Collection getReferencesOfObjectsInTxn() {
    return referenced.keySet();
  }
  
  public void addNotify(Notify notify) {
    if (!notify.isNull()) notifies.add(notify);
  }

  public List addNotifiesTo(List l) {
    l.addAll(notifies);
    return l;
  }

  public String toString() {
    return "ClientTransactionImpl [ " + getTransactionID() + " ]";
  }

  public int getNotifiesCount() {
    return notifies.size();
  }

  public void updateMBean(ClientTxMonitorMBean txMBean) {
    int modifiedObjectCount = 0;
    final TIntArrayList writesPerObject = new TIntArrayList(objectChanges.size());

    final Map creationCountByClass = new HashMap();
    if (!objectChanges.isEmpty()) {
      int currentIndex = 0;
      for (Iterator iter = objectChanges.keySet().iterator(); iter.hasNext(); currentIndex++) {
        final TCChangeBuffer buffer = (TCChangeBuffer) objectChanges.get(iter.next());
        final TCObject tco = buffer.getTCObject();
        if (tco.isNew()) {
          final Class instanceClass = tco.getTCClass().getPeerClass();
          Integer counter = (Integer) creationCountByClass.get(instanceClass);
          if (counter == null) {
            counter = new Integer(1);
          } else {
            counter = new Integer(counter.intValue() + 1);
          }
          creationCountByClass.put(instanceClass, counter);
        } else {
          ++modifiedObjectCount;
        }
        writesPerObject.add(buffer.getTotalEventCount());
      }
    }
    txMBean.committedWriteTransaction(getNotifiesCount(), modifiedObjectCount, writesPerObject.toNativeArray(),
                                      creationCountByClass);
  }

}