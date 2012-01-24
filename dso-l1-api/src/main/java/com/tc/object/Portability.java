/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
