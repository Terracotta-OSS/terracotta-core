/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.injection;

import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.object.bytecode.ClassAdapterFactory;

/**
 * Provides a {@code ClassAdapterFactory} that performs instrumentation for injection. This class adapter should inject
 * the instances as the first operation in the constructor since injected fields can only be assigned a value once.
 */
public interface InjectionInstrumentation {
  public ClassAdapterFactory getClassAdapterFactoryForFieldInjection(FieldInfo fieldToInjectInto);
}