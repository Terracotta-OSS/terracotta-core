/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.ui.IActionFilter;

class JavaElementActionFilter implements IActionFilter {
  public boolean testAttribute(Object target, String name, String value) {
    if(target instanceof IField || target instanceof IMethod || target instanceof IType) {
      IProject project = ((IJavaElement)target).getJavaProject().getProject();
      
      try {
        return project.hasNature(ProjectNature.NATURE_ID);
      } catch(Exception e) {
        return false;
      }
    }
    return true;
  }
}

