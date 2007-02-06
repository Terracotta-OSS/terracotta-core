/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import java.util.Arrays;

/**
 * Represents the <code>application-contexts</code> config element hierarchy
 */
public class AppContext {

  private final String[]            paths;
  private final String[]            distributedEvents;
  private final SpringContextBean[] beans;
  private final String              rootName;
  private final boolean             locationInfoEnabled;

  public AppContext(String[] paths, String[] distributedEvents, SpringContextBean[] beans, String rootName,
                    boolean locationInfoEnabled) {
    this.paths = paths;
    this.distributedEvents = distributedEvents;
    this.beans = beans;
    this.rootName = rootName;
    this.locationInfoEnabled = locationInfoEnabled;
  }

  public String[] paths() {
    return paths;
  }

  public String[] distributedEvents() {
    return distributedEvents;
  }

  public SpringContextBean[] beans() {
    return beans;
  }

  public String rootName() {
    return rootName;
  }
  
  public boolean locationInfoEnabled() {
    return locationInfoEnabled;
  }
  
  public String toString() {
    return "APP-CONTEXT: \nDIST-EVENTS: " + Arrays.asList(distributedEvents) + "\nPATHS\n\n" + Arrays.asList(paths)
        + "\n" + Arrays.asList(beans);
  }
}
