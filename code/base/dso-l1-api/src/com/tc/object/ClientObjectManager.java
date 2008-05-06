/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.logging.DumpHandler;
import com.tc.object.appevent.ApplicationEvent;
import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.dna.api.DNA;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.object.util.ToggleableStrongReference;
import com.tc.text.PrettyPrintable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;

/**
 * Manages client-side (L1) object state in a VM.
 */
public interface ClientObjectManager extends DumpHandler, PrettyPrintable {

  /**
   * Find a class based on the class name and the classloader name
   *
   * @param className Class name
   * @param loaderDesc Classloader name
   * @return Class, never null
   * @throws ClassNotFoundException If class not found
   */
  public Class getClassFor(String className, String loaderDesc) throws ClassNotFoundException;

  /**
   * Determine whether this instance is managed.
   *
   * @param pojo The instance
   * @return True if managed
   */
  public boolean isManaged(Object pojo);

  /**
   * Mark a managed object as referenced
   *
   * @param tcobj Managed object
   */
  public void markReferenced(TCObject tcobj);

  /**
   * Determine whether this class is portable
   *
   * @param clazz The class to check
   * @return True if portable
   */
  public boolean isPortableClass(Class clazz);

  /**
   * Determine whether this instance is portable
   *
   * @param instance The instance to check
   * @return True if portable
   */
  public boolean isPortableInstance(Object instance);

  /**
   * Check whether field of an instance is portable
   *
   * @param value Field value
   * @param fieldName Field name
   * @param pojo Instance to check
   * @throws TCNonPortableObjectError If field is not portable
   */
  public void checkPortabilityOfField(Object value, String fieldName, Object pojo) throws TCNonPortableObjectError;

  /**
   * Check whether logical action is portable
   *
   * @param params Method call parameters
   * @param paramIndex Parameter index
   * @param methodName Method name
   * @param pojo Instance
   * @throws TCNonPortableObjectError If logical action is not portable
   */
  public void checkPortabilityOfLogicalAction(Object[] params, int paramIndex, String methodName, Object pojo)
      throws TCNonPortableObjectError;

  /**
   * Replace root ID. Primitive roots are replaceable. Object reference roots generally are not but this can be
   * controlled by the configuration.
   *
   * @param rootName Root object name
   * @param newRootID New root object identifier
   */
  public void replaceRootIDIfNecessary(String rootName, ObjectID newRootID);

  /**
   * Find object by ID. If necessary, the object will be faulted into the JVM. The default fault-count will be used to
   * limit the number of dependent objects that are also faulted in.
   *
   * @param id Identifier
   * @return Instance for the id
   * @throws ClassNotFoundException If class can't be found in this VM
   */
  public Object lookupObject(ObjectID id) throws ClassNotFoundException;

  /**
   * Look up object by ID, faulting into the JVM if necessary, This method also passes the parent Object context so that
   * more intelligent prefetching is possible at the L2. The default fault-count will be used to limit the number of
   * dependent objects that are also faulted in.
   *
   * @param id Object identifier of the object we are looking up
   * @param parentContext Object identifier of the parent object
   * @return The actual object
   * @throws TCClassNotFoundException If a class is not found during faulting
   */
  public Object lookupObject(ObjectID id, ObjectID parentContext) throws ClassNotFoundException;

  /**
   * Find object by ID. If necessary, the object will be faulted into the JVM. No fault-count depth will be used and all
   * dependent objects will be faulted into memory.
   *
   * @param id Identifier
   * @return Instance for the id
   * @throws ClassNotFoundException If class can't be found in this VM
   */
  public Object lookupObjectNoDepth(ObjectID id) throws ClassNotFoundException;

  /**
   * Find the managed object for this instance or create a new one if it does not yet exist.
   *
   * @param obj Instance
   * @return Managed object, may be new. Should never be null, but might be object representing null TCObject.
   */
  public TCObject lookupOrCreate(Object obj);

  /**
   * Find the managed object for this instance or share. This method is (exclusively?) used when implementing
   * ConcurrentHashMap sharing.
   *
   * @param obj Instance
   * @return Should never be null, but might be object representing null TCObject.
   */
  public TCObject lookupOrShare(Object pojo);

  /**
   * Find identifier for existing instance
   *
   * @param obj Object instance
   * @return Identifier
   */
  public ObjectID lookupExistingObjectID(Object obj);

  /**
   * Find named root object
   *
   * @param name Root name
   * @return Root object
   */
  public Object lookupRoot(String name);

  /**
   * Find and create if necessary a root object for the specified named root. All dependent objects needed will be
   * faulted in to arbitrary depth.
   *
   * @param rootName Root name
   * @param object Instance to use if new
   * @return New or existing object to use as root
   */
  public Object lookupOrCreateRootNoDepth(String rootName, Object object);

  /**
   * Find and create if necessary a root object for the specified named root. All dependent objects needed will be
   * faulted in, limited to the fault-count specified in the configuration.
   *
   * @param name Root name
   * @param obj Instance to use if new
   * @return New or existing object to use as root
   */
  public Object lookupOrCreateRoot(String name, Object obj);

  /**
   * Find and create if necessary a root object for the specified named root. All dependent objects needed will be
   * faulted in, limited to the fault-count specified in the configuration.
   *
   * @param name Root name
   * @param obj Instance to use if new
   * @param dsoFinal Specify whether this is root is considered final and whether an existing root can be replaced
   * @return New or existing object to use as root
   */
  public Object lookupOrCreateRoot(String name, Object obj, boolean dsoFinal);

