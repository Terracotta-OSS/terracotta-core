/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.tree;

import org.eclipse.jdt.core.IJavaElement;

import java.util.Arrays;

public class JavaElementComparable implements Comparable {
  private IJavaElement m_element;
    
  JavaElementComparable(IJavaElement element) {
    m_element = element;
  }
    
  public IJavaElement element() {
    return m_element;
  }

  public int compareTo(Object o) {
    JavaElementComparable other     = (JavaElementComparable)o;
    IJavaElement          otherType = other.element();
    String                otherName = otherType.getElementName();

    return m_element.getElementName().compareTo(otherName);
  }

  public static IJavaElement[] sort(IJavaElement[] elements) {
    JavaElementComparable[] tmp = new JavaElementComparable[elements.length];
    
    for(int i = 0; i < elements.length; i++) {
      tmp[i] = new JavaElementComparable(elements[i]);
    }
    Arrays.sort(tmp);
    for(int i = 0; i < elements.length; i++) {
      elements[i] = tmp[i].element();
    }

    return elements;
  }
}
