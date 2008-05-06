/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.appevent.NonPortableEventContext;

/**
 * @author steve
 */
public interface TraverseTest {
  public boolean shouldTraverse(Object object);

  public void checkPortability(TraversedReference obj, Class referringClass, NonPortableEventContext context)
      throws TCNonPortableObjectError;
}