/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.appevent;

import com.tc.net.protocol.tcm.ChannelIDProvider;

public class NonPortableEventContextFactory {

  private final ChannelIDProvider provider;

  public NonPortableEventContextFactory(ChannelIDProvider provider) {
    this.provider = provider;
  }

  private String getJVMId() {
    return "VM(" + provider.getChannelID().toLong() + ")";
  }

  public NonPortableEventContext createNonPortableEventContext(String targetClassName) {
    return new NonPortableEventContext(targetClassName, Thread.currentThread().getName(), getJVMId());
  }

  public NonPortableFieldSetContext createNonPortableFieldSetContext(String targetClassName, String fieldName,
                                                                     boolean isRoot) {
    return new NonPortableFieldSetContext(targetClassName, Thread.currentThread().getName(), getJVMId(), fieldName,
                                          isRoot);
  }

  public NonPortableLogicalInvokeContext createNonPortableLogicalInvokeContext(String targetClassName, String methodName) {
    return new NonPortableLogicalInvokeContext(targetClassName, Thread.currentThread().getName(), getJVMId(),
                                               methodName);
  }

}
