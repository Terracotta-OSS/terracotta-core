/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

import java.util.Map;
import java.util.Set;

public interface ObjectManagerLookupResults {

  Map getObjects();

  Set getLookupPendingObjectIDs();

}
