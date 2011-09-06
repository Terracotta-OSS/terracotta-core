/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.change.TCChangeBuffer;
import com.tc.object.change.TCChangeBufferImpl;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.locks.Notify;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client side transaction : Collects all changes by a single thread under a lock
 */
public class ClientTransactionImpl extends AbstractClientTransaction {
  private final RuntimeLogger                 runtimeLogger;
  private final Map<ObjectID, TCChangeBuffer> objectChanges = new LinkedHashMap<ObjectID, TCChangeBuffer>();

  private Map                                 newRoots;
  private List                                notifies;
  private List                                dmis;

  // used to keep things referenced until the transaction is completely ACKED
  private final Map                           referenced    = new IdentityHashMap();

  public ClientTransactionImpl(RuntimeLogger logger) {
    super();
    this.runtimeLogger = logger;
  }

  public boolean isConcurrent() {
    return this.getLockType().isConcurrent();
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

  @Override
  protected void basicLiteralValueChanged(TCObject source, Object newValue, Object oldValue) {
    if (runtimeLogger.getFieldChangeDebug()) {
      runtimeLogger.literalValueChanged(source, newValue);
    }

    getOrCreateChangeBuffer(source).literalValueChanged(newValue);
    // To prevent it gcing on us.
    addReferenced(oldValue);
    addReferenced(newValue);
  }

  @Override
  protected void basicFieldChanged(TCObject source, String classname, String fieldname, Object newValue, int index) {
    if (runtimeLogger.getFieldChangeDebug()) {
      runtimeLogger.fieldChanged(source, classname, fieldname, newValue, index);
    }

    getOrCreateChangeBuffer(source).fieldChanged(classname, fieldname, newValue, index);
  }

  @Override
  protected void basicArrayChanged(TCObject source, int startPos, Object array, int length) {
    if (runtimeLogger.getArrayChangeDebug()) {
      runtimeLogger.arrayChanged(source, startPos, array);
    }

    getOrCreateChangeBuffer(source).arrayChanged(startPos, array, length);
  }

  @Override
  protected void basicLogicalInvoke(TCObject source, int method, Object[] parameters) {
    getOrCreateChangeBuffer(source).logicalInvoke(method, parameters);
  }

  @Override
  protected void basicCreate(TCObject object) {
    getOrCreateChangeBuffer(object);
  }

  @Override
  protected void basicCreateRoot(String name, ObjectID root) {
    if (newRoots == null) {
      newRoots = new HashMap();
    }
    newRoots.put(name, root);
  }

  private TCChangeBuffer getOrCreateChangeBuffer(TCObject object) {
    addReferenced(object.getPeerObject());

    ObjectID oid = object.getObjectID();

    TCChangeBuffer cb = objectChanges.get(oid);
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
    return Collections.unmodifiableCollection(referenced.keySet());
  }

  public void addNotify(Notify notify) {
    if (!notify.isNull()) {
      if (notifies == null) {
        notifies = new ArrayList();
      }

      notifies.add(notify);
    }
  }

  @Override
  public String toString() {
    return "ClientTransactionImpl@" + System.identityHashCode(this) + " [ " + getTransactionID() + " ]";
  }

  public int getNotifiesCount() {
    return getNotifies().size();
  }

  public void addDmiDescriptor(DmiDescriptor dd) {
    if (dmis == null) {
      dmis = new ArrayList();
    }
    dmis.add(dd);
  }

  @Override
  protected void basicAddMetaDataDescriptor(TCObject tco, MetaDataDescriptorInternal md) {
    getOrCreateChangeBuffer(tco).addMetaDataDescriptor(md);
  }

  public List getDmiDescriptors() {
    return dmis == null ? Collections.EMPTY_LIST : dmis;
  }

}
