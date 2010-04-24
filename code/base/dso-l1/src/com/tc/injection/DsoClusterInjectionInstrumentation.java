/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.injection;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.commons.AdviceAdapter;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

public class DsoClusterInjectionInstrumentation implements InjectionInstrumentation {

  public final static String TC_INJECTION_METHOD_NAME = ByteCodeUtil.TC_METHOD_PREFIX + "injection";

  public ClassAdapterFactory getClassAdapterFactoryForFieldInjection(final FieldInfo fieldToInjectInto) {
    return new Factory(fieldToInjectInto);
  }

  private final static class Factory implements ClassAdapterFactory {
    private final FieldInfo fieldToInjectInto;
    private final String    identityString;
    private final String    declaringTypeInternalName;
    private final String    fieldTypeInternalName;

    private Factory(final FieldInfo fieldToInjectInto) {
      this.fieldToInjectInto = fieldToInjectInto;
      this.identityString = fieldToInjectInto.getDeclaringType().getName() + "." + fieldToInjectInto.getName();
      this.declaringTypeInternalName = ByteCodeUtil.classNameToInternalName(fieldToInjectInto.getDeclaringType()
          .getName());
      this.fieldTypeInternalName = ByteCodeUtil.classNameToInternalName(fieldToInjectInto.getType().getName());
    }

    @Override
    public int hashCode() {
      return identityString.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Factory other = (Factory) obj;
      if (fieldToInjectInto == null) {
        if (other.fieldToInjectInto != null) return false;
      }
      return identityString.equals(other.identityString);
    }

    public ClassAdapter create(final ClassVisitor visitor, final ClassLoader loader) {
      return new Adapter(visitor, loader);
    }

    private final class Adapter extends ClassAdapter implements Opcodes {

      private boolean isInjectionMethodPresent = false;

      private Adapter(final ClassVisitor cv, final ClassLoader caller) {
        super(cv);
      }

      @Override
      public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
                                       final String[] exceptions) {
        if (TC_INJECTION_METHOD_NAME.equals(name)) {
          isInjectionMethodPresent = true;
          return new DsoClusterInjectionMethod(super.visitMethod(access, name, desc, signature, exceptions), access,
                                               name, desc);
        } else if ("<init>".equals(name)) { return new DsoClusterConstructorInjection(super.visitMethod(access, name,
                                                                                                        desc,
                                                                                                        signature,
                                                                                                        exceptions),
                                                                                      access, name, desc); }

        return super.visitMethod(access, name, desc, signature, exceptions);
      }

      private void addFieldInjectionByteCode(final MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "getManager",
                           "()Lcom/tc/object/bytecode/Manager;");
        mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/bytecode/Manager", "getDsoCluster",
                           "()Lcom/tc/cluster/DsoCluster;");
        mv.visitFieldInsn(PUTFIELD, declaringTypeInternalName, fieldToInjectInto.getName(),
                          'L' + fieldTypeInternalName + ';');
      }

      @Override
      public void visitEnd() {
        // create and populate the injection method if it's not already present
        if (!isInjectionMethodPresent) {
          MethodVisitor mv = super.visitMethod(ACC_PUBLIC, TC_INJECTION_METHOD_NAME, "()V", null, null);
          addFieldInjectionByteCode(mv);
          mv.visitInsn(RETURN);
          mv.visitMaxs(1, 1);
          mv.visitEnd();
        }

        super.visitEnd();
      }

      private final class DsoClusterInjectionMethod extends AdviceAdapter {
        @Override
        protected void onMethodEnter() {
          addFieldInjectionByteCode(mv);
        }

        public DsoClusterInjectionMethod(final MethodVisitor mv, final int access, final String name, final String desc) {
          super(mv, access, name, desc);
        }

      }

      private final class DsoClusterConstructorInjection extends AdviceAdapter {
        @Override
        protected void onMethodEnter() {
          // ensure that the injection method is always called first
          super.visitVarInsn(ALOAD, 0);
          super.visitMethodInsn(INVOKEVIRTUAL, declaringTypeInternalName, TC_INJECTION_METHOD_NAME, "()V");
        }

        // todo: this should be done in a better way, by catching the visitVarInsn(ALOAD, 0) call and holding it back
        // unless the next instruction is something else than an injection method call
        @Override
        public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
          // remove any other injection method calls
          if (TC_INJECTION_METHOD_NAME.equals(name)) {
            mv.visitInsn(POP);
          } else {
            super.visitMethodInsn(opcode, owner, name, desc);
          }
        }

        public DsoClusterConstructorInjection(final MethodVisitor mv, final int access, final String name,
                                              final String desc) {
          super(mv, access, name, desc);
        }

      }
    }
  }
}