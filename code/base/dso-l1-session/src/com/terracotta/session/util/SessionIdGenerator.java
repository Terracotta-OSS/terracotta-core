/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.SessionId;

public interface SessionIdGenerator {

  SessionId generateNewId();

  SessionId makeInstanceFromBrowserId(String requestedSessionId);

  SessionId makeInstanceFromInternalKey(String key);

}
