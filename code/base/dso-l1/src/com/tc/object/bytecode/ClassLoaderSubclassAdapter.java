/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;

import com.tc.object.bytecode.hook.ClassLoaderPreProcessorImpl.LoadClassAdapter;

public class ClassLoaderSubclassAdapter extends ClassAdapter {

  public ClassLoaderSubclassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("loadClass".equals(name) && "(Ljava/lang/String;)Ljava/lang/Class;".equals(desc)) {
      return new LoadClassAdapter(mv, access, name, desc);
    } else {
      return mv;
    }
  }
}
