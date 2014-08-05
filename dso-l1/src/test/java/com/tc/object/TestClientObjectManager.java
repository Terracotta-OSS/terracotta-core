/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.net.GroupID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.dna.api.DNA;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.platform.PlatformService;
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
  private int                      idSequence      = 1;
  private ClientTransactionManager txManager;

  public void add(final Object id, final TCObject tc) {
    this.objects.put(id, tc);
    this.object2TCObject.put(tc.getPeerObject(), tc);
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
  public TCObject lookup(final ObjectID id) {
    System.out.println(this + ".lookup(" + id + ")");
    return (TCObject) this.objects.get(id);
  }

  @Override
  public TCObject lookupQuiet(final ObjectID id) {
    return lookup(id);
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
  public TCClass getOrCreateClass(final Class clazz) {
    throw new ImplementMe();
  }

  @Override
  public void setTransactionManager(final ClientTransactionManager txManager) {
    this.txManager = txManager;
  }

  @Override
  public void setPlatformService(PlatformService platformService) {
    throw new ImplementMe();
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
  public boolean isCreationInProgress() {
    return false;
  }

  @Override
  public WeakReference newWeakObjectReference(final ObjectID objectID, final Object peer) {
    return new WeakObjectReference(objectID, peer, this.referenceQueue);
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
