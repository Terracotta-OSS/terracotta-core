/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.ui.IActionFilter;

class JavaElementActionFilter implements IActionFilter {
  public boolean testAttribute(Object target, String name, String value) {
    if(target instanceof IField ||
       target instanceof IMethod ||
       target instanceof IType)
    {
      return TcPlugin.getDefault().hasTerracottaNature((IJavaElement)target);
    }
    return true;
  }
}