  /**
   * Find managed object locally (don't fault in an object from the server).
   *
   * @param id Identifier
   * @return Managed object or null if not in client
   */
  public TCObject lookupIfLocal(ObjectID id);

  /**
   * Find managed object by identifier
   *
   * @param id Identifier
   * @return Managed object
   * @throws ClassNotFoundException If a class needed to hydrate cannot be found
   */
  public TCObject lookup(ObjectID id) throws ClassNotFoundException;

  /**
   * Find managed object by instance, which may be null
   *
   * @param pojo Instance
   * @return Managed object if it exists, or null otherwise
   */
  public TCObject lookupExistingOrNull(Object pojo);

  /**
   * Get all IDs currently in the cache and add to c. Clear all from remote object manager.
   *
   * @param c Collection to collect IDs in
   * @return c
   */
  public Collection getAllObjectIDsAndClear(Collection c);

  /**
   * Create new peer object instance for the clazz, referred to through a WeakReference.
   *
   * @param clazz The kind of class
   * @param dna The dna defining the object instance
   * @return Weak reference referring to the peer
   */
  public WeakReference createNewPeer(TCClass clazz, DNA dna);

  // public WeakObjectReference createNewPeer(TCClass clazz, DNA dna);

  /**
   * Create new peer object instance for the clazz, referred to through a WeakReference.
   *
   * @param clazz The kind of class
   * @param size The size if this is an array
   * @param id The object identifier
   * @param parentID The parent object, if this is an inner object
   * @return Weak reference referring to the peer
   */
  public WeakReference createNewPeer(TCClass clazz, int size, ObjectID id, ObjectID parentID);

  // public WeakObjectReference createNewPeer(TCClass clazz, int size, ObjectID id, ObjectID parentID);

  /**
   * Get or create a reference to the managed class for this clazz
   *
   * @param clazz The Java class
   * @return The Terracotta class
   */
  public TCClass getOrCreateClass(Class clazz);

  /**
   * Set the client transaction manager
   *
   * @param txManager Transaction manager
   */
  public void setTransactionManager(ClientTransactionManager txManager);

  /**
   * Get the client transaction manager
   *
   * @return Transaction manager
   */
  public ClientTransactionManager getTransactionManager();

  /**
   * Get the reference queue for weakly referenced peers
   *
   * @return Reference queue
   */
  public ReferenceQueue getReferenceQueue();

  /**
   * Shutdown the client object manager
   */
  public void shutdown();

  /**
   * Unpause, moving state to running
   */
  public void unpause();

  /**
   * Pause client object manager, for use while starting
   */
  public void pause();

  /**
   * Change to STARTING state
   */
  public void starting();

  /**
   * Do deep copy of source object using the transaction manager
   *
   * @param source Source object
   * @param optimisticTxManager Transaction manager to use
   * @return Deep copy of source
   */
  public Object deepCopy(Object source, OptimisticTransactionManager optimisticTxManager);

  /**
   * Take a source and a parent (if non-static inner) and create a new empty instance
   *
   * @param source Source object
   * @param parent Parent object
   * @return New copy instance of source
   */
  public Object createNewCopyInstance(Object source, Object parent);

  /**
   * For an inner object, create or find the containing parent instance.
   *
   * @param visited Map of those objects that have been visited so far
   * @param cloned Map of those objects that have been cloned already
   * @param v The object
   * @return The new or existing parent object clone
   */
  public Object createParentCopyInstanceIfNecessary(Map visited, Map cloned, Object v);

  /**
   * @return True if creation in progress
   */
  public boolean isCreationInProgress();

  /**
   * Add all pending create object actions (created during traversals) to the current transaction.
   */
  public void addPendingCreateObjectsToTransaction();

  /**
   * Check whether there are any currently pending create objects
   *
   * @return True if any pending
   */
  public boolean hasPendingCreateObjects();

  /**
   * Create or replace a root value, typically used for replacable roots.
   *
   * @param rootName Root name
   * @param root New root value
   */
  public Object createOrReplaceRoot(String rootName, Object root);

  // // The following are in support of the Eclipse ApplicationEventDialog and the Session Configurator.

  /**
   * Store the pojo object hierarchy in the context's tree model.
   *
   * @param pojo The object
   * @param context The event context
   */
  void storeObjectHierarchy(Object pojo, ApplicationEventContext context);

  /**
   * Send an ApplicationEvent occurring on pojo to the server via JMX. The handling of concrete event types occurs in
   * com.tc.objectserver.DSOApplicationEvents.
   *
   * @param pojo The object
   * @param event The event
   */
  void sendApplicationEvent(Object pojo, ApplicationEvent event);

  /**
   * Clone logicalPojo and then apply the specified logical operation, returning the clone.
   *
   * @param logicalPojo The logical object
   * @param methodName The method name on the logical object
   * @param parameters The parameter values
   * @return The cloned object
   */
  Object cloneAndInvokeLogicalOperation(Object logicalPojo, String methodName, Object[] parameters);

  /**
   * Get or create the toggle reference for the given TCObject
   *
   * @param objectID The TCObjet
   * @param peer The peer object
   * @return the toggle reference
   */
  ToggleableStrongReference getOrCreateToggleRef(ObjectID objectID, Object peer);

}
