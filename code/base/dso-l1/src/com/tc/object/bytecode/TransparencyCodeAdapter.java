/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
  private final TransparencyCodeSpec codeSpec;
  private final Label                labelZero = new Label();

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
      spec.shouldProceedInstrumentation(modifiers, theMethodName, desc);
      storeStackValuesToLocalVariables(desc);
      super.visitMethodInsn(INVOKESPECIAL, spec.getClassNameSlashes(), ByteCodeUtil.fieldGetterMethod(ClassAdapterBase
          .getDelegateFieldName(logicalExtendingClassName)), "()L" + logicalExtendingClassName + ";");
      loadLocalVariables(desc);
      super.visitMethodInsn(INVOKEVIRTUAL, logicalExtendingClassName, theMethodName, desc);
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
      super.visitInsn(DUP);
      super.visitInsn(DUP);
      super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/Util", "resolveAllReferencesBeforeClone",
                            "(Ljava/lang/Object;)V");
      super.visitMethodInsn(opcode, classname, theMethodName, desc);
      super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/Util",
                            "fixTCObjectReferenceOfClonedObject",
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
            super.visitInsn(DUP);
            super.visitLdcInsn(new Integer(autoLockType));
            mgrHelper.callManagerMethod("monitorEnter", this);
            super.visitInsn(opCode);
          } else {
            super.visitInsn(opCode);
          }
          return;
        case MONITOREXIT:
          if (this.isAutolock) {
            super.visitInsn(DUP);
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
          Label noIndexException = new Label();
          super.visitInsn(DUP2);
          super.visitInsn(POP);
          callArrayManagerMethod("getObject", "(Ljava/lang/Object;)Lcom/tc/object/TCObject;");
          super.visitInsn(DUP);
          super.visitJumpInsn(IFNULL, notManaged);
          super.visitInsn(DUP2);
          super.visitInsn(SWAP);
          super.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "checkArrayIndex",
                                "(I)Ljava/lang/ArrayIndexOutOfBoundsException;");
          super.visitInsn(DUP);
          super.visitJumpInsn(IFNULL, noIndexException);
          super.visitInsn(SWAP);
          super.visitInsn(POP);
          super.visitInsn(SWAP);
          super.visitInsn(POP);
          super.visitInsn(SWAP);
          super.visitInsn(POP);
          super.visitInsn(ATHROW);
          super.visitLabel(noIndexException);
          super.visitInsn(POP);
          super.visitInsn(DUP_X2);
          super.visitInsn(DUP);
          super.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "getResolveLock", "()Ljava/lang/Object;");
          super.visitInsn(MONITORENTER);
          super.visitInsn(DUP2);
          super.visitInsn(SWAP);
          super.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "resolveArrayReference", "(I)V");
          super.visitInsn(POP);
          super.visitInsn(opCode);
          super.visitInsn(SWAP);
          super.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "getResolveLock", "()Ljava/lang/Object;");
          super.visitInsn(MONITOREXIT);
          super.visitJumpInsn(GOTO, end);
          super.visitLabel(notManaged);
          super.visitInsn(POP);
          super.visitInsn(opCode);
          super.visitLabel(end);
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
    super.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, name, desc);
  }

  public void visitMaxs(int stack, int vars) {
    super.visitMaxs(stack, vars + 1);
  }

  public void visitFieldInsn(final int opcode, final String classname, final String fieldName, final String desc) {
    spec.shouldProceedInstrumentation(fieldName, desc);

    if (!spec.needInstrumentFieldInsn() || !visitInit || !codeSpec.isFieldInstrumentationReq(fieldName)) {
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
    super.visitInsn(DUP);
    Label l1 = new Label();
    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mgrHelper.callManagerMethod("isPhysicallyInstrumented", this);
    super.visitJumpInsn(IFEQ, l1);
    swap(fieldType, reference);
    visitMethodInsn(INVOKEVIRTUAL, classname, ByteCodeUtil.fieldSetterMethod(fieldName), sDesc);
    Label l2 = new Label();
    super.visitJumpInsn(GOTO, l2);
    super.visitLabel(l1);
    swap(fieldType, reference);
    super.visitFieldInsn(PUTFIELD, classname, fieldName, desc);
    super.visitLabel(l2);
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
    super.visitInsn(DUP);
    Label l1 = new Label();
    super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mgrHelper.callManagerMethod("isPhysicallyInstrumented", this);
    super.visitJumpInsn(IFEQ, l1);
    visitMethodInsn(INVOKEVIRTUAL, classname, ByteCodeUtil.fieldGetterMethod(fieldName), gDesc);
    Label l2 = new Label();
    super.visitJumpInsn(GOTO, l2);
    super.visitLabel(l1);
    super.visitFieldInsn(GETFIELD, classname, fieldName, desc);
    super.visitLabel(l2);
  }

  private boolean isRoot(String classname, String fieldName) {
    return getTransparencyClassSpec().isRoot(classname.replace('/', '.'), fieldName);
  }

  protected String getSignature() {
    // This method added to silence warning about never reading "signature" field. If some code actually starts usiung
    // that field, then you can kill this method
    return this.signature;
  }

  protected void onMethodEnter() {
    if ("<init>".equals(methodName)) {
      visitInit = true;
      if (getTransparencyClassSpec().isLockMethod(modifiers, methodName, description, exceptions)) {
        callTCBeginWithLocks(this);
        super.visitLabel(labelZero);
      }
    }
  }

  protected void onMethodExit(int opcode) {
    if ("<init>".equals(methodName)
        && getTransparencyClassSpec().isLockMethod(modifiers, methodName, description, exceptions)) {

      if (opcode == RETURN) {
        callTCCommit(this);
      } else if (opcode == ATHROW) {
        // nothing special to do here, exception handler for method will do the commit
      } else {
        // <init> should not be returning with any other opcodes
        throw new AssertionError("unexpected exit instruction: " + opcode);
      }
    }
  }

  public void visitEnd() {
    if ("<init>".equals(methodName)
        && getTransparencyClassSpec().isLockMethod(modifiers, methodName, description, exceptions)) {

      Label labelEnd = new Label();
      super.visitLabel(labelEnd);
      super.visitTryCatchBlock(labelZero, labelEnd, labelEnd, null);
      int localVar = newLocal(1);
      super.visitVarInsn(ASTORE, localVar);
      callTCCommit(mv);
      super.visitVarInsn(ALOAD, localVar);
      mv.visitInsn(ATHROW);
    }

    super.visitEnd();
  }

}
