/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.object.config.MethodSpec;
import com.tc.object.logging.InstrumentationLogger;
import com.tcclient.util.MapEntrySetWrapper;
import com.tcclient.util.SortedViewSetWrapper;

import java.lang.reflect.Modifier;

/**
 * Used to create wrappers for logical methods
 */
public class LogicalMethodAdapter implements MethodAdapter, Opcodes {
  private String   ownerSlashes;
  private String   methodName;
  private String   originalMethodName;
  private String   description;
  private int      access;
  private String[] exceptions;
  private String   signature;
  private int      instrumentationType;
  private int      wrapperAccess;

  public LogicalMethodAdapter() {
    // When using this as a Method creator it doesn't need any of that stuff. yuck :-)
  }

  public LogicalMethodAdapter(String methodName, int instrumentationType) {
    this.methodName = methodName;
    this.instrumentationType = instrumentationType;
  }

  public void initialize(int anAccess, String aClassName, String aMethodName, String aOriginalMethodName,
                         String aDescription, String sig, String[] anExceptions, InstrumentationLogger logger,
                         MemberInfo info) {
    this.ownerSlashes = aClassName.replace('.', '/');
    this.methodName = aMethodName;
    this.originalMethodName = aOriginalMethodName;
    this.description = aDescription;
    this.wrapperAccess = anAccess & (~Modifier.SYNCHRONIZED); // wrapper method should have synch removed
    this.access = anAccess;
    this.exceptions = anExceptions;
    this.signature = sig;
  }

  public MethodVisitor adapt(ClassVisitor classVisitor) {
    createWrapperMethod(classVisitor);
    return classVisitor.visitMethod(access, getNewName(), description, signature, exceptions);
  }

  public boolean doesOriginalNeedAdapting() {
    return true;
  }

  protected String getNewName() {
    return ByteCodeUtil.TC_METHOD_PREFIX + methodName;
  }

  protected void createWrapperMethod(ClassVisitor classVisitor) {
    switch (instrumentationType) {
      case MethodSpec.ALWAYS_LOG:
        createAlwaysLogWrapperMethod(classVisitor, true);
        break;
      case MethodSpec.HASHMAP_REMOVE_LOG:
        createHashMapRemoveWrapperMethod(classVisitor, true);
        break;
      case MethodSpec.HASHTABLE_REMOVE_LOG:
        createHashMapRemoveWrapperMethod(classVisitor, false);
        break;
      case MethodSpec.TOBJECTHASH_REMOVE_AT_LOG:
        createTObjectHashRemoveAtWrapperMethod(classVisitor);
        break;
      case MethodSpec.HASHMAP_PUT_LOG:
        createHashPutWrapperMethod(classVisitor, true);
        break;
      case MethodSpec.HASHTABLE_PUT_LOG:
        createHashPutWrapperMethod(classVisitor, false);
        break;
      case MethodSpec.HASHTABLE_CLEAR_LOG:
        createAlwaysLogWrapperMethod(classVisitor, false);
        break;
      case MethodSpec.THASHMAP_PUT_LOG:
        createTHashMapPutWrapperMethod(classVisitor);
        break;
      case MethodSpec.IF_TRUE_LOG:
        createIfTrueLogWrapperMethod(classVisitor);
        break;
      case MethodSpec.SET_ITERATOR_WRAPPER_LOG:
        createSetIteratorWrapper(classVisitor);
        break;
      case MethodSpec.ENTRY_SET_WRAPPER_LOG:
        createEntrySetWrapper(classVisitor);
        break;
      case MethodSpec.KEY_SET_WRAPPER_LOG:
        createKeySetWrapper(classVisitor);
        break;
      case MethodSpec.VALUES_WRAPPER_LOG:
        createValuesWrapper(classVisitor);
        break;
      case MethodSpec.SORTED_SET_VIEW_WRAPPER_LOG:
        createSortedViewSetWrapper(classVisitor);
        break;
      default:
        throw new AssertionError("illegal instrumentationType:" + instrumentationType);
    }

  }

