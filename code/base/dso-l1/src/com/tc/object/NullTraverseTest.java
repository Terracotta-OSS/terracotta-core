/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.object.appevent.NonPortableEventContext;

/**
 * @author steve
 */
public class NullTraverseTest implements TraverseTest {

  public boolean shouldTraverse(Object object) {
    return true;
  }

  public void checkPortability(TraversedReference reference, Class referringClass, NonPortableEventContext context)
      throws TCNonPortableObjectError {
    return;
  }

}