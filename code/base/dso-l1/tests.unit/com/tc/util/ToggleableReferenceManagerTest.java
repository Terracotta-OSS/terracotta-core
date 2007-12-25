/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.appevent.ApplicationEvent;
import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.dna.api.DNA;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.object.util.ToggleableStrongReference;
import com.tc.util.concurrent.ThreadUtil;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;

import junit.framework.TestCase;

public class ToggleableReferenceManagerTest extends TestCase {

  public void test() throws Exception {
    ToggleableReferenceManager mgr = new ToggleableReferenceManager();

    Object peer = new Object();
    mgr.setObjectManager(new ObjMgr(peer));
    mgr.start();

    ToggleableStrongReference toggleRef = mgr.getOrCreateFor(new ObjectID(1), peer);
    toggleRef.strongRef();
    peer = null;

    System.gc();
    ThreadUtil.reallySleep(5000);

    Assert.assertEquals(1, mgr.size());
    Assert.assertEquals(0, mgr.clearCount());

    toggleRef.clearStrongRef();
    System.gc();
    ThreadUtil.reallySleep(5000);

    Assert.assertEquals(0, mgr.size());
    Assert.assertEquals(1, mgr.clearCount());
  }

  private static class ObjMgr implements ClientObjectManager {

    private Object ref;

    ObjMgr(Object ref) {
      this.ref = ref;
    }

    public void addPendingCreateObjectsToTransaction() {
      throw new ImplementMe();
    }

    public void checkPortabilityOfField(Object value, String fieldName, Object pojo) throws TCNonPortableObjectError {
      throw new ImplementMe();
    }

    public void checkPortabilityOfLogicalAction(Object[] params, int paramIndex, String methodName, Object pojo)
        throws TCNonPortableObjectError {
      throw new ImplementMe();
    }

    public Object cloneAndInvokeLogicalOperation(Object logicalPojo, String methodName, Object[] parameters) {
      throw new ImplementMe();
    }

    public Object createNewCopyInstance(Object source, Object parent) {
      throw new ImplementMe();
    }

    public WeakReference createNewPeer(TCClass clazz, DNA dna) {
      throw new ImplementMe();
    }

    public WeakReference createNewPeer(TCClass clazz, int size, ObjectID id, ObjectID parentID) {
      throw new ImplementMe();
    }

    public Object createOrReplaceRoot(String rootName, Object root) {
      throw new ImplementMe();
    }

    public Object createParentCopyInstanceIfNecessary(Map visited, Map cloned, Object v) {
      throw new ImplementMe();
    }

    public Object deepCopy(Object source, OptimisticTransactionManager optimisticTxManager) {
      throw new ImplementMe();
    }

    public Collection getAllObjectIDsAndClear(Collection c) {
      throw new ImplementMe();
    }

    public Class getClassFor(String className, String loaderDesc) {
      throw new ImplementMe();
    }

    public TCClass getOrCreateClass(Class clazz) {
      throw new ImplementMe();
    }

    public ToggleableStrongReference getOrCreateToggleRef(ObjectID objectID, Object peer) {
      throw new ImplementMe();
    }

    public ReferenceQueue getReferenceQueue() {
      throw new ImplementMe();
    }

    public ClientTransactionManager getTransactionManager() {
      throw new ImplementMe();
    }

    public boolean hasPendingCreateObjects() {
      throw new ImplementMe();
    }

    public boolean isCreationInProgress() {
      throw new ImplementMe();
    }

    public boolean isManaged(Object pojo) {
      throw new ImplementMe();
    }

    public boolean isPortableClass(Class clazz) {
      throw new ImplementMe();
    }

    public boolean isPortableInstance(Object instance) {
      throw new ImplementMe();
    }

    public TCObject lookup(ObjectID id) {
      throw new ImplementMe();
    }

    public ObjectID lookupExistingObjectID(Object obj) {
      throw new ImplementMe();
    }

    public TCObject lookupExistingOrNull(Object pojo) {
      throw new ImplementMe();
    }

    public TCObject lookupIfLocal(ObjectID id) {
      throw new ImplementMe();
    }

    public Object lookupObject(ObjectID id) {
      Object rv = ref;
      ref = null;
      return rv;
    }

    public Object lookupObject(ObjectID id, ObjectID parentContext) {
      throw new ImplementMe();
    }

    public Object lookupObjectNoDepth(ObjectID id) {
      throw new ImplementMe();
    }

    public TCObject lookupOrCreate(Object obj) {
      throw new ImplementMe();
    }

    public Object lookupOrCreateRoot(String name, Object obj) {
      throw new ImplementMe();
    }

    public Object lookupOrCreateRoot(String name, Object obj, boolean dsoFinal) {
      throw new ImplementMe();
    }

    public Object lookupOrCreateRootNoDepth(String rootName, Object object) {
      throw new ImplementMe();
    }

    public TCObject lookupOrShare(Object pojo) {
      throw new ImplementMe();
    }

    public Object lookupRoot(String name) {
      throw new ImplementMe();
    }

    public void markReferenced(TCObject tcobj) {
      throw new ImplementMe();
    }

    public void pause() {
      throw new ImplementMe();
    }

    public void replaceRootIDIfNecessary(String rootName, ObjectID newRootID) {
      throw new ImplementMe();
    }

    public void sendApplicationEvent(Object pojo, ApplicationEvent event) {
      throw new ImplementMe();
    }

    public void setTransactionManager(ClientTransactionManager txManager) {
      throw new ImplementMe();
    }

    public void shutdown() {
      throw new ImplementMe();
    }

    public void starting() {
      throw new ImplementMe();
    }

    public void storeObjectHierarchy(Object pojo, ApplicationEventContext context) {
      throw new ImplementMe();
    }

    public void unpause() {
      throw new ImplementMe();
    }

  }

}
