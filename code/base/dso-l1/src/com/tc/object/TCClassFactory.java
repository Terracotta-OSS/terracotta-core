/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object;

import com.tc.object.applicator.ChangeApplicator;

public interface TCClassFactory {

  public TCClass getOrCreate(Class clazz, ClientObjectManager objectManager);

  public ChangeApplicator createApplicatorFor(TCClass clazz, boolean indexed);

}
