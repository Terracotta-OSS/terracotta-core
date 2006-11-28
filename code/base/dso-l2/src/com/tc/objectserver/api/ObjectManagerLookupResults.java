/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.api;

import java.util.Map;
import java.util.Set;

public interface ObjectManagerLookupResults {

  Map getObjects();

  Set getLookupPendingObjectIDs();

}
