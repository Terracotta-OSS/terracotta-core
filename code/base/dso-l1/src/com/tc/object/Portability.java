/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.util.NonPortableReason;

public interface Portability {

  public NonPortableReason getNonPortableReason(Class topLevelClass);

  public boolean isPortableClass(Class clazz);

  public boolean isClassPhysicallyInstrumented(Class clazz);

  public boolean isInstrumentationNotNeeded(String name);

  public boolean isPortableInstance(Object obj);

  public boolean overridesHashCode(Object obj);

  public boolean overridesHashCode(Class clazz);
}
