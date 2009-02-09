/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.injection;

import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.object.bytecode.ClassAdapterFactory;

public interface InjectionInstrumentation {
  public ClassAdapterFactory getClassAdapterFactoryForFieldInjection(FieldInfo fieldToInjectInto);
}