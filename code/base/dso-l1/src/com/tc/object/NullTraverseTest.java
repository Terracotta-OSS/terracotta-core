/*
 * Created on Sep 14, 2004
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