/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.io.Serializable;

public class State implements Serializable {
  private final String name;

  public State(String name) {
    Assert.assertNotNull(name);
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public boolean equals(Object o) {
    if (!(o instanceof State)) { return false; }
    return name.equals(((State) o).name);
  }

  public int hashCode() {
    return name.hashCode();
  }

  public String toString() {
    return "State[ " + this.name + " ]";
  }
}