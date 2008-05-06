/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

public interface DSOObjectVisitor {
  public void visitDSOField(DSOField field);
  public void visitDSORoot(DSORoot root);
  public void visitDSOMapEntryField(DSOMapEntryField mapEntryField);
}
