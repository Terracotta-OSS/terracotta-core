/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object;

import com.tc.text.PrettyPrintable;

public interface ObjectRequestMonitor extends PrettyPrintable {
  public void notifyObjectRequest(ObjectRequestContext ctxt);
}
