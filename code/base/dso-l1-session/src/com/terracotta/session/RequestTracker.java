/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session;

import javax.servlet.http.HttpServletRequest;

interface RequestTracker {

  boolean end();

  void recordSessionId(TerracottaRequest terracottaRequest);

  void begin(HttpServletRequest req);

}