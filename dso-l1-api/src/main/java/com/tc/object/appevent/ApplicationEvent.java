/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.appevent;

import java.io.Serializable;

/**
 * An event detailing an abnormal application condition, such as an attempt to share a non-portable object or an
 * unlocked shared object modification.
 */
public interface ApplicationEvent extends Serializable {
  /**
   * @return The context in which this event occurred
   */
  ApplicationEventContext getApplicationEventContext();

  /**
   * @return The event message
   */
  String getMessage();
}
