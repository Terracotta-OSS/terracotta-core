/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.util.ToggleableStrongReference;

public class JavaUtilConcurrentLocksRRWLSyncAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  // XXX: rename this class

  private static final String TOGGLE_REF_FIELD = ByteCodeUtil.TC_FIELD_PREFIX + "RRWLSyncToggleRef";
  private static final String TOGGLE_REF_CLASS = ToggleableStrongReference.class.getName().replace('.', '/');
  private static final String TOGGLE_REF_TYPE  = "L" + TOGGLE_REF_CLASS + ";";

  private String              className;

  public JavaUtilConcurrentLocksRRWLSyncAdapter(ClassVisitor cv) {
    super(cv);
  }

  public JavaUtilConcurrentLocksRRWLSyncAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new JavaUtilConcurrentLocksRRWLSyncAdapter(visitor);
  }

  public void visitEnd() {
    addToggleRefField();
    addStateChangedMethod();

    super.visitEnd();
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.className = name;
  }

  /**
   * When a managed sync changes to state zero, clear the hard reference (allowing it to be flushed. For all other
   * states, make sure the hard reference in place.
   *
   * <pre>
   * void __tc_AQS_stateChanged(int state) {
   *   TCObject tco = __tc_managed();
   *   if (tco != null) {
   *     if (__tc_RRWLToggleRef == null) {
   *       __tc_RRWLToggleRef = tco.getOrCreateToggleRef();
   *     }
   *     if (state == 0) {
   *       __tc_RRWLToggleRef.clearStrongRef();
   *     } else {
   *       // Since we can't know the previous state value in all cases, we can't
   *       // optimize the case of setting the hard reference only when going from 0 to non-zero
   *       __tc_RRWLToggleRef.strongRef();
   *     }
   *   }
   * }
   * </pre>
   */
  private void addStateChangedMethod() {
    MethodVisitor mv = super.visitMethod(ACC_SYNTHETIC | ACC_PROTECTED | ACC_FINAL,
                                         JavaUtilConcurrentLocksAQSAdapter.TC_STAGE_CHANGED,
                                         JavaUtilConcurrentLocksAQSAdapter.TC_STAGE_CHANGED_DESC, null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEINTERFACE, ByteCodeUtil.MANAGEABLE_CLASS, "__tc_managed", "()Lcom/tc/object/TCObject;");
    mv.visitVarInsn(ASTORE, 2);
    mv.visitVarInsn(ALOAD, 2);
    Label notManaged = new Label();
    mv.visitJumpInsn(IFNULL, notManaged);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, className, TOGGLE_REF_FIELD, TOGGLE_REF_TYPE);
    Label nonNullToggleRef = new Label();
    mv.visitJumpInsn(IFNONNULL, nonNullToggleRef);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "getOrCreateToggleRef", "()" + TOGGLE_REF_TYPE);
    mv.visitFieldInsn(PUTFIELD, className, TOGGLE_REF_FIELD, TOGGLE_REF_TYPE);
    mv.visitLabel(nonNullToggleRef);
    mv.visitVarInsn(ILOAD, 1);
    Label stateNonZero = new Label();
    mv.visitJumpInsn(IFNE, stateNonZero);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, className, TOGGLE_REF_FIELD, TOGGLE_REF_TYPE);
    mv.visitMethodInsn(INVOKEINTERFACE, TOGGLE_REF_CLASS, "clearStrongRef", "()V");
    mv.visitJumpInsn(GOTO, notManaged);
    mv.visitLabel(stateNonZero);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, className, TOGGLE_REF_FIELD, TOGGLE_REF_TYPE);
    mv.visitMethodInsn(INVOKEINTERFACE, TOGGLE_REF_CLASS, "strongRef", "()V");
    mv.visitLabel(notManaged);
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addToggleRefField() {
    super.visitField(ACC_PRIVATE | ACC_SYNTHETIC | ACC_VOLATILE | ACC_TRANSIENT, TOGGLE_REF_FIELD, TOGGLE_REF_TYPE,
                     null, null);
  }

}
