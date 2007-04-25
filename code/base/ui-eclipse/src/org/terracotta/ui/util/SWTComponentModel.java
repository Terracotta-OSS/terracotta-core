/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.util;



public interface SWTComponentModel {

  /**
   * Initializes data for this model - creates a state object which stores data and references as member fields - no
   * accessor methods).
   */
  void init(Object data);

  /**
   * Clears state information. This will set both <tt>isInit()</tt> and <tt>isActive()</tt> to <tt>false</tt>
   */
  void clearState();

  /**
   * Deactivates registered action listeners
   */
  void setActive(boolean activate);

  boolean isActive();
}
