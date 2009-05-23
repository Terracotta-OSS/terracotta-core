/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.appevent.ApplicationEvent;
import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.bytecode.Manageable;
import com.tc.object.dna.api.DNA;
import com.tc.object.loaders.LoaderDescription;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.util.ToggleableStrongReference;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class TestClientObjectManager implements ClientObjectManager {
  public final Map                 objects         = new HashMap();
  public final Map                 object2TCObject = new IdentityHashMap();
  private int                      idSequence      = 1;
  private final Object             root            = new IdentityHashMap();
  private boolean                  isManaged;
  private ReferenceQueue           referenceQueue;
  private ClientTransactionManager txManager;

  public void add(final Object id, final TCObject tc) {
    this.objects.put(id, tc);
    this.object2TCObject.put(tc.getPeerObject(), tc);
  }

  public void setIsManaged(final boolean b) {
    this.isManaged = b;
  }

  public boolean getIsManaged() {
    return this.isManaged;
  }

  public boolean isManaged(final Object pojo) {
    return this.object2TCObject.containsKey(pojo) || this.isManaged;
  }

  public boolean isPortableInstance(final Object pojo) {
    return true;
  }

  public boolean isPortableClass(final Class clazz) {
    return true;
  }

  public void sharedIfManaged(final Object pojo) {
    if (isManaged(pojo)) {
      lookupOrCreate(pojo);
    }
  }

  public synchronized TCObject lookupOrCreate(final Object obj) {
    // System.out.println(this + ".lookupOrCreate(" + obj + ")");
    TCObject rv = lookup(obj);
    if (rv == null) {
      rv = new MockTCObject(new ObjectID(this.idSequence++), obj);
      this.object2TCObject.put(obj, rv);
      if (obj instanceof Manageable) {
        ((Manageable) obj).__tc_managed(rv);
      }
    }
    return rv;
  }

  private synchronized TCObject lookup(final Object obj) {
    TCObject rv = (TCObject) this.object2TCObject.get(obj);
    return rv;
  }

  public Object lookupOrCreateRoot(final String name, final Object candidate) {
    Object rv = null;
    if (candidate == null) {
      rv = this.root;
    } else {
      rv = candidate;
    }
    Assert.assertNotNull(rv);
    return rv;
  }

  public TCObject lookupIfLocal(final ObjectID id) {
    throw new ImplementMe();
  }

  public Collection getAllObjectIDsAndClear(final Collection c) {
    c.addAll(this.objects.keySet());
    return c;
  }

  public TCObject lookup(final ObjectID id) {
    System.out.println(this + ".lookup(" + id + ")");
    return (TCObject) this.objects.get(id);
  }

  public WeakReference createNewPeer(final TCClass clazz, final int size, final ObjectID id, final ObjectID parentID) {
    throw new ImplementMe();
  }

  public Object lookupObject(final ObjectID id) {
    return ((TCObject) this.objects.get(id)).getPeerObject();
  }

  public Object lookupObject(final ObjectID id, final ObjectID parentContext) {
    return ((TCObject) this.objects.get(id)).getPeerObject();
  }

  public TCClass getOrCreateClass(final Class clazz) {
    throw new ImplementMe();
  }

  public void setTransactionManager(final ClientTransactionManager txManager) {
    this.txManager = txManager;
  }

  public void setReferenceQueue(final ReferenceQueue rq) {
    this.referenceQueue = rq;
  }

  public ReferenceQueue getReferenceQueue() {
    return this.referenceQueue;
  }

  public ObjectID lookupExistingObjectID(final Object obj) {
    return ((TCObject) this.object2TCObject.get(obj)).getObjectID();
  }

  public void markReferenced(final TCObject tcobj) {
    // mark referenced
  }

  public void shutdown() {
    //
  }

  public ClientTransactionManager getTransactionManager() {
    return this.txManager;
  }

  public Class getClassFor(final String className, final LoaderDescription loaderDesc) {
    throw new ImplementMe();
  }

  public TCObject lookupExistingOrNull(final Object pojo) {
    // if (isManaged) {
    // lookupOrCreate(pojo);
    // }

    return (TCObject) this.object2TCObject.get(pojo);
  }

  public Object lookupRoot(final String name) {
    throw new ImplementMe();
  }

  public void unpause() {
    return;
  }

  public void pause() {
    return;
  }

  public void starting() {
    return;
  }

  public void checkPortabilityOfField(final Object value, final String fieldName, final Object pojo)
      throws TCNonPortableObjectError {
    return;
  }

  public void checkPortabilityOfLogicalAction(final Object[] params, final int index, final String methodName,
                                              final Object pojo) throws TCNonPortableObjectError {
    return;
  }

  public WeakReference createNewPeer(final TCClass clazz, final DNA dna) {
    throw new ImplementMe();
  }

  public void replaceRootIDIfNecessary(final String rootName, final ObjectID newRootID) {
    throw new ImplementMe();
  }

  public Object lookupOrCreateRoot(final String name, final Object obj, final boolean dsoFinal) {
    throw new ImplementMe();
  }

  public TCObject lookupOrShare(final Object pojo) {
    throw new ImplementMe();
  }

  public boolean isCreationInProgress() {
    return false;
  }

  public void addPendingCreateObjectsToTransaction() {
    // do nothing
  }

  public boolean hasPendingCreateObjects() {
    return false;
  }

  public Object lookupObjectNoDepth(final ObjectID id) {
    throw new ImplementMe();
  }

  public Object lookupOrCreateRootNoDepth(final String rootName, final Object object) {
    throw new ImplementMe();
  }

  public Object createOrReplaceRoot(final String rootName, final Object r) {
    throw new ImplementMe();
  }

  public void storeObjectHierarchy(final Object pojo, final ApplicationEventContext context) {
    throw new ImplementMe();
  }

  public void sendApplicationEvent(final Object pojo, final ApplicationEvent event) {
    throw new ImplementMe();
  }

  public Object cloneAndInvokeLogicalOperation(final Object pojo, final String methodName, final Object[] parameters) {
    throw new ImplementMe();
  }

  public ToggleableStrongReference getOrCreateToggleRef(final ObjectID id, final Object peer) {
    throw new ImplementMe();
  }

  public String dump() {
    throw new ImplementMe();
  }

  public void dumpToLogger() {
    throw new ImplementMe();
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    throw new ImplementMe();
  }

  public WeakReference newWeakObjectReference(final ObjectID objectID, final Object peer) {
    return new WeakObjectReference(objectID, peer, this.referenceQueue);
  }

  public boolean isLocal(final ObjectID objectID) {
    throw new ImplementMe();
  }

  public void preFetchObject(ObjectID id) {
    throw new ImplementMe();
  }
}
