/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.dna.api.DNA;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;

import java.lang.ref.ReferenceQueue;
import java.util.Collection;
import java.util.Map;

/**
 * @author steve
 */
public interface ClientObjectManager {

  public boolean addTraverseTest(TraverseTest additionalTest);

  public Class getClassFor(String className, String loaderDesc) throws ClassNotFoundException;

  public boolean isManaged(Object pojo);

  public void markReferenced(TCObject tcobj);

  public boolean isPortableClass(Class clazz);

  public boolean isPortableInstance(Object instance);

  public void checkPortabilityOfField(Object value, String fieldName, Class targetClass)
      throws TCNonPortableObjectError;

  public void checkPortabilityOfLogicalAction(Object param, String methodName, Class logicalType)
      throws TCNonPortableObjectError;

  public void replaceRootIDIfNecessary(String rootName, ObjectID newRootID);

  public Object lookupObject(ObjectID id);

  public Object lookupObjectNoDepth(ObjectID id);

  public TCObject lookupOrCreate(Object obj);

  public TCObject lookupOrShare(Object pojo);

  public ObjectID lookupExistingObjectID(Object obj);

  public Object lookupRoot(String name);

  public Object lookupOrCreateRootNoDepth(String rootName, Object object);

  public Object lookupOrCreateRoot(String name, Object obj);

  public Object lookupOrCreateRoot(String name, Object obj, boolean dsoFinal);

  public TCObject lookupIfLocal(ObjectID id);

  public TCObject lookup(ObjectID id);

  public TCObject lookupExistingOrNull(Object pojo);

  public Collection getAllObjectIDsAndClear(Collection c);

  public WeakObjectReference createNewPeer(TCClass clazz, DNA dna);

  public WeakObjectReference createNewPeer(TCClass clazz, int size, ObjectID id, ObjectID parentID);

  public TCClass getOrCreateClass(Class clazz);

  public void setTransactionManager(ClientTransactionManager txManager);

  public ClientTransactionManager getTransactionManager();

  public ReferenceQueue getReferenceQueue();

  public boolean enableDistributedMethods();

  public void shutdown();

  public void unpause();

  public void pause();

  public void starting();

  public Object deepCopy(Object source, OptimisticTransactionManager optimisticTxManager);

  /**
   * take a source and a parent (if non-static inner) and create a new empty instance
   */
  public Object createNewCopyInstance(Object source, Object parent);

  public Object createParentCopyInstanceIfNecessary(Map visited, Map cloned, Object v);

  public boolean isCreationInProgress();

  public void addPendingCreateObjectsToTransaction();

  public boolean hasPendingCreateObjects();
  
  public Object createOrReplaceRoot(String rootName, Object root);
}
