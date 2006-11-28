/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.session;

public interface SessionSupport {

  void resumeRequest();

  void pauseRequest();

}
