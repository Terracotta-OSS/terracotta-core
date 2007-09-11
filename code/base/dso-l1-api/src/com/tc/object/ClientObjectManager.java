/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.appevent.ApplicationEvent;
import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.dna.api.DNA;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;

/**
 * @author steve
 */
public interface ClientObjectManager {

  public Class getClassFor(String className, String loaderDesc) throws ClassNotFoundException;

  public boolean isManaged(Object pojo);

  public void markReferenced(TCObject tcobj);

  public boolean isPortableClass(Class clazz);

  public boolean isPortableInstance(Object instance);

  public void checkPortabilityOfField(Object value, String fieldName, Object pojo) throws TCNonPortableObjectError;

  public void checkPortabilityOfLogicalAction(Object[] params, int paramIndex, String methodName, Object pojo)
      throws TCNonPortableObjectError;

  public void replaceRootIDIfNecessary(String rootName, ObjectID newRootID);

  public Object lookupObject(ObjectID id) throws ClassNotFoundException;

  public Object lookupObjectNoDepth(ObjectID id) throws ClassNotFoundException;

  public TCObject lookupOrCreate(Object obj);

  public TCObject lookupOrShare(Object pojo);

  public ObjectID lookupExistingObjectID(Object obj);

  public Object lookupRoot(String name);

  public Object lookupOrCreateRootNoDepth(String rootName, Object object);

  public Object lookupOrCreateRoot(String name, Object obj);

  public Object lookupOrCreateRoot(String name, Object obj, boolean dsoFinal);

  public TCObject lookupIfLocal(ObjectID id);

  public TCObject lookup(ObjectID id) throws ClassNotFoundException;

  public TCObject lookupExistingOrNull(Object pojo);

  public Collection getAllObjectIDsAndClear(Collection c);

  //public WeakObjectReference createNewPeer(TCClass clazz, DNA dna);
  public WeakReference createNewPeer(TCClass clazz, DNA dna);

  //public WeakObjectReference createNewPeer(TCClass clazz, int size, ObjectID id, ObjectID parentID);
  public WeakReference createNewPeer(TCClass clazz, int size, ObjectID id, ObjectID parentID);

  public TCClass getOrCreateClass(Class clazz);

  public void setTransactionManager(ClientTransactionManager txManager);

  public ClientTransactionManager getTransactionManager();

  public ReferenceQueue getReferenceQueue();

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

  /**
   * The following are in support of the Eclipse ApplicationEventDialog and the Session Configurator.
   */

  /**
   * Store the pojo object hierarchy in the context's tree model.
   */
  void storeObjectHierarchy(Object pojo, ApplicationEventContext context);

  /**
   * Send an ApplicationEvent occurring on pojo to the server via JMX.
   * The handling of concrete event types occurs in com.tc.objectserver.DSOApplicationEvents.
   */
  void sendApplicationEvent(Object pojo, ApplicationEvent event);

  /**
   * Clone logicalPojo and then apply the specified logical operation, returning the clone.
   */
  Object cloneAndInvokeLogicalOperation(Object logicalPojo, String methodName, Object[] parameters);
}
