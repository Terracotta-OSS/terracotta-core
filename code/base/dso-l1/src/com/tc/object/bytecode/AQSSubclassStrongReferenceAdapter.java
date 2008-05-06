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
import com.tc.object.util.ToggleableStrongReference;

/**
 * This adapter is to add behavior to subclasses of AbstractQueuedSynchronizer(AQS). A transient field is added to store
 * a toggle reference. When the state becomes non-zero, the toggle reference causes this object to be strongly
 * referenced. The strong reference is cleared when the state returns to zero<br>
 * <br>
 * NOTE: This zero/non-zero policy might not be appropriate for all AQS subclasses. It is possible that other types
 * might use different values to represent conditions that should apply for the toggling of the strong reference.
 */
public class AQSSubclassStrongReferenceAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  private static final String TOGGLE_REF_FIELD = ByteCodeUtil.TC_FIELD_PREFIX + "toggleRef";
  private static final String TOGGLE_REF_CLASS = ToggleableStrongReference.class.getName().replace('.', '/');
  private static final String TOGGLE_REF_TYPE  = "L" + TOGGLE_REF_CLASS + ";";

  private String              className;

  public AQSSubclassStrongReferenceAdapter(ClassVisitor cv) {
    super(cv);
  }

  public AQSSubclassStrongReferenceAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new AQSSubclassStrongReferenceAdapter(visitor);
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
   *       //
   * <br>
   *       // NOTE: It might be worth trying to optimize this to only happen in the case where the transition is from zero to non-zero
   *       //       For the path from AQS.compareAndSwapState() this is reasonable since we known the expected prior state, for AQS.setState()
   *       //       you'll need to evaluate if it safe to read the state before it is mutated.
   *       __tc_RRWLToggleRef.strongRef(this);
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
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEINTERFACE, TOGGLE_REF_CLASS, "strongRef", "(Ljava/lang/Object;)V");
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
