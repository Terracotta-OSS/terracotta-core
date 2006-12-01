/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.asm.commons.AdviceAdapter;
import com.tc.exception.TCInternalError;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.TransparencyCodeSpec;

/**
 * @author steve
 */
public class TransparencyCodeAdapter extends AdviceAdapter implements Opcodes {

  private final boolean              isAutolock;
  private final int                  autoLockType;
  private final int                  modifiers;
  private final String               methodName;
  private final String               signature;
  private final String               description;
  private final String[]             exceptions;
  private final ManagerHelper        mgrHelper;
  private final InstrumentationSpec  spec;
  private final Label                jsrLabel;
  private final Label                labelZero;
  private final Label                labelEnd;
  private final TransparencyCodeSpec codeSpec;

  private int                        localVariablesCount;
  private int[]                      localVariablesForMethodCall;

  private boolean                    visitInit = false;

  public TransparencyCodeAdapter(InstrumentationSpec spec, boolean isAutolock, int autoLockType,
                                 final MethodVisitor mv, final int modifiers, String originalMethodName,
                                 String methodName, String methodDesc, String signature, final String[] exceptions) {
    super(mv, modifiers, methodName, methodDesc);
    this.spec = spec;
    this.isAutolock = isAutolock;
    this.autoLockType = autoLockType;
    this.modifiers = modifiers;
    this.methodName = methodName;
    this.signature = signature;
    this.description = methodDesc;
    this.exceptions = exceptions;
    this.jsrLabel = new Label();
    this.labelEnd = new Label();
    this.labelZero = new Label();
    this.mgrHelper = spec.getManagerHelper();
    this.codeSpec = spec.getTransparencyClassSpec().getCodeSpec(originalMethodName, description, isAutolock);
    
    if (!"<init>".equals(methodName)) {
      visitInit = true;
    }
  }

  private void storeStackValuesToLocalVariables(String methodInsnDesc) {
    Type[] types = Type.getArgumentTypes(methodInsnDesc);
    localVariablesForMethodCall = new int[types.length];
    for (int i = 0; i < types.length; i++) {
      localVariablesForMethodCall[i] = newLocal(types[i].getSize());
      localVariablesCount++;
    }
    for (int i = types.length - 1; i >= 0; i--) {
      super.visitVarInsn(types[i].getOpcode(ISTORE), localVariablesForMethodCall[i]);
    }
  }

  private void loadLocalVariables(String methodInsnDesc) {
    Type[] types = Type.getArgumentTypes(methodInsnDesc);
    for (int i = 0; i < types.length; i++) {
      super.visitVarInsn(types[i].getOpcode(ILOAD), localVariablesForMethodCall[i]);
    }
  }

  public void visitMethodInsn(int opcode, String classname, String theMethodName, String desc) {
    if (handleSubclassOfLogicalClassMethodInsn(opcode, classname, theMethodName, desc)) { return; }
    if (codeSpec.isArraycopyInstrumentationReq(classname, theMethodName)) {
      rewriteArraycopy();
    } else if (classname.equals("java/lang/Object")) {
      handleJavaLangObjectMethodCall(opcode, classname, theMethodName, desc);
    } else {
      super.visitMethodInsn(opcode, classname, theMethodName, desc);
    }
  }

  private boolean handleSubclassOfLogicalClassMethodInsn(int opcode, String classname, String theMethodName, String desc) {
    if (!spec.hasDelegatedToLogicalClass()) { return false; }
    String logicalExtendingClassName = spec.getSuperClassNameSlashes();
    if (INVOKESPECIAL == opcode && !spec.getClassNameSlashes().equals(classname) && !"<init>".equals(theMethodName)) {
      storeStackValuesToLocalVariables(desc);
      mv.visitMethodInsn(INVOKESPECIAL, spec.getClassNameSlashes(), ByteCodeUtil.fieldGetterMethod(ClassAdapterBase
          .getDelegateFieldName(logicalExtendingClassName)), "()L" + logicalExtendingClassName + ";");
      loadLocalVariables(desc);
      mv.visitMethodInsn(INVOKEVIRTUAL, logicalExtendingClassName, theMethodName, desc);
      return true;
    }
    return false;
  }

  private TransparencyClassSpec getTransparencyClassSpec() {
    return spec.getTransparencyClassSpec();
  }

