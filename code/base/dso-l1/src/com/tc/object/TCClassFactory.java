/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.object.applicator.ChangeApplicator;

public interface TCClassFactory {

  public TCClass getOrCreate(Class clazz, ClientObjectManager objectManager);

  public ChangeApplicator createApplicatorFor(TCClass clazz, boolean indexed);

}
