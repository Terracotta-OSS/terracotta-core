/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats;

import com.tc.logging.TCLogger;

public interface Stats {
  
  public String getDetails();
  
  public void logDetails(TCLogger statsLogger);

}
