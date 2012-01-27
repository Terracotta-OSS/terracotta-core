/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.appevent;

import java.io.Serializable;

/**
 * Event context information
 */
public interface ApplicationEventContext extends Serializable {

  /**
   * @return Object that the event is related to
   */
  Object getPojo();

  /**
   * Optional - specify Eclipse project
   * 
   * @return Eclipse project name
   */
  String getProjectName();
}
