/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object;

import com.tc.util.NonPortableReason;

public interface Portability {

  public NonPortableReason getNonPortableReason(Class topLevelClass);

  public boolean isPortableClass(Class clazz);

  public boolean isClassPhysicallyInstrumented(Class clazz);

  public boolean isInstrumentationNotNeeded(String name);

  public boolean isPortableInstance(Object obj);
}
