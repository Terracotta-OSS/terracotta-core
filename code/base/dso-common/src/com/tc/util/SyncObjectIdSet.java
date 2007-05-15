/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.Set;

public interface SyncObjectIdSet extends Set {
  public void startPopulating();

  public void stopPopulating(ObjectIDSet2 fullSet);

  public ObjectIDSet2 snapshot();
}
