/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.Session;

public interface LifecycleEventMgr {

  void fireSessionCreatedEvent(Session s);

  void fireSessionDestroyedEvent(Session s);

  void unbindAttribute(Session sess, String name, Object val);

  void bindAttribute(Session sess, String name, Object val);

  void setAttribute(Session sess, String name, Object val);
  
  void removeAttribute(Session sess, String name, Object val);
  
  void replaceAttribute(Session sess, String name, Object oldVal, Object newVal);
}