  private void createTObjectHashRemoveAtWrapperMethod(ClassVisitor classVisitor) {
    MethodVisitor mv = classVisitor.visitMethod(wrapperAccess, methodName, description, signature, exceptions);
    addCheckWriteAccessInstrumentedCode(mv, true);
    mv.visitInsn(ACONST_NULL);
    mv.visitVarInsn(ASTORE, 2);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, ownerSlashes, "_set", "[Ljava/lang/Object;");
    mv.visitVarInsn(ILOAD, 1);
    mv.visitInsn(AALOAD);
    mv.visitVarInsn(ASTORE, 2);
    ByteCodeUtil.pushThis(mv);
    Type[] params = Type.getArgumentTypes(description);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, getNewName(), description);

    ByteCodeUtil.pushThis(mv);

    mv.visitLdcInsn(methodName + description);
    mv.visitLdcInsn(Integer.valueOf(1));
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitInsn(DUP);
    mv.visitLdcInsn(Integer.valueOf(0));
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(AASTORE);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                       "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");

    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createSortedViewSetWrapper(ClassVisitor classVisitor) {
    MethodVisitor mv = classVisitor.visitMethod(wrapperAccess, methodName, description, signature, exceptions);
    mv.visitTypeInsn(NEW, SortedViewSetWrapper.CLASS_SLASH);
    mv.visitInsn(DUP);
    ByteCodeUtil.pushThis(mv);
    ByteCodeUtil.pushThis(mv);
    Type[] params = Type.getArgumentTypes(description);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/TreeSet", getNewName(), description);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }
    if ("headSet".equals(methodName)) {
      mv.visitInsn(ICONST_1);
      mv.visitMethodInsn(INVOKESPECIAL, SortedViewSetWrapper.CLASS_SLASH, "<init>",
                         "(Ljava/util/SortedSet;Ljava/util/SortedSet;Ljava/lang/Object;Z)V");
    } else if ("tailSet".equals(methodName)) {
      mv.visitInsn(ICONST_0);
      mv.visitMethodInsn(INVOKESPECIAL, SortedViewSetWrapper.CLASS_SLASH, "<init>",
                         "(Ljava/util/SortedSet;Ljava/util/SortedSet;Ljava/lang/Object;Z)V");
    } else {
      mv.visitMethodInsn(INVOKESPECIAL, SortedViewSetWrapper.CLASS_SLASH, "<init>",
                         "(Ljava/util/SortedSet;Ljava/util/SortedSet;Ljava/lang/Object;Ljava/lang/Object;)V");
    }

    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createSetIteratorWrapper(ClassVisitor classVisitor) {
    MethodVisitor mv = classVisitor.visitMethod(wrapperAccess, methodName, description, signature, exceptions);
    mv.visitTypeInsn(NEW, "com/tc/util/SetIteratorWrapper");
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, getNewName(), "()Ljava/util/Iterator;");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "com/tc/util/SetIteratorWrapper", "<init>",
                       "(Ljava/util/Iterator;Ljava/util/Set;)V");
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createEntrySetWrapper(ClassVisitor classVisitor) {
    MethodVisitor mv = classVisitor.visitMethod(wrapperAccess, methodName, description, signature, exceptions);
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitTypeInsn(NEW, MapEntrySetWrapper.CLASS_SLASH);
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, getNewName(), "()Ljava/util/Set;");
    mv.visitMethodInsn(INVOKESPECIAL, MapEntrySetWrapper.CLASS_SLASH, "<init>", "(Ljava/util/Map;Ljava/util/Set;)V");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Ljava/util/Map;", null, l0, l1, 0);
    mv.visitMaxs(5, 1);
    mv.visitEnd();
  }

  private void createKeySetWrapper(ClassVisitor classVisitor) {
    MethodVisitor mv = classVisitor.visitMethod(wrapperAccess, methodName, description, signature, exceptions);
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitTypeInsn(NEW, "com/tc/util/THashMapCollectionWrapper");
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, getNewName(), "()Ljava/util/Set;");
    mv.visitMethodInsn(INVOKESPECIAL, "com/tc/util/THashMapCollectionWrapper", "<init>",
                       "(Ljava/util/Map;Ljava/util/Collection;)V");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Ljava/util/Map;", null, l0, l1, 0);
    mv.visitMaxs(5, 1);
    mv.visitEnd();
  }

  private void createValuesWrapper(ClassVisitor classVisitor) {
    MethodVisitor mv = classVisitor.visitMethod(wrapperAccess, methodName, description, signature, exceptions);
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitTypeInsn(NEW, "com/tc/util/THashMapCollectionWrapper");
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, getNewName(), "()Ljava/util/Collection;");
    mv.visitMethodInsn(INVOKESPECIAL, "com/tc/util/THashMapCollectionWrapper", "<init>",
                       "(Ljava/util/Map;Ljava/util/Collection;)V");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Ljava/util/Map;", null, l0, l1, 0);
    mv.visitMaxs(5, 1);
    mv.visitEnd();
  }

  private void createTHashMapPutWrapperMethod(ClassVisitor classVisitor) {
    MethodVisitor mv = classVisitor.visitMethod(wrapperAccess, methodName, description, signature, exceptions);
    addCheckWriteAccessInstrumentedCode(mv, true);
    mv.visitInsn(ACONST_NULL);
    mv.visitVarInsn(ASTORE, 3);
    ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, "containsKey", "(Ljava/lang/Object;)Z");
    Label l2 = new Label();
    mv.visitJumpInsn(IFEQ, l2);
    ByteCodeUtil.pushThis(mv);
    mv.visitFieldInsn(GETFIELD, ownerSlashes, "_set", "[Ljava/lang/Object;");
    ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, "index", "(Ljava/lang/Object;)I");
    mv.visitInsn(AALOAD);
    mv.visitVarInsn(ASTORE, 3);
    mv.visitLabel(l2);
    ByteCodeUtil.pushThis(mv);
    Type[] params = Type.getArgumentTypes(description);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, getNewName(), description);

    mv.visitVarInsn(ASTORE, 4);

    ByteCodeUtil.pushThis(mv);
    mv.visitLdcInsn(methodName + description);
    mv.visitLdcInsn(Integer.valueOf(3));
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitInsn(DUP);
    int count = 0;
    mv.visitLdcInsn(Integer.valueOf(count++));
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInsn(AASTORE);

    mv.visitInsn(DUP);
    mv.visitLdcInsn(Integer.valueOf(count++));
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(AASTORE);

    mv.visitInsn(DUP);
    mv.visitLdcInsn(Integer.valueOf(count++));
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(AASTORE);

    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                       "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");

    mv.visitVarInsn(ALOAD, 4);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createHashPutWrapperMethod(ClassVisitor classVisitor, boolean checkWriteAccessRequired) {
    MethodVisitor mv = classVisitor.visitMethod(wrapperAccess, methodName, description, signature, exceptions);
    if (checkWriteAccessRequired) {
      addCheckWriteAccessInstrumentedCode(mv, true);
    }

    // run the local put()
    ByteCodeUtil.pushThis(mv);
    Type[] params = Type.getArgumentTypes(description);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, getNewName(), description);
    mv.visitVarInsn(ASTORE, params.length + 1);

    // record the logical action if this map managed
    Label notManaged = new Label();
    ByteCodeUtil.pushThis(mv);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, ClassAdapterBase.MANAGED_METHOD, "()Lcom/tc/object/TCObject;");
    mv.visitJumpInsn(IFNULL, notManaged);
    ByteCodeUtil.pushThis(mv);
    mv.visitLdcInsn(originalMethodName + description);
    ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, "getEntry", "(Ljava/lang/Object;)L" + ownerSlashes + "$Entry;");
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes + "$Entry", "getKey", "()Ljava/lang/Object;");
    mv.visitVarInsn(ASTORE, 1);
    ByteCodeUtil.createParametersToArrayByteCode(mv, params);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                       "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");

    mv.visitLabel(notManaged);
    mv.visitVarInsn(ALOAD, params.length + 1);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createHashMapRemoveWrapperMethod(ClassVisitor classVisitor, boolean checkWriteAccessRequired) {
    MethodVisitor mv = classVisitor.visitMethod(wrapperAccess, methodName, description, signature, exceptions);
    if (checkWriteAccessRequired) {
      addCheckWriteAccessInstrumentedCode(mv, true);
    }
    Type[] params = Type.getArgumentTypes(description);
    Label l0 = new Label();
    mv.visitLabel(l0);
    ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, "getEntry", "(Ljava/lang/Object;)L" + ownerSlashes + "$Entry;");
    mv.visitVarInsn(ASTORE, 2);
    mv.visitVarInsn(ALOAD, 2);
    Label l2 = new Label();
    mv.visitJumpInsn(IFNULL, l2);
    ByteCodeUtil.pushThis(mv);
    mv.visitLdcInsn(originalMethodName + description);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes + "$Entry", "getKey", "()Ljava/lang/Object;");
    mv.visitVarInsn(ASTORE, 1);
    ByteCodeUtil.createParametersToArrayByteCode(mv, params);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                       "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    mv.visitLabel(l2);
    ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, getNewName(), description);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createIfTrueLogWrapperMethod(ClassVisitor classVisitor) {
    MethodVisitor mv = classVisitor.visitMethod(wrapperAccess, methodName, description, signature, exceptions);
    addCheckWriteAccessInstrumentedCode(mv, true);
    Type[] params = Type.getArgumentTypes(description);
    Type returnType = Type.getReturnType(description);
    ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, getNewName(), description);
    mv.visitVarInsn(returnType.getOpcode(ISTORE), 2);
    mv.visitVarInsn(ILOAD, 2);
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    ByteCodeUtil.pushThis(mv);
    mv.visitLdcInsn(methodName + description);
    ByteCodeUtil.createParametersToArrayByteCode(mv, params);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                       "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    mv.visitLabel(l1);
    mv.visitVarInsn(returnType.getOpcode(ILOAD), 2);
    mv.visitInsn(returnType.getOpcode(IRETURN));
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createAlwaysLogWrapperMethod(ClassVisitor classVisitor, boolean checkWriteAccessRequired) {
    MethodVisitor mv = classVisitor.visitMethod(wrapperAccess, methodName, description, signature, exceptions);
    if (checkWriteAccessRequired) {
      addCheckWriteAccessInstrumentedCode(mv, true);
    }
    Label l0 = new Label();
    mv.visitLabel(l0);
    ByteCodeUtil.pushThis(mv);
    Type[] params = Type.getArgumentTypes(description);
    Type returnType = Type.getReturnType(description);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }

    mv.visitMethodInsn(INVOKESPECIAL, ownerSlashes, getNewName(), description);
    if (!returnType.equals(Type.VOID_TYPE)) {
      mv.visitVarInsn(returnType.getOpcode(ISTORE), params.length + 1);
    }
    ByteCodeUtil.pushThis(mv);
    mv.visitLdcInsn(originalMethodName + description);

    ByteCodeUtil.createParametersToArrayByteCode(mv, params);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "logicalInvoke",
                       "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");

    if (!returnType.equals(Type.VOID_TYPE)) {
      mv.visitVarInsn(returnType.getOpcode(ILOAD), params.length + 1);
    }
    mv.visitInsn(returnType.getOpcode(IRETURN));
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  protected void addCheckWriteAccessInstrumentedCode(MethodVisitor mv, boolean checkManaged) {
    Label notManaged = new Label();

    if (checkManaged) {
      ByteCodeUtil.pushThis(mv);
      mv.visitMethodInsn(INVOKEVIRTUAL, getOwnerSlashes(), ClassAdapterBase.MANAGED_METHOD,
                         "()Lcom/tc/object/TCObject;");
      mv.visitJumpInsn(IFNULL, notManaged);
    }
    ByteCodeUtil.pushThis(mv);
    mv.visitMethodInsn(INVOKESTATIC, ManagerUtil.CLASS, "checkWriteAccess", "(Ljava/lang/Object;)V");
    mv.visitLabel(notManaged);
  }

  protected int getInstrumentationType() {
    return instrumentationType;
  }

  protected int getAccess() {
    return access;
  }

  protected String getDescription() {
    return description;
  }

  protected String[] getExceptions() {
    return exceptions;
  }

  protected String getMethodName() {
    return methodName;
  }

  protected String getOwnerSlashes() {
    return ownerSlashes;
  }

  protected String getSignature() {
    return signature;
  }

  protected int getWrapperAccess() {
    return wrapperAccess;
  }

  protected String getOriginalMethodName() {
    return originalMethodName;
  }
}
