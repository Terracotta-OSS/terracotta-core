/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.injection;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.commons.AdviceAdapter;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

public class DsoClusterInjectionInstrumentation implements InjectionInstrumentation {

  public ClassAdapterFactory getClassAdapterFactoryForFieldInjection(final FieldInfo fieldToInjectInto) {
    return new Factory(fieldToInjectInto);
  }

  private final static class Factory implements ClassAdapterFactory {
    private final FieldInfo fieldToInjectInto;

    private Factory(final FieldInfo fieldToInjectInto) {
      this.fieldToInjectInto = fieldToInjectInto;
    }

    public ClassAdapter create(final ClassVisitor visitor, final ClassLoader loader) {
      return new Adapter(visitor, loader);
    }

    private final class Adapter extends ClassAdapter implements Opcodes {

      private Adapter(final ClassVisitor cv, final ClassLoader caller) {
        super(cv);
      }

      @Override
      public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        if ("<init>".equals(name)) {
          return new DsoClusterConstructorInjection(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc);
        }

        return super.visitMethod(access, name, desc, signature, exceptions);
      }

      private final class DsoClusterConstructorInjection extends AdviceAdapter {
        @Override
        protected void onMethodEnter() {
          mv.visitVarInsn(ALOAD, 0);
          mv.visitFieldInsn(GETFIELD, ByteCodeUtil.classNameToInternalName(fieldToInjectInto.getDeclaringType().getName()),
                            fieldToInjectInto.getName(), 'L'+ByteCodeUtil.classNameToInternalName(fieldToInjectInto.getType().getName())+';');

          Label labelFieldAlreadyInjected = new Label();
          mv.visitJumpInsn(IFNONNULL, labelFieldAlreadyInjected);
          mv.visitVarInsn(ALOAD, 0);
          mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "getManager", "()Lcom/tc/object/bytecode/Manager;");
          mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/bytecode/Manager", "getDsoCluster", "()Lcom/tc/cluster/DsoCluster;");
          mv.visitFieldInsn(PUTFIELD, ByteCodeUtil.classNameToInternalName(fieldToInjectInto.getDeclaringType().getName()),
                            fieldToInjectInto.getName(), 'L'+ByteCodeUtil.classNameToInternalName(fieldToInjectInto.getType().getName())+';');
          mv.visitLabel(labelFieldAlreadyInjected);
        }

        public DsoClusterConstructorInjection(final MethodVisitor mv, final int access, final String name, final String desc) {
          super(mv, access, name, desc);
        }

      }
    }
  }
}