/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
