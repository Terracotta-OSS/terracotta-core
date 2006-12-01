/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package jmx;

import java.util.ArrayList;

/**
 * Example of a Standard Bean using various data types
 */
public class SimpleStandard extends TCStandardBean implements SimpleStandardMBean {

  private static final String INIT_STATE  = "Initial State";
  
  private String              _state       = null;
  private ArrayList           _allStates   = new ArrayList();
  private int                 _changeCount = 0;

  public SimpleStandard() {
    setState(INIT_STATE);
  }
  /**
   *
   */

  public String getState() {
    return _state;
  }
  
  public String[] getAllStates() {
    String rv[] = new String[_allStates.size()];
    _allStates.toArray(rv);
    return rv;
  }

  /**
   *
   */

  public void setState(String s) {
    System.err.println("Setting state to: " + s);
    _state = s;
    _allStates.add(s);
    _changeCount++;
  }

  /**
   *
   */

  public Integer getNbChanges() {
    return new Integer(_changeCount);
  }
  
  public int getNbChangesInt() {
    return _changeCount;
  }

  /**
   *
   */

  public void reset() {
    _state = INIT_STATE;
    _changeCount = 0;
    _allStates.clear();
  }

}