/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tctest.runner.TransparentAppConfig;

import java.util.HashMap;
import java.util.Map;

public class RootClassChangeTest extends TransparentTestBase {
  public static final int MUTATOR_NODE_COUNT    = 2;
  public static final int ADAPTED_MUTATOR_COUNT = 1;

  public void doSetUp(TransparentTestIface t) throws Exception {
    TransparentAppConfig tac = t.getTransparentAppConfig();
    tac.setClientCount(MUTATOR_NODE_COUNT).setIntensity(1).setAdaptedMutatorCount(ADAPTED_MUTATOR_COUNT);

    Map adapterMap = new HashMap();
    adapterMap.put(RootClassChangeTestApp.DeepLargeObject.class.getName(), DeepLargeObjectAdapter.class);
    tac.setAttribute(TransparentAppConfig.adapterMapKey, adapterMap);

    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return RootClassChangeTestApp.class;
  }

  public static class DeepLargeObjectAdapter extends ClassAdapter {

    private String owner;

    public DeepLargeObjectAdapter(ClassVisitor cv) {
      super(cv);
    }

    public void visitEnd() {
      super.visitField(Opcodes.ACC_PRIVATE, "foo", "Lcom/tctest/RootClassChangeTestApp$FooObject;", null, null);
      super.visitEnd();
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      super.visit(version, access, name, signature, superName, interfaces);
      owner = name;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);
      if (name.equals("setFooObject")) {
        visitor.visitCode();
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitTypeInsn(Opcodes.NEW, "com/tctest/RootClassChangeTestApp$FooObject");
        visitor.visitInsn(Opcodes.DUP);
        visitor.visitLdcInsn("Yeah");
        visitor.visitInsn(Opcodes.ICONST_5);
        visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/tctest/RootClassChangeTestApp$FooObject", "<init>",
                                "(Ljava/lang/String;I)V");
        visitor.visitFieldInsn(Opcodes.PUTFIELD, owner, "foo", "Lcom/tctest/RootClassChangeTestApp$FooObject;");
        visitor.visitInsn(Opcodes.RETURN);
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
        return null;
      } else if (name.equals("getFooObject")) {
        visitor.visitCode();
        visitor.visitVarInsn(Opcodes.ALOAD, 0);
        visitor.visitFieldInsn(Opcodes.GETFIELD, owner, "foo", "Lcom/tctest/RootClassChangeTestApp$FooObject;");
        visitor.visitInsn(Opcodes.ARETURN);
        visitor.visitMaxs(0, 0);
        visitor.visitEnd();
        return null;
      }
      return visitor;
    }
  }
}
