/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.util;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.IRegion;

public class StackElementInfo {
  private StackTraceElement fStackElement;
  private IRegion           fRegion;
  private IType             fType;
  private IMethod           fMethod;

  public StackElementInfo(StackTraceElement stackElement) {
    fStackElement = stackElement;
  }

  public StackTraceElement getStackElement() {
    return fStackElement;
  }

  public IRegion getRegion() {
    return fRegion;
  }

  public void setRegion(IRegion region) {
    fRegion = region;
  }

  public IType getType() {
    return fType;
  }

  public void setType(IType type) {
    fType = type;
  }

  public IMethod getMethod() {
    return fMethod;
  }

  public void setMethod(IMethod method) {
    fMethod = method;
  }

  public String toString() {
    return fStackElement.toString();
  }
}
