/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.object.applicator.ChangeApplicator;
import com.tc.platform.PlatformService;

public interface TCClassFactory {

  public static final String SERVER_MAP_CLASSNAME = "com.terracotta.toolkit.collections.map.ServerMap";

  public TCClass getOrCreate(Class clazz, ClientObjectManager objectManager);

  public ChangeApplicator createApplicatorFor(TCClass clazz);

  public void setPlatformService(PlatformService platformService);

}
