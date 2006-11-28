/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.admin.dso;

public interface DSOObjectVisitor {
  public void visitDSOField(DSOField field);
  public void visitDSORoot(DSORoot root);
  public void visitDSOMapEntryField(DSOMapEntryField mapEntryField);
}
