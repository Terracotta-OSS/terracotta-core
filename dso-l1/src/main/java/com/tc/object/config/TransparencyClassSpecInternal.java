/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import com.tc.object.bytecode.ClassAdapterFactory;

import java.util.Collection;

public interface TransparencyClassSpecInternal extends TransparencyClassSpec {

  /**
   * Add a custom class adapter factory to be executed after DSO adapters
   * <p/>
   * They will later be processed according to their order of registration and delegate control to the earlier ones
   * through the standard ASM visitor pattern. This means that any instrumentation that's done in the first class
   * adapter will be seen by the second class adapter, and so on.
   * 
   * @param customClassAdapter Custom factory
   */
  public void addAfterDSOClassAdapter(ClassAdapterFactory customClassAdapter);

  /**
   * Get the custom class adapter factories to be executed after DSO adapters. The returned list is in the reverse order
   * of addition. The first class adapter factory that was added will be the last one in the list.
   * 
   * @return Adapter factories
   */
  public Collection<ClassAdapterFactory> getAfterDSOClassAdapters();

}
