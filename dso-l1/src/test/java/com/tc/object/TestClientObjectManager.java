/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.net.GroupID;
import com.tc.object.LogicalOperation;
import com.tc.object.bytecode.Manageable;
import com.tc.object.dna.api.DNA;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.util.Assert;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class TestClientObjectManager implements ClientObjectManager {
  public final Map                 objects         = new HashMap();
  public final Map                 object2TCObject = new IdentityHashMap();
  private final ReferenceQueue     referenceQueue  = new ReferenceQueue();
  private final Object             root            = new IdentityHashMap();
  private boolean                  isManaged;
  private int                      idSequence      = 1;
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

  @Override
  public boolean isManaged(final Object pojo) {
    return this.object2TCObject.containsKey(pojo) || this.isManaged;
  }

  @Override
  public boolean isPortableInstance(final Object pojo) {
    return true;
  }

  @Override
  public boolean isPortableClass(final Class clazz) {
    return true;
  }

  public void sharedIfManaged(final Object pojo) {
    if (isManaged(pojo)) {
      lookupOrCreate(pojo);
    }
  }

  @Override
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

  @Override
  public synchronized TCObject lookupOrCreate(final Object obj, GroupID gid) {
    return lookupOrCreate(obj);
  }

  private synchronized TCObject lookup(final Object obj) {
    final TCObject rv = (TCObject) this.object2TCObject.get(obj);
    return rv;
  }

  @Override
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

  @Override
  public TCObject lookupIfLocal(final ObjectID id) {
    throw new ImplementMe();
  }

  @Override
  public TCObject lookup(final ObjectID id) {
    System.out.println(this + ".lookup(" + id + ")");
    return (TCObject) this.objects.get(id);
  }

  @Override
  public TCObject lookupQuiet(final ObjectID id) {
    return lookup(id);
  }

  @Override
  public WeakReference createNewPeer(final TCClass clazz, final int size, final ObjectID id, final ObjectID parentID) {
    throw new ImplementMe();
  }

  @Override
  public Object lookupObjectQuiet(final ObjectID id) {
    return lookupObject(id);
  }

  @Override
  public Object lookupObject(final ObjectID id) {
    return ((TCObject) this.objects.get(id)).getPeerObject();
  }

  @Override
  public Object lookupObject(final ObjectID id, final ObjectID parentContext) {
    return ((TCObject) this.objects.get(id)).getPeerObject();
  }

  @Override
  public TCClass getOrCreateClass(final Class clazz) {
    throw new ImplementMe();
  }

  @Override
  public void setTransactionManager(final ClientTransactionManager txManager) {
    this.txManager = txManager;
  }

  @Override
  public ObjectID lookupExistingObjectID(final Object obj) {
    return ((TCObject) this.object2TCObject.get(obj)).getObjectID();
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    //
  }

  @Override
  public ClientTransactionManager getTransactionManager() {
    return this.txManager;
  }

  @Override
  public Class getClassFor(final String className) {
    throw new ImplementMe();
  }

  @Override
  public TCObject lookupExistingOrNull(final Object pojo) {
    // if (isManaged) {
    // lookupOrCreate(pojo);
    // }

    return (TCObject) this.object2TCObject.get(pojo);
  }

  @Override
  public Object lookupRoot(final String name) {
    throw new ImplementMe();
  }

  @Override
  public void checkPortabilityOfField(final Object value, final String fieldName, final Object pojo)
      throws TCNonPortableObjectError {
    return;
  }

  @Override
  public void checkPortabilityOfLogicalAction(final LogicalOperation method, final Object[] params, final int index,
                                              final Object pojo) throws TCNonPortableObjectError {
    return;
  }

  @Override
  public WeakReference createNewPeer(final TCClass clazz, final DNA dna) {
    throw new ImplementMe();
  }

  @Override
  public void replaceRootIDIfNecessary(final String rootName, final ObjectID newRootID) {
    throw new ImplementMe();
  }

  @Override
  public Object lookupOrCreateRoot(final String name, final Object obj, final boolean dsoFinal) {
    throw new ImplementMe();
  }

  @Override
  public boolean isCreationInProgress() {
    return false;
  }

  @Override
  public Object lookupObjectNoDepth(final ObjectID id) {
    throw new ImplementMe();
  }

  @Override
  public Object lookupOrCreateRootNoDepth(final String rootName, final Object object) {
    throw new ImplementMe();
  }

  @Override
  public Object createOrReplaceRoot(final String rootName, final Object r) {
    throw new ImplementMe();
  }

  @Override
  public WeakReference newWeakObjectReference(final ObjectID objectID, final Object peer) {
    return new WeakObjectReference(objectID, peer, this.referenceQueue);
  }

  @Override
  public boolean isLocal(final ObjectID objectID) {
    throw new ImplementMe();
  }

  @Override
  public void preFetchObject(final ObjectID id) {
    throw new ImplementMe();
  }

  @Override
  public void removedTCObjectSelfFromStore(TCObjectSelf value) {
    throw new ImplementMe();
  }

  @Override
  public void initializeTCClazzIfRequired(TCObjectSelf tcoObjectSelf) {
    throw new ImplementMe();

  }

  @Override
  public Object lookupOrCreateRoot(String name, Object obj, GroupID gid) {
    throw new ImplementMe();
  }

  @Override
  public Object lookupRoot(String name, GroupID groupID) {
    throw new ImplementMe();
  }

  @Override
  public TCObject addLocalPrefetch(DNA object) {
    throw new ImplementMe();
  }
}
