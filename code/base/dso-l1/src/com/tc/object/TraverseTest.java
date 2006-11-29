/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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