/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;

public class MergeTCObjectSelfToTargetAdapter extends MergeSourceToTargetAdapter {

  public static final String TC_OBJECT_SELF_CLASS_NAME_DOTS = "com.tc.object.TCObjectSelfImpl";

  public MergeTCObjectSelfToTargetAdapter(ClassVisitor cv, String targetClassNameDots) {
    super(cv, targetClassNameDots, TC_OBJECT_SELF_CLASS_NAME_DOTS);
  }

}