  private void rewriteArraycopy() {
    callArrayManagerMethod("arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
  }

  private void handleJavaLangObjectMethodCall(int opcode, String classname, String theMethodName, String desc) {
    if (handleJavaLangObjectWaitNotifyCalls(opcode, classname, theMethodName, desc)) {
      return;
    } else if (handleJavaLangObjectCloneCall(opcode, classname, theMethodName, desc)) {
      return;
    } else {
      super.visitMethodInsn(opcode, classname, theMethodName, desc);
    }
  }

  /*
   * The assumption here is that the compiler wouldnt call invokevirtual on a classname other than java.lang.Object when
   * there is no implementation of clone() defined in that classes' hierarchy. If it does, it a bug in the compiler ;-)
   * This adaption is needed for both PORTABLE and ADAPTABLE classes as we can have instance where Logical subclass of
   * ADAPTABLE class calls clone() to make a copy of itself.
   * 
   * @see AbstractMap and HashMap
   */
  private boolean handleJavaLangObjectCloneCall(int opcode, String classname, String theMethodName, String desc) {
    if ("clone".equals(theMethodName) && "()Ljava/lang/Object;".equals(desc)) {
      mv.visitInsn(DUP);
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/Util", "resolveAllReferencesBeforeClone",
                         "(Ljava/lang/Object;)V");
      super.visitMethodInsn(opcode, classname, theMethodName, desc);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/Util", "fixTCObjectReferenceOfClonedObject",
                         "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
      return true;
    }
    return false;
  }

  private boolean handleJavaLangObjectWaitNotifyCalls(int opcode, String classname, String theMethodName, String desc) {
    if (spec.isLogical() || !codeSpec.isWaitNotifyInstrumentationReq()) { return false; }

    Type[] args = Type.getArgumentTypes(desc);

    if (theMethodName.equals("notify") || theMethodName.equals("notifyAll")) {
      if (args.length == 0) {
        if (theMethodName.endsWith("All")) {
          mgrHelper.callManagerMethod("objectNotifyAll", this);
        } else {
          mgrHelper.callManagerMethod("objectNotify", this);
        }
        return true;
      }
      throw new TCInternalError("Unexpected java.lang.Object method signature: " + theMethodName + " + " + desc);
    } else if (theMethodName.equals("wait")) {

      switch (args.length) {
        case 0: {
          mgrHelper.callManagerMethod("objectWait0", this);
          return true;
        }
        case 1: {
          if (args[0].equals(Type.LONG_TYPE)) {
            mgrHelper.callManagerMethod("objectWait1", this);
            return true;
          }
          throw new TCInternalError("Unexpected java.lang.Object method signature: " + theMethodName + " + " + desc);
        }
        case 2: {
          if ((args[0].equals(Type.LONG_TYPE)) && (args[1].equals(Type.INT_TYPE))) {
            mgrHelper.callManagerMethod("objectWait2", this);
            return true;
          }
          throw new TCInternalError("Unexpected java.lang.Object method signature: " + theMethodName + " + " + desc);
        }
        default: {
          throw new TCInternalError("Unexpected java.lang.Object method signature: " + theMethodName + " + " + desc);
        }
      }
    } else { // neither wait(...) nor notify[All]()
      return false;
    }

    // should be unreachable
  }

  private void callTCBeginWithLocks(MethodVisitor c) {
    c.visitLabel(new Label());
    LockDefinition[] defs = getTransparencyClassSpec().lockDefinitionsFor(modifiers, methodName, description,
                                                                          exceptions);
    for (int i = 0; i < defs.length; i++) {
      if (!defs[i].isAutolock()) {
        callTCBeginWithLock(defs[i], c);
      }
    }
  }

  private void callTCBeginWithLock(LockDefinition lock, MethodVisitor c) {
    c.visitLdcInsn(ByteCodeUtil.generateNamedLockName(lock.getLockName()));
    c.visitLdcInsn(new Integer(lock.getLockLevelAsInt()));
    mgrHelper.callManagerMethod("beginLock", c);
  }

  private void callTCCommit(MethodVisitor c) {
    LockDefinition[] locks = getTransparencyClassSpec().lockDefinitionsFor(modifiers, methodName, description,
                                                                           exceptions);
    for (int i = 0; i < locks.length; i++) {
      if (!locks[i].isAutolock()) {
        c.visitLdcInsn(ByteCodeUtil.generateNamedLockName(locks[i].getLockName()));
        mgrHelper.callManagerMethod("commitLock", c);
      }
    }
  }

