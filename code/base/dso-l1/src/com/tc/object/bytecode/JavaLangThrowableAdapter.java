/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.object.Portability;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.util.Assert;

public class JavaLangThrowableAdapter extends TransparencyClassAdapter {

  public JavaLangThrowableAdapter(ClassInfo classInfo, TransparencyClassSpec spec, ClassVisitor cv,
                                  ManagerHelper mgrHelper, InstrumentationLogger instrumentationLogger,
                                  ClassLoader caller, Portability portability) {
    super(classInfo, spec, cv, mgrHelper, instrumentationLogger, caller, portability);
  }

  protected void basicVisit(final int version, final int access, final String name, String signature,
                            final String superClassName, final String[] interfaces) {
    Assert.assertEquals("java/lang/Throwable", name);
    super.basicVisit(version, access, name, signature, superClassName, interfaces);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (ClassAdapterBase.VALUES_GETTER.equals(name)) {
      // add a class to initialize the stacktrace element
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Throwable", "getOurStackTrace", "()[Ljava/lang/StackTraceElement;");
      mv.visitInsn(POP);
    }
    return mv;
  }
}
