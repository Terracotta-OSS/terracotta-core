/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.object.ClientIDProvider;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.object.util.ReadOnlyException;

/**
 * A factory for creating the proper {@link NonPortableEventContext}
 */
public class NonPortableEventContextFactory {

  private final ClientIDProvider provider;

  /**
   * Construct the factory with the provider of a ChannelID
   * 
   * @param provider2 The provider
   */
  public NonPortableEventContextFactory(ClientIDProvider provider) {
    this.provider = provider;
  }

  private String getJVMId() {
    return "VM(" + provider.getClientID().toLong() + ")";
  }

  /**
   * Create a context for when a non-portable object is found while traversing an object graph
   * 
   * @param pojo The non portable object
   * @return The context
   */
  public NonPortableEventContext createNonPortableEventContext(Object pojo) {
    return new NonPortableEventContext(pojo, Thread.currentThread().getName(), getJVMId());
  }

  /**
   * Create a context for when a non-portable object is set as a root
   * 
   * @param rootName The root name
   * @param rootValue The non-portable value being set
   * @return The context
   */
  public NonPortableRootContext createNonPortableRootContext(String rootName, Object rootValue) {
    return new NonPortableRootContext(Thread.currentThread().getName(), getJVMId(), rootName, rootValue);
  }

  /**
   * Create a context for when a non-portable object is set on a field
   * 
   * @param pojo The object that is having the field set
   * @param fieldName The field of the pojo
   * @param fieldValue The non-portable value being set
   * @return The context
   */
  public NonPortableFieldSetContext createNonPortableFieldSetContext(Object pojo, String fieldName, Object fieldValue) {
    return new NonPortableFieldSetContext(Thread.currentThread().getName(), getJVMId(), pojo, fieldName, fieldValue);
  }

  /**
   * Create a context for an invocation of a logical method with a non-portable object
   * 
   * @param pojo The logical object
   * @param methodName The method being called on the pojo
   * @param params The parameters passed to the method
   * @param index The index into the params of the non-portable object
   * @return The context
   */
  public NonPortableLogicalInvokeContext createNonPortableLogicalInvokeContext(Object pojo, String methodName,
                                                                               Object[] params, int index) {
    return new NonPortableLogicalInvokeContext(pojo, Thread.currentThread().getName(), getJVMId(), methodName, params,
                                               index);
  }

  /**
   * Create a context for when there is an attempt to access a shared object outside the scope of a shared lock.
   * 
   * @param pojo The object being accessed
   * @param classname The class of the object
   * @param fieldname The field of the object being accessed
   * @param ex The exception thrown
   * @return The context
   */
  public UnlockedSharedObjectEventContext createUnlockedSharedObjectEventContext(Object pojo, String classname,
                                                                                 String fieldname,
                                                                                 UnlockedSharedObjectException ex) {
    return new UnlockedSharedObjectEventContext(pojo, classname, fieldname, Thread.currentThread().getName(),
                                                getJVMId(), ex);
  }

  /**
   * Create a context for when there is an attempt to access a shared object outside the scope of a shared lock.
   * 
   * @param pojo The object being accessed
   * @param ex The exception thrown
   * @return The context
   */
  public UnlockedSharedObjectEventContext createUnlockedSharedObjectEventContext(Object pojo,
                                                                                 UnlockedSharedObjectException ex) {
    return new UnlockedSharedObjectEventContext(pojo, Thread.currentThread().getName(), getJVMId(), ex);
  }

  /**
   * Create a context for when a transaction with read-only access is attempting to modify a shared object.
   * 
   * @param pojo The object being accessed
   * @param ex The exception that occurred
   * @return The context
   */
  public ReadOnlyObjectEventContext createReadOnlyObjectEventContext(Object pojo, ReadOnlyException ex) {
    return new ReadOnlyObjectEventContext(pojo, Thread.currentThread().getName(), getJVMId(), ex);
  }

  /**
   * Create a context for when a transaction with read-only access is attempting to modify a shared object.
   * 
   * @param pojo The object being accessed
   * @param classname The class of the pojo
   * @param fieldname The field name of the object
   * @param ex The exception that occurred
   * @return The context
   */
  public ReadOnlyObjectEventContext createReadOnlyObjectEventContext(Object pojo, String classname, String fieldname,
                                                                     ReadOnlyException ex) {
    return new ReadOnlyObjectEventContext(pojo, classname, fieldname, Thread.currentThread().getName(), getJVMId(), ex);
  }
}
