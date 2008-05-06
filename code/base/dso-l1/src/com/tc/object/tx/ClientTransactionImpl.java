/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.management.beans.tx.ClientTxMonitorMBean;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.change.TCChangeBuffer;
import com.tc.object.change.TCChangeBufferImpl;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.logging.RuntimeLogger;
import com.tc.util.Assert;

import gnu.trove.TIntArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author steve
 */
public class ClientTransactionImpl extends AbstractClientTransaction {
  private final RuntimeLogger runtimeLogger;
  private final Map           objectChanges = new HashMap();

  private Map                 newRoots;
  private List                notifies;
  private List                dmis;

  // used to keep things referenced until the transaction is completely ACKED
  private final Map           referenced    = new IdentityHashMap();

  public ClientTransactionImpl(TransactionID txID, RuntimeLogger logger) {
    super(txID);
    this.runtimeLogger = logger;
  }

  public boolean isConcurrent() {
    return this.getTransactionType().isConcurrent();
  }

  public boolean hasChangesOrNotifies() {
    return !(objectChanges.isEmpty() && getNewRoots().isEmpty() && getNotifies().isEmpty());
  }

  public boolean hasChanges() {
    return !(objectChanges.isEmpty() && getNewRoots().isEmpty());
  }

  public Map getNewRoots() {
    return newRoots == null ? Collections.EMPTY_MAP : newRoots;
  }

  public List getNotifies() {
    return notifies == null ? Collections.EMPTY_LIST : notifies;
  }

  public Map getChangeBuffers() {
    return this.objectChanges;
  }

  protected void basicLiteralValueChanged(TCObject source, Object newValue, Object oldValue) {
    if (runtimeLogger.getFieldChangeDebug()) {
      runtimeLogger.literalValueChanged(source, newValue);
    }

    getOrCreateChangeBuffer(source).literalValueChanged(newValue);
    // To prevent it gcing on us.
    addReferenced(oldValue);
    addReferenced(newValue);
  }

  protected void basicFieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    if (runtimeLogger.getFieldChangeDebug()) {
      runtimeLogger.fieldChanged(source, classname, fieldname, newValue, index);
    }

    getOrCreateChangeBuffer(source).fieldChanged(classname, fieldname, newValue, index);
  }

  protected void basicArrayChanged(TCObject source, int startPos, Object array, int length) {
    if (runtimeLogger.getArrayChangeDebug()) {
      runtimeLogger.arrayChanged(source, startPos, array);
    }

    getOrCreateChangeBuffer(source).arrayChanged(startPos, array, length);
  }

  protected void basicLogicalInvoke(TCObject source, int method, Object[] parameters) {
    getOrCreateChangeBuffer(source).logicalInvoke(method, parameters);
  }

  protected void basicCreate(TCObject object) {
    getOrCreateChangeBuffer(object);
  }

  protected void basicCreateRoot(String name, ObjectID root) {
    if (newRoots == null) {
      newRoots = new HashMap();
    }
    newRoots.put(name, root);
  }

  private TCChangeBuffer getOrCreateChangeBuffer(TCObject object) {
    addReferenced(object.getPeerObject());

    ObjectID oid = object.getObjectID();

    TCChangeBuffer cb = (TCChangeBuffer) objectChanges.get(oid);
    if (cb == null) {
      cb = new TCChangeBufferImpl(object);
      objectChanges.put(oid, cb);
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
    if (!notify.isNull()) {
      if (notifies == null) {
        notifies = new ArrayList();
      }

      notifies.add(notify);
    }
  }

  public String toString() {
    return "ClientTransactionImpl [ " + getTransactionID() + " ]";
  }

  public int getNotifiesCount() {
    return getNotifies().size();
  }

  public void updateMBean(ClientTxMonitorMBean txMBean) {
    int modifiedObjectCount = 0;
    final TIntArrayList writesPerObject = new TIntArrayList(objectChanges.size());

    final Map creationCountByClass = new HashMap();
    if (!objectChanges.isEmpty()) {
      int currentIndex = 0;
      for (Iterator iter = objectChanges.values().iterator(); iter.hasNext(); currentIndex++) {
        final TCChangeBuffer buffer = (TCChangeBuffer) iter.next();
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

  public void addDmiDescritor(DmiDescriptor dd) {
    if (dmis == null) {
      dmis = new ArrayList();
    }
    dmis.add(dd);
  }

  public List getDmiDescriptors() {
    return dmis == null ? Collections.EMPTY_LIST : dmis;
  }


}