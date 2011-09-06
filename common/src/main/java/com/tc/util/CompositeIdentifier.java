/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.Arrays;

/**
 * Use me to build an identifier composed of 1 or more AbstactIdentifier instances
 */
public class CompositeIdentifier {

  private final AbstractIdentifier[] components;
  private final int                  hashCode;

  public CompositeIdentifier(AbstractIdentifier[] components) {
    Assert.assertNoNullElements(components);
    this.components = components;
    this.hashCode = makeHashCode(components);
  }

  private int makeHashCode(AbstractIdentifier[] ids) {
    int rv = 17;
    for (int i = 0; i < ids.length; i++) {
      rv += (37 * rv) + ids[i].hashCode();
    }
    return rv;
  }

  public boolean contains(AbstractIdentifier id) {
    for (int i = 0; i < components.length; i++) {
      if (components[i].equals(id)) {
        return true;
      }
    }
    return false;
  }

  public int hashCode() {
    return this.hashCode;
  }

  public boolean equals(Object obj) {
    if (obj instanceof CompositeIdentifier) {
      CompositeIdentifier other = (CompositeIdentifier) obj;
      return Arrays.equals(this.components, other.components);
    }

    return false;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < components.length; i++) {
      buf.append(components[i].toString());
      if (i != components.length - 1) {
        buf.append(',');
      }
    }

    return buf.toString();
  }

  public AbstractIdentifier[] getComponents() {
    return components;
  }
}
