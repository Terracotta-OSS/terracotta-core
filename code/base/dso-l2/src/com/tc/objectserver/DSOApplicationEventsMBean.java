/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver;

import com.tc.management.TerracottaMBean;

public interface DSOApplicationEventsMBean extends TerracottaMBean {

  static final String NON_PORTABLE_OBJECT_EVENT    = "non-portable error";
  static final String UNLOCKED_SHARED_OBJECT_EVENT = "unlocked shared object error";
  static final String READ_ONLY_OBJECT_EVENT = "read-only shared object error";

}
