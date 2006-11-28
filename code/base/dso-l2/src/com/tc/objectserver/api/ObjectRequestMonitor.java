/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.objectserver.context.ManagedObjectRequestContext;
import com.tc.text.PrettyPrintable;

public interface ObjectRequestMonitor extends PrettyPrintable {
  public void notifyObjectRequest(ManagedObjectRequestContext ctxt);
}
