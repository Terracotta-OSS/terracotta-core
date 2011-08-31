/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.model.IBasicObject;
import com.tc.admin.model.IMapEntry;

public interface DSOObjectVisitor {
  public void visitBasicObject(IBasicObject basicObject);

  public void visitMapEntry(IMapEntry mapEntry);
}
