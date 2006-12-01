/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats;

import com.tc.logging.TCLogger;

public interface Stats {
  
  public String getDetails();
  
  public void logDetails(TCLogger statsLogger);

}
