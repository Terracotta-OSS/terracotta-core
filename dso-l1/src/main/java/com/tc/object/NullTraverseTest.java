/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCNonPortableObjectError;

/**
 * @author steve
 */
public class NullTraverseTest implements TraverseTest {

  @Override
  public boolean shouldTraverse(Object object) {
    return true;
  }

  @Override
  public void checkPortability(TraversedReference reference, Class referringClass)
      throws TCNonPortableObjectError {
    return;
  }

}