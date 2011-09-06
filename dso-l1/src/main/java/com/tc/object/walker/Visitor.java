/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

public interface Visitor {

  void visitValue(MemberValue value, int depth);

  void visitRootObject(MemberValue value);

  void visitMapEntry(int index, int depth);

}