  public void visitInsn(int opCode) {
    if (isMonitorInstrumentationReq(opCode)) {
      switch (opCode) {
        case MONITORENTER:
          if (this.isAutolock) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(new Integer(autoLockType));
            mgrHelper.callManagerMethod("monitorEnter", this);
            super.visitInsn(opCode);
          } else {
            super.visitInsn(opCode);
          }
          return;
        case MONITOREXIT:
          if (this.isAutolock) {
            mv.visitInsn(DUP);
            super.visitInsn(opCode);
            mgrHelper.callManagerMethod("monitorExit", this);
          } else {
            super.visitInsn(opCode);
          }
          return;
      }
    }
    if (isArrayOperatorInstrumentationReq(opCode)) {
      switch (opCode) {
        case AALOAD:
          Label end = new Label();
          Label notManaged = new Label();
          mv.visitInsn(DUP2);
          mv.visitInsn(POP);
          callArrayManagerMethod("getObject", "(Ljava/lang/Object;)Lcom/tc/object/TCObject;");
          mv.visitInsn(DUP);
          mv.visitJumpInsn(IFNULL, notManaged);
          mv.visitInsn(DUP_X2);
          mv.visitInsn(DUP);
          mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "getResolveLock", "()Ljava/lang/Object;");
          mv.visitInsn(MONITORENTER);
          mv.visitInsn(DUP2);
          mv.visitInsn(SWAP);
          mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "resolveArrayReference", "(I)V");
          mv.visitInsn(POP);
          super.visitInsn(opCode);
          mv.visitInsn(SWAP);
          mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "getResolveLock", "()Ljava/lang/Object;");
          mv.visitInsn(MONITOREXIT);
          mv.visitJumpInsn(GOTO, end);
          mv.visitLabel(notManaged);
          mv.visitInsn(POP);
          super.visitInsn(opCode);
          mv.visitLabel(end);
          return;
        case AASTORE:
          callArrayManagerMethod("objectArrayChanged", "([Ljava/lang/Object;ILjava/lang/Object;)V");
          return;
        case LASTORE:
          callArrayManagerMethod("longArrayChanged", "([JIJ)V");
          return;
        case SASTORE:
          callArrayManagerMethod("shortArrayChanged", "([SIS)V");
          return;
        case IASTORE:
          callArrayManagerMethod("intArrayChanged", "([III)V");
          return;
        case DASTORE:
          callArrayManagerMethod("doubleArrayChanged", "([DID)V");
          return;
        case FASTORE:
          callArrayManagerMethod("floatArrayChanged", "([FIF)V");
          return;
        case BASTORE:
          callArrayManagerMethod("byteOrBooleanArrayChanged", "(Ljava/lang/Object;IB)V");
          return;
        case CASTORE:
          callArrayManagerMethod("charArrayChanged", "([CIC)V");
          return;
      }
    }
    super.visitInsn(opCode);
  }

  private boolean isArrayOperatorInstrumentationReq(int opCode) {
    return ((opCode == AALOAD || opCode == AASTORE || opCode == LASTORE || opCode == SASTORE || opCode == IASTORE
             || opCode == DASTORE || opCode == FASTORE || opCode == BASTORE || opCode == CASTORE) && codeSpec
        .isArrayOperatorInstrumentationReq());
  }

  private boolean isMonitorInstrumentationReq(int opCode) {
    return ((opCode == MONITORENTER || opCode == MONITOREXIT) && codeSpec.isMonitorInstrumentationReq());
  }

  private void callArrayManagerMethod(String name, String desc) {
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, name, desc);
  }

  public void visitMaxs(int stack, int vars) {
    super.visitMaxs(stack, vars + 1);
  }

  public void visitFieldInsn(final int opcode, final String classname, final String fieldName, final String desc) {
    spec.shouldProceedInstrumentation(fieldName, desc);

    if (spec.isLogical() || !visitInit || !codeSpec.isFieldInstrumentationReq(fieldName)) {
      super.visitFieldInsn(opcode, classname, fieldName, desc);
      return;
    }

    if (spec.isPhysical()) {
      // if (opcode == GETFIELD && (isRoot(classname, fieldName) || !isPrimitive(Type.getType(desc)))) {
      if (opcode == GETFIELD) {
        visitGetFieldInsn(classname, fieldName, desc);
        return;
      } else if (opcode == PUTFIELD) {
        visitSetFieldInsn(classname, fieldName, desc);
        return;
      } else if (opcode == PUTSTATIC && isRoot(classname, fieldName)) {
        String sDesc = "(" + desc + ")V";
        visitMethodInsn(INVOKESTATIC, classname, ByteCodeUtil.fieldSetterMethod(fieldName), sDesc);
        return;
      } else if (opcode == GETSTATIC && isRoot(classname, fieldName)) {
        String gDesc = "()" + desc;
        visitMethodInsn(INVOKESTATIC, classname, ByteCodeUtil.fieldGetterMethod(fieldName), gDesc);
        return;
      }
      super.visitFieldInsn(opcode, classname, fieldName, desc);
    } else {
      super.visitFieldInsn(opcode, classname, fieldName, desc);
    }
  }

  private void visitSetFieldInsn(String classname, String fieldName, String desc) {
    boolean inClassHierarchy = spec.isInClassHierarchy(classname);
    if ((spec.isClassPortable() && inClassHierarchy) || isRoot(classname, fieldName)) {
      // If the field is a root, we assume that the class is instrumented automatically.
      // If it is not then bad things are gonna happen anyway.
      visitUncheckedSetFieldInsn(classname, fieldName, desc);
    } else if (spec.isClassAdaptable() && inClassHierarchy) {
      visitSetFieldInsnOriginal(classname, fieldName, desc);
    } else {
      visitCheckedSetFieldInsn(classname, fieldName, desc);
    }
  }

  private void visitSetFieldInsnOriginal(String classname, String fieldName, String desc) {
    // System.err.println("Original :: My class : " + spec.getClassNameSlashes() + " set on : " + classname + " field :
    // " + fieldName);
    super.visitFieldInsn(PUTFIELD, classname, fieldName, desc);
  }

  private void visitUncheckedSetFieldInsn(String classname, String fieldName, String desc) {
    // System.err.println("Unchecked :: My class : " + spec.getClassNameSlashes() + " set on : " + classname + " field :
    // " + fieldName);
    String sDesc = "(" + desc + ")V";
    visitMethodInsn(INVOKEVIRTUAL, classname, ByteCodeUtil.fieldSetterMethod(fieldName), sDesc);
  }

  /**
   * This method assumes that we dont have anyinfo on the class that we are setting the field to. so we take the
   * conservative approach.
   */
  private void visitCheckedSetFieldInsn(String classname, String fieldName, String desc) {
    // System.err.println("Checked :: My class : " + spec.getClassNameSlashes() + " set on : " + classname + " field : "
    // + fieldName);
    Type fieldType = Type.getType(desc);
    Type reference = Type.getType("Ljava/lang/Object;");
    String sDesc = "(" + desc + ")V";

    swap(reference, fieldType);
    mv.visitInsn(DUP);
    Label l1 = new Label();
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mgrHelper.callManagerMethod("isPhysicallyInstrumented", mv);
    mv.visitJumpInsn(IFEQ, l1);
    swap(fieldType, reference);
    visitMethodInsn(INVOKEVIRTUAL, classname, ByteCodeUtil.fieldSetterMethod(fieldName), sDesc);
    Label l2 = new Label();
    mv.visitJumpInsn(GOTO, l2);
    mv.visitLabel(l1);
    swap(fieldType, reference);
    mv.visitFieldInsn(PUTFIELD, classname, fieldName, desc);
    mv.visitLabel(l2);
  }

  private void visitGetFieldInsn(String classname, String fieldName, String desc) {
    boolean inClassHierarchy = spec.isInClassHierarchy(classname);
    if ((spec.isClassPortable() && inClassHierarchy) || isRoot(classname, fieldName)) {
      // If the field is a root, we assume that the class is instrumented automatically.
      // If it is not then bad things are gonna happen anyway.
      visitUncheckedGetFieldInsn(classname, fieldName, desc);
    } else if (spec.isClassAdaptable() && inClassHierarchy) {
      visitGetFieldInsnOriginal(classname, fieldName, desc);
    } else {
      visitCheckedGetFieldInsn(classname, fieldName, desc);
    }
  }

  private void visitGetFieldInsnOriginal(String classname, String fieldName, String desc) {
    // System.err.println("Original :: My class : " + spec.getClassNameSlashes() + " get on : " + classname + " field :
    // " + fieldName);
    super.visitFieldInsn(GETFIELD, classname, fieldName, desc);
  }

  private void visitUncheckedGetFieldInsn(String classname, String fieldName, String desc) {
    // System.err.println("Unchecked :: My class: " + spec.getClassNameSlashes() + " get on : " + classname + " field :
    // " + fieldName);
    String gDesc = "()" + desc;
    visitMethodInsn(INVOKEVIRTUAL, classname, ByteCodeUtil.fieldGetterMethod(fieldName), gDesc);
  }

  /**
   * This method assumes that we dont have anyinfo on the class that we are setting the field to. so we take the
   * conservative approach.
   */
  private void visitCheckedGetFieldInsn(String classname, String fieldName, String desc) {
    String gDesc = "()" + desc;
    mv.visitInsn(DUP);
    Label l1 = new Label();
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mgrHelper.callManagerMethod("isPhysicallyInstrumented", mv);
    mv.visitJumpInsn(IFEQ, l1);
    visitMethodInsn(INVOKEVIRTUAL, classname, ByteCodeUtil.fieldGetterMethod(fieldName), gDesc);
    Label l2 = new Label();
    mv.visitJumpInsn(GOTO, l2);
    mv.visitLabel(l1);
    mv.visitFieldInsn(GETFIELD, classname, fieldName, desc);
    mv.visitLabel(l2);
  }

  public void visitLocalVariable(String name, String desc, String fieldSignature, Label start, Label end, int index) {
    super.visitLocalVariable(name, desc, fieldSignature, start, end, index);
    localVariablesCount++;
  }

  private void addFinallyByteCode(MethodVisitor c) {

    mv.visitJumpInsn(GOTO, jsrLabel);
    mv.visitLabel(labelEnd);
    mv.visitVarInsn(ASTORE, localVariablesCount + 1);
    callTCCommit(c);
    mv.visitVarInsn(ALOAD, localVariablesCount + 1);
    mv.visitInsn(ATHROW);
    mv.visitLabel(jsrLabel);
    callTCCommit(c);
  }

  private boolean isRoot(String classname, String fieldName) {
    return getTransparencyClassSpec().isRoot(classname.replace('/', '.'), fieldName);
  }

  protected String getSignature() {
    // This method added to silence warning about never reading "signature" field. If some code actually starts usiung
    // that field, then you can kill this method
    return this.signature;
  }

  // --------------------------------------------------------------------
  // Copied from GeneratorAdapter
  // --------------------------------------------------------------------
  public void swap() {
    mv.visitInsn(Opcodes.SWAP);
  }

  /**
   * Generates the instructions to swap the top two stack values.
   * 
   * @param prev type of the top - 1 stack value.
   * @param type type of the top stack value.
   */
  public void swap(final Type prev, final Type type) {
    if (type.getSize() == 1) {
      if (prev.getSize() == 1) {
        swap(); // same as dupX1(), pop();
      } else {
        dupX2();
        pop();
      }
    } else {
      if (prev.getSize() == 1) {
        dup2X1();
        pop2();
      } else {
        dup2X2();
        pop2();
      }
    }
  }

  // ------------------------------------------------------------------------
  // Instructions to manage the stack
  // ------------------------------------------------------------------------

  /**
   * Generates a POP instruction.
   */
  public void pop() {
    mv.visitInsn(Opcodes.POP);
  }

  /**
   * Generates a POP2 instruction.
   */
  public void pop2() {
    mv.visitInsn(Opcodes.POP2);
  }

  /**
   * Generates a DUP instruction.
   */
  public void dup() {
    mv.visitInsn(Opcodes.DUP);
  }

  /**
   * Generates a DUP2 instruction.
   */
  public void dup2() {
    mv.visitInsn(Opcodes.DUP2);
  }

  /**
   * Generates a DUP_X1 instruction.
   */
  public void dupX1() {
    mv.visitInsn(Opcodes.DUP_X1);
  }

  /**
   * Generates a DUP_X2 instruction.
   */
  public void dupX2() {
    mv.visitInsn(Opcodes.DUP_X2);
  }

  /**
   * Generates a DUP2_X1 instruction.
   */
  public void dup2X1() {
    mv.visitInsn(Opcodes.DUP2_X1);
  }

  /**
   * Generates a DUP2_X2 instruction.
   */
  public void dup2X2() {
    mv.visitInsn(Opcodes.DUP2_X2);
  }

  protected void onMethodEnter() {
    if ("<init>".equals(methodName)) {
      visitInit = true;
      if (getTransparencyClassSpec().isLockMethod(modifiers, methodName, description, exceptions)) {
        mv.visitTryCatchBlock(labelZero, labelEnd, labelEnd, null);
        callTCBeginWithLocks(mv);
        mv.visitLabel(labelZero);
      }
    }
  }

  protected void onMethodExit(int opcode) {
    if ("<init>".equals(methodName)
        && getTransparencyClassSpec().isLockMethod(modifiers, methodName, description, exceptions)) {
      addFinallyByteCode(mv);
    }
  }

}
