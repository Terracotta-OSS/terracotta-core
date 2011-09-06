/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class JavaUtilConcurrentHashMapHashEntryAdapter extends ClassAdapter implements Opcodes {
  private static final String THIS_TYPE                    = "java/util/concurrent/ConcurrentHashMap$HashEntry";

  private static final String PREFIX                       = ByteCodeUtil.TC_METHOD_PREFIX + "CHM_";

  static final String         GET_VALUE                    = PREFIX + "getValue";
  static final String         GET_VALUE_STORE_ONLY_NONNULL = PREFIX + "getValueStoreOnlyNonNull";
  static final String         GET_VALUE_RAW                = PREFIX + "getValueRaw";
  
  public JavaUtilConcurrentHashMapHashEntryAdapter(ClassVisitor cv) {
    super(cv);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterface(interfaces, TCMapEntry.class.getName().replace('.', '/'));
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public void visitEnd() {
    createTCRawSetValueMethod();
    createTCIsFaultedInMethod();
    createLazyGetValueMethods();
    createLazyGetValueInternal();
    createRawGetValueMethod();
    
    super.visitEnd();
  }

  /**
   * <code>
     Object __tc_CHM_getValue() {
       return __tc_CHM_getValue(false);
     }

     Object __tc_CHM_getValueStoreOnlyNonNull() {
       return __tc_CHM_getValue(true);
     }
     </code>
   */
  private void createLazyGetValueMethods() {
    {
      MethodVisitor mv = super.visitMethod(0, GET_VALUE, "()Ljava/lang/Object;", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitInsn(ICONST_0);
      mv.visitMethodInsn(INVOKESPECIAL, THIS_TYPE, GET_VALUE, "(Z)Ljava/lang/Object;");
      mv.visitInsn(ARETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }

    {
      MethodVisitor mv = super.visitMethod(0, GET_VALUE_STORE_ONLY_NONNULL, "()Ljava/lang/Object;", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitInsn(ICONST_1);
      mv.visitMethodInsn(INVOKESPECIAL, THIS_TYPE, GET_VALUE, "(Z)Ljava/lang/Object;");
      mv.visitInsn(ARETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }

  }

  /**
   * <code>
     private synchronized Object __tc_CHM_getValue(boolean storeOnlyNonNull) {
       Object o = value;
       if (o instanceof ObjectID) {
         o = ManagerUtil.lookupObject((ObjectID) o);
         if (storeOnlyNonNull && o == null) return null;
         value = o;
       }
       return o;
     }
     </code>
   */
  private void createLazyGetValueInternal() {
    MethodVisitor mv = super
        .visitMethod(ACC_PRIVATE + ACC_SYNCHRONIZED, GET_VALUE, "(Z)Ljava/lang/Object;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, THIS_TYPE, "value", "Ljava/lang/Object;");
    mv.visitVarInsn(ASTORE, 2);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitTypeInsn(INSTANCEOF, "com/tc/object/ObjectID");
    Label l2 = new Label();
    mv.visitJumpInsn(IFEQ, l2);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitTypeInsn(CHECKCAST, "com/tc/object/ObjectID");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "lookupObject",
                       "(Lcom/tc/object/ObjectID;)Ljava/lang/Object;");
    mv.visitVarInsn(ASTORE, 2);
    mv.visitVarInsn(ILOAD, 1);
    Label l5 = new Label();
    mv.visitJumpInsn(IFEQ, l5);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitJumpInsn(IFNONNULL, l5);
    mv.visitInsn(ACONST_NULL);
    mv.visitInsn(ARETURN);
    mv.visitLabel(l5);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitFieldInsn(PUTFIELD, THIS_TYPE, "value", "Ljava/lang/Object;");
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createRawGetValueMethod() {
    MethodVisitor mv = super.visitMethod(ACC_SYNCHRONIZED, GET_VALUE_RAW, "()Ljava/lang/Object;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, THIS_TYPE, "value", "Ljava/lang/Object;");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();    
  }
  /**
   * <code>
     public synchronized void __tc_rawSetValue(Object o) {
       this.value = o;
     }
     </code>
   */
  private void createTCRawSetValueMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNCHRONIZED, TCMapEntry.TC_RAWSETVALUE_METHOD_NAME,
                                         TCMapEntry.TC_RAWSETVALUE_METHOD_DESC, null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, THIS_TYPE, "value", "Ljava/lang/Object;");
    mv.visitInsn(RETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();
  }

  /**
   * <code>
     public synchronized boolean __tc_isValueFaultedIn() {
       return !(this.value instanceof ObjectID);
     }
     </code>
   */
  private void createTCIsFaultedInMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNCHRONIZED, TCMapEntry.TC_ISVALUEFAULTEDIN_METHOD_NAME,
                                         TCMapEntry.TC_ISVALUEFAULTEDIN_METHOD_DESC, null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, THIS_TYPE, "value", "Ljava/lang/Object;");
    mv.visitTypeInsn(INSTANCEOF, "com/tc/object/ObjectID");
    Label isNotObjectID = new Label();
    mv.visitJumpInsn(IFEQ, isNotObjectID);
    mv.visitInsn(ICONST_0);
    Label returnLabel = new Label();
    mv.visitJumpInsn(GOTO, returnLabel);
    mv.visitLabel(isNotObjectID);
    mv.visitInsn(ICONST_1);
    mv.visitLabel(returnLabel);
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }
}
