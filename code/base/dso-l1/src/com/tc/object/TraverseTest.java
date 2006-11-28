/*
 * Created on Sep 14, 2004 TODO To change the template for this generated file go to Window - Preferences - Java - Code
 * Style - Code Templates
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