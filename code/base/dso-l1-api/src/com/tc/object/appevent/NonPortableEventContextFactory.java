/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.object.util.ReadOnlyException;

public class NonPortableEventContextFactory {

  private final ChannelIDProvider provider;

  public NonPortableEventContextFactory(ChannelIDProvider provider) {
    this.provider = provider;
  }

  private String getJVMId() {
    return "VM(" + provider.getChannelID().toLong() + ")";
  }

  public NonPortableEventContext createNonPortableEventContext(Object pojo) {
    return new NonPortableEventContext(pojo, Thread.currentThread().getName(), getJVMId());
  }

  public NonPortableRootContext createNonPortableRootContext(String rootName, Object rootValue) {
    return new NonPortableRootContext(Thread.currentThread().getName(), getJVMId(), rootName, rootValue);
  }

  public NonPortableFieldSetContext createNonPortableFieldSetContext(Object pojo, String fieldName, Object fieldValue) {
    return new NonPortableFieldSetContext(Thread.currentThread().getName(), getJVMId(), pojo, fieldName, fieldValue);
  }

  public NonPortableLogicalInvokeContext createNonPortableLogicalInvokeContext(Object pojo, String methodName, Object[] params, int index) {
    return new NonPortableLogicalInvokeContext(pojo, Thread.currentThread().getName(), getJVMId(),
                                               methodName, params, index);
  }

  public UnlockedSharedObjectEventContext createUnlockedSharedObjectEventContext(Object pojo, String classname, String fieldname, UnlockedSharedObjectException ex) {
    return new UnlockedSharedObjectEventContext(pojo, classname, fieldname, Thread.currentThread().getName(), getJVMId(), ex);
  }

  public UnlockedSharedObjectEventContext createUnlockedSharedObjectEventContext(Object pojo, UnlockedSharedObjectException ex) {
    return new UnlockedSharedObjectEventContext(pojo, Thread.currentThread().getName(), getJVMId(), ex);
  }

  public ReadOnlyObjectEventContext createReadOnlyObjectEventContext(Object pojo, ReadOnlyException ex) {
    return new ReadOnlyObjectEventContext(pojo, Thread.currentThread().getName(), getJVMId(), ex);
  }

  public ReadOnlyObjectEventContext createReadOnlyObjectEventContext(Object pojo, String classname, String fieldname, ReadOnlyException ex) {
    return new ReadOnlyObjectEventContext(pojo, classname, fieldname, Thread.currentThread().getName(), getJVMId(), ex);
  }
}
