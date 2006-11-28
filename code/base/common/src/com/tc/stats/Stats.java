/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats;

import com.tc.logging.TCLogger;

public interface Stats {
  
  public String getDetails();
  
  public void logDetails(TCLogger statsLogger);

}
