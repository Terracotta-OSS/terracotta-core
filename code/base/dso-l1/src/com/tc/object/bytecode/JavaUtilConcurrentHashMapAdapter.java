/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.asm.commons.LocalVariablesSorter;
import com.tc.util.runtime.Vm;

public class JavaUtilConcurrentHashMapAdapter extends ClassAdapter implements Opcodes {
  private final static String CONCURRENT_HASH_MAP_SLASH           = "java/util/concurrent/ConcurrentHashMap";
  private final static String TC_HASH_METHOD_NAME                 = ByteCodeUtil.TC_METHOD_PREFIX + "hash";
  private final static String TC_HASH_METHOD_DESC                 = "(Ljava/lang/Object;)I";
  private final static String TC_HASH_METHOD_CHECK_DESC           = "(Ljava/lang/Object;Z)I";
  private final static String TC_REHASH_METHOD_NAME               = ByteCodeUtil.TC_METHOD_PREFIX + "rehash";
  private final static String TC_REHASH_METHOD_DESC               = "()V";
  private final static String TC_CLEAR_METHOD_NAME                = ByteCodeUtil.TC_METHOD_PREFIX + "clear";
  private final static String TC_CLEAR_METHOD_DESC                = "()V";
  private final static String TC_PUT_METHOD_NAME                  = ByteCodeUtil.TC_METHOD_PREFIX + "put";
  private final static String TC_PUT_METHOD_DESC                  = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
  private final static String SEGMENT_TC_PUT_METHOD_DESC          = "(Ljava/lang/Object;ILjava/lang/Object;Z)Ljava/lang/Object;";
  private final static String TC_IS_DSO_HASH_REQUIRED_METHOD_NAME = ByteCodeUtil.TC_METHOD_PREFIX + "isDsoHashRequired";
  private final static String TC_IS_DSO_HASH_REQUIRED_METHOD_DESC = "(Ljava/lang/Object;)Z";
  private final static String TC_FULLY_LOCK_METHOD_NAME           = ByteCodeUtil.TC_METHOD_PREFIX + "fullyLock";
  private final static String TC_FULLY_LOCK_METHOD_DESC           = "()V";
  private final static String TC_FULLY_UNLOCK_METHOD_NAME         = ByteCodeUtil.TC_METHOD_PREFIX + "fullyUnLock";
  private final static String TC_FULLY_UNLOCK_METHOD_DESC         = "()V";
  private final static String HASH_METHOD_NAME                    = "hash";

  public JavaUtilConcurrentHashMapAdapter(ClassVisitor cv) {
    super(cv);
  }

  /**
   * We need to instrument the size(), isEmpty(), and containsValue() methods because the original implementation in jdk
   * 1.5 has an optimization which uses a volatile variable and does not require locking of the segments. It resorts to
   * locking only after several unsucessful attempts. For instance, the original implementation of the size() method
   * looks at the count and mod_count volatile variables of each segment and makes sure that there is no update during
   * executing the size() method. If it detects any update while the size() method is being executed, it will resort to
   * locking. Since ConcurrentHashMap is supported logically, it is possible that while the application is obtaining the
   * size of the map while there are still pending updates. Therefore, when ConcurrentHashMap is shared, the
   * instrumented code will always use an locking scheme to make sure all updates are applied before returing the size.
   * The same is true for isEmpty() and containsValue methods().
   */
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if ("size".equals(name) && "()I".equals(desc)) {
      return addWrapperMethod(access, name, desc, signature, exceptions);
    } else if ("isEmpty".equals(name) && "()Z".equals(desc)) {
      return addWrapperMethod(access, name, desc, signature, exceptions);
    } else if ("containsValue".equals(name) && "(Ljava/lang/Object;)Z".equals(desc)) { return addWrapperMethod(
                                                                                                               access,
                                                                                                               name,
                                                                                                               desc,
                                                                                                               signature,
                                                                                                               exceptions); }

    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("entrySet".equals(name) && "()Ljava/util/Set;".equals(desc)) {
      return new EntrySetMethodAdapter(mv);
    } else if ("segmentFor".equals(name) && "(I)Ljava/util/concurrent/ConcurrentHashMap$Segment;".equals(desc)) {
      rewriteSegmentForMethod(mv);
    } else if ("containsKey".equals(name) && "(Ljava/lang/Object;)Z".equals(desc)) {
      mv = new ContainsKeyMethodAdapter(mv);
    } else if ("get".equals(name) && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
      mv = new GetMethodAdapter(mv);
    } else if ("remove".equals(name) && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
      mv = new SimpleRemoveMethodAdapter(mv);
    } else if ("remove".equals(name) && "(Ljava/lang/Object;Ljava/lang/Object;)Z".equals(desc)) {
      mv = new RemoveMethodAdapter(mv);
    } else if ("replace".equals(name) && "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
      mv = new SimpleReplaceMethodAdapter(mv);
    } else if ("replace".equals(name) && "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z".equals(desc)) {
      mv = new ReplaceMethodAdapter(mv);
    }

    return new ConcurrentHashMapMethodAdapter(access, desc, mv);
  }

  public void visitEnd() {
    createTCPutMethod();
    createTCSharedHashMethod();
    createTCForcedHashMethod();
    createTCDsoRequiredMethod();
    createTCRehashAndSupportMethods();
    createTCFullyLockMethod();
    createTCFullyUnLockMethod();
    super.visitEnd();
  }

  private String getNewName(String methodName) {
    return ByteCodeUtil.TC_METHOD_PREFIX + methodName;
  }

  private MethodVisitor addWrapperMethod(int access, String name, String desc, String signature, String[] exceptions) {
    createWrapperMethod(access, name, desc, signature, exceptions);
    return cv.visitMethod(ACC_PRIVATE, getNewName(name), desc, signature, exceptions);
  }

  private void createWrapperMethod(int access, String name, String desc, String signature, String[] exceptions) {
    Type[] params = Type.getArgumentTypes(desc);

    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2, null);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(805, l3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
    mv.visitVarInsn(ISTORE, 2);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(806, l4);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitJumpInsn(IFEQ, l0);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLineNumber(807, l5);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_FULLY_LOCK_METHOD_NAME, TC_FULLY_LOCK_METHOD_DESC);
    mv.visitLabel(l0);
    mv.visitLineNumber(810, l0);
    mv.visitVarInsn(ALOAD, 0);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, getNewName(name), desc);
    mv.visitVarInsn(ISTORE, 4);
    mv.visitLabel(l1);
    mv.visitLineNumber(812, l1);
    mv.visitVarInsn(ILOAD, 2);
    Label l6 = new Label();
    mv.visitJumpInsn(IFEQ, l6);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLineNumber(813, l7);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_FULLY_UNLOCK_METHOD_NAME,
                       TC_FULLY_UNLOCK_METHOD_DESC);
    mv.visitLabel(l6);
    mv.visitLineNumber(810, l6);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitInsn(IRETURN);
    mv.visitLabel(l2);
    mv.visitLineNumber(811, l2);
    mv.visitVarInsn(ASTORE, 3);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitLineNumber(812, l8);
    mv.visitVarInsn(ILOAD, 2);
    Label l9 = new Label();
    mv.visitJumpInsn(IFEQ, l9);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitLineNumber(813, l10);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_FULLY_UNLOCK_METHOD_NAME,
                       TC_FULLY_UNLOCK_METHOD_DESC);
    mv.visitLabel(l9);
    mv.visitLineNumber(815, l9);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInsn(ATHROW);
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitLocalVariable("this", "Ljava/util/concurrent/ConcurrentHashMap;",
                          "Ljava/util/concurrent/ConcurrentHashMap<TK;TV;>;", l3, l11, 0);
    mv.visitLocalVariable("value", "Ljava/lang/Object;", null, l3, l11, 1);
    mv.visitLocalVariable("isManaged", "Z", null, l4, l11, 2);
    mv.visitMaxs(2, 5);
    mv.visitEnd();
  }

  private void rewriteSegmentForMethod(MethodVisitor mv) {
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "abs", "(I)I");
    mv.visitVarInsn(ISTORE, 1);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, CONCURRENT_HASH_MAP_SLASH, "segments",
                      "[Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitVarInsn(ILOAD, 1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, CONCURRENT_HASH_MAP_SLASH, "segments",
                      "[Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitInsn(ARRAYLENGTH);
    mv.visitInsn(IREM);
    mv.visitInsn(AALOAD);
    mv.visitInsn(ARETURN);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createTCFullyLockMethod() {
    MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, TC_FULLY_LOCK_METHOD_NAME, TC_FULLY_LOCK_METHOD_DESC, null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(789, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, CONCURRENT_HASH_MAP_SLASH, "segments",
                      "[Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitVarInsn(ASTORE, 1);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLineNumber(790, l1);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 2);
    Label l2 = new Label();
    mv.visitLabel(l2);
    Label l3 = new Label();
    mv.visitJumpInsn(GOTO, l3);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(791, l4);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitInsn(AALOAD);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$Segment", "lock", "()V");
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLineNumber(790, l5);
    mv.visitIincInsn(2, 1);
    mv.visitLabel(l3);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IF_ICMPLT, l4);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitLineNumber(792, l6);
    mv.visitInsn(RETURN);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLocalVariable("this", "Ljava/util/concurrent/ConcurrentHashMap;",
                          "Ljava/util/concurrent/ConcurrentHashMap<TK;TV;>;", l0, l7, 0);
    mv.visitLocalVariable("segments", "[Ljava/util/concurrent/ConcurrentHashMap$Segment;", null, l1, l7, 1);
    mv.visitLocalVariable("i", "I", null, l2, l6, 2);
    mv.visitMaxs(2, 3);
    mv.visitEnd();
  }

  private void createTCFullyUnLockMethod() {
    MethodVisitor mv = cv
        .visitMethod(ACC_PRIVATE, TC_FULLY_UNLOCK_METHOD_NAME, TC_FULLY_UNLOCK_METHOD_DESC, null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(795, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, CONCURRENT_HASH_MAP_SLASH, "segments",
                      "[Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitVarInsn(ASTORE, 1);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLineNumber(796, l1);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 2);
    Label l2 = new Label();
    mv.visitLabel(l2);
    Label l3 = new Label();
    mv.visitJumpInsn(GOTO, l3);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(797, l4);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitInsn(AALOAD);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$Segment", "unlock", "()V");
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLineNumber(796, l5);
    mv.visitIincInsn(2, 1);
    mv.visitLabel(l3);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IF_ICMPLT, l4);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitLineNumber(798, l6);
    mv.visitInsn(RETURN);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLocalVariable("this", "Ljava/util/concurrent/ConcurrentHashMap;",
                          "Ljava/util/concurrent/ConcurrentHashMap<TK;TV;>;", l0, l7, 0);
    mv.visitLocalVariable("segments", "[Ljava/util/concurrent/ConcurrentHashMap$Segment;", null, l1, l7, 1);
    mv.visitLocalVariable("i", "I", null, l2, l6, 2);
    mv.visitMaxs(2, 3);
    mv.visitEnd();
  }

  private void createTCDsoRequiredMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE, TC_IS_DSO_HASH_REQUIRED_METHOD_NAME,
                                         TC_IS_DSO_HASH_REQUIRED_METHOD_DESC, null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, CONCURRENT_HASH_MAP_SLASH, "__tc_managed", "()Lcom/tc/object/TCObject;");
    Label l1 = new Label();
    mv.visitJumpInsn(IFNULL, l1);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitTypeInsn(INSTANCEOF, "com/tc/object/bytecode/Manageable");
    Label l2 = new Label();
    mv.visitJumpInsn(IFEQ, l2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitTypeInsn(CHECKCAST, "com/tc/object/bytecode/Manageable");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/bytecode/Manageable", "__tc_managed",
                       "()Lcom/tc/object/TCObject;");
    mv.visitJumpInsn(IFNONNULL, l1);
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
    mv.visitJumpInsn(IF_ICMPNE, l1);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitInsn(ICONST_0);
    mv.visitInsn(IRETURN);
    mv.visitLabel(l1);
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IRETURN);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createTCRehashAndSupportMethods() {
    createTCRehashMethod();
    createTCClearMethod();
  }

  private void createTCRehashMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_SYNTHETIC, TC_REHASH_METHOD_NAME, TC_REHASH_METHOD_DESC,
                                         null, null);
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    mv.visitTryCatchBlock(l0, l1, l1, "java/lang/Throwable");
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l2, l2, null);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(670, l3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, CONCURRENT_HASH_MAP_SLASH, "size", "()I");
    Label l4 = new Label();
    mv.visitJumpInsn(IFLE, l4);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLineNumber(671, l5);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_FULLY_LOCK_METHOD_NAME, TC_FULLY_LOCK_METHOD_DESC);
    mv.visitLabel(l0);
    mv.visitLineNumber(673, l0);
    mv.visitTypeInsn(NEW, "java/util/ArrayList");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
    mv.visitVarInsn(ASTORE, 1);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitLineNumber(674, l6);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 2);
    Label l7 = new Label();
    mv.visitLabel(l7);
    Label l8 = new Label();
    mv.visitJumpInsn(GOTO, l8);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitLineNumber(675, l9);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, CONCURRENT_HASH_MAP_SLASH, "segments",
                      "[Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitVarInsn(ILOAD, 2);
    mv.visitInsn(AALOAD);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "table",
                      "[Ljava/util/concurrent/ConcurrentHashMap$HashEntry;");
    mv.visitVarInsn(ASTORE, 3);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitLineNumber(676, l10);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 4);
    Label l11 = new Label();
    mv.visitLabel(l11);
    Label l12 = new Label();
    mv.visitJumpInsn(GOTO, l12);
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitLineNumber(677, l13);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitInsn(AALOAD);
    Label l14 = new Label();
    mv.visitJumpInsn(IFNULL, l14);
    Label l15 = new Label();
    mv.visitLabel(l15);
    mv.visitLineNumber(678, l15);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitInsn(AALOAD);
    mv.visitVarInsn(ASTORE, 5);
    Label l16 = new Label();
    mv.visitLabel(l16);
    mv.visitLineNumber(679, l16);
    Label l17 = new Label();
    mv.visitJumpInsn(GOTO, l17);
    Label l18 = new Label();
    mv.visitLabel(l18);
    mv.visitLineNumber(680, l18);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z");
    mv.visitInsn(POP);
    Label l19 = new Label();
    mv.visitLabel(l19);
    mv.visitLineNumber(681, l19);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "next",
                      "Ljava/util/concurrent/ConcurrentHashMap$HashEntry;");
    mv.visitVarInsn(ASTORE, 5);
    mv.visitLabel(l17);
    mv.visitLineNumber(679, l17);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitJumpInsn(IFNONNULL, l18);
    mv.visitLabel(l14);
    mv.visitLineNumber(676, l14);
    mv.visitIincInsn(4, 1);
    mv.visitLabel(l12);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IF_ICMPLT, l13);
    Label l20 = new Label();
    mv.visitLabel(l20);
    mv.visitLineNumber(674, l20);
    mv.visitIincInsn(2, 1);
    mv.visitLabel(l8);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, CONCURRENT_HASH_MAP_SLASH, "segments",
                      "[Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IF_ICMPLT, l9);
    Label l21 = new Label();
    mv.visitLabel(l21);
    mv.visitLineNumber(686, l21);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, CONCURRENT_HASH_MAP_SLASH, "__tc_clear", "()V");
    Label l22 = new Label();
    mv.visitLabel(l22);
    mv.visitLineNumber(687, l22);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;");
    mv.visitVarInsn(ASTORE, 2);
    Label l23 = new Label();
    mv.visitLabel(l23);
    Label l24 = new Label();
    mv.visitJumpInsn(GOTO, l24);
    Label l25 = new Label();
    mv.visitLabel(l25);
    mv.visitLineNumber(688, l25);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;");
    mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/ConcurrentHashMap$HashEntry");
    mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/ConcurrentHashMap$HashEntry");
    mv.visitVarInsn(ASTORE, 3);
    Label l26 = new Label();
    mv.visitLabel(l26);
    mv.visitLineNumber(689, l26);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "key", "Ljava/lang/Object;");
    mv.visitVarInsn(ASTORE, 4);
    Label l27 = new Label();
    mv.visitLabel(l27);
    mv.visitLineNumber(690, l27);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "value", "Ljava/lang/Object;");
    mv.visitVarInsn(ASTORE, 5);
    Label l28 = new Label();
    mv.visitLabel(l28);
    mv.visitLineNumber(691, l28);
    invokeJdkHashMethod(mv, 4);
    mv.visitVarInsn(ISTORE, 6);
    Label l29 = new Label();
    mv.visitLabel(l29);
    mv.visitLineNumber(692, l29);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitInsn(ICONST_0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_HASH_METHOD_NAME, TC_HASH_METHOD_CHECK_DESC);
    mv.visitMethodInsn(INVOKEVIRTUAL, CONCURRENT_HASH_MAP_SLASH, "segmentFor",
                       "(I)Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ILOAD, 6);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitInsn(ICONST_0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$Segment", TC_PUT_METHOD_NAME,
                       SEGMENT_TC_PUT_METHOD_DESC);
    mv.visitInsn(POP);
    mv.visitLabel(l24);
    mv.visitLineNumber(687, l24);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
    mv.visitJumpInsn(IFNE, l25);
    Label l30 = new Label();
    mv.visitLabel(l30);
    Label l31 = new Label();
    mv.visitJumpInsn(GOTO, l31);
    mv.visitLabel(l1);
    mv.visitLineNumber(694, l1);
    mv.visitVarInsn(ASTORE, 1);
    Label l32 = new Label();
    mv.visitLabel(l32);
    mv.visitLineNumber(695, l32);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "(Ljava/io/PrintStream;)V");
    Label l33 = new Label();
    mv.visitLabel(l33);
    mv.visitJumpInsn(GOTO, l31);
    mv.visitLabel(l2);
    mv.visitLineNumber(696, l2);
    mv.visitVarInsn(ASTORE, 7);
    Label l34 = new Label();
    mv.visitLabel(l34);
    mv.visitLineNumber(697, l34);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_FULLY_UNLOCK_METHOD_NAME,
                       TC_FULLY_UNLOCK_METHOD_DESC);
    Label l35 = new Label();
    mv.visitLabel(l35);
    mv.visitLineNumber(698, l35);
    mv.visitVarInsn(ALOAD, 7);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l31);
    mv.visitLineNumber(697, l31);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, "__tc_fullyUnLock", "()V");
    mv.visitLabel(l4);
    mv.visitLineNumber(700, l4);
    mv.visitInsn(RETURN);
    Label l36 = new Label();
    mv.visitLabel(l36);
    mv.visitLocalVariable("this", "Ljava/util/concurrent/ConcurrentHashMap;",
                          "Ljava/util/concurrent/ConcurrentHashMap<TK;TV;>;", l3, l36, 0);
    mv.visitLocalVariable("entries", "Ljava/util/List;", null, l6, l1, 1);
    mv.visitLocalVariable("i", "I", null, l7, l21, 2);
    mv.visitLocalVariable("segmentEntries", "[Ljava/util/concurrent/ConcurrentHashMap$HashEntry;", null, l10, l20, 3);
    mv.visitLocalVariable("j", "I", null, l11, l20, 4);
    mv.visitLocalVariable("first", "Ljava/util/concurrent/ConcurrentHashMap$HashEntry;", null, l16, l14, 5);
    mv.visitLocalVariable("i", "Ljava/util/Iterator;", null, l23, l30, 2);
    mv.visitLocalVariable("entry", "Ljava/util/concurrent/ConcurrentHashMap$HashEntry;", null, l26, l24, 3);
    mv.visitLocalVariable("key", "Ljava/lang/Object;", null, l27, l24, 4);
    mv.visitLocalVariable("value", "Ljava/lang/Object;", null, l28, l24, 5);
    mv.visitLocalVariable("hash", "I", null, l29, l24, 6);
    mv.visitLocalVariable("t", "Ljava/lang/Throwable;", null, l32, l33, 1);
    mv.visitMaxs(5, 8);
    mv.visitEnd();
  }

  private void createTCPutMethod() {
    MethodVisitor mv = super
        .visitMethod(ACC_PUBLIC + ACC_SYNTHETIC, TC_PUT_METHOD_NAME, TC_PUT_METHOD_DESC, null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 2);
    Label l1 = new Label();
    mv.visitJumpInsn(IFNONNULL, l1);
    mv.visitTypeInsn(NEW, "java/lang/NullPointerException");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "()V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l1);
    invokeJdkHashMethod(mv, 1);
    mv.visitVarInsn(ISTORE, 3);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ICONST_0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_HASH_METHOD_NAME, TC_HASH_METHOD_CHECK_DESC);
    mv.visitMethodInsn(INVOKEVIRTUAL, CONCURRENT_HASH_MAP_SLASH, "segmentFor",
                       "(I)Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ICONST_0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$Segment", TC_PUT_METHOD_NAME,
                       SEGMENT_TC_PUT_METHOD_DESC);
    mv.visitInsn(ARETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createTCClearMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE + ACC_SYNTHETIC, TC_CLEAR_METHOD_NAME, TC_CLEAR_METHOD_DESC, null,
                                         null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 1);
    Label l1 = new Label();
    mv.visitLabel(l1);
    Label l2 = new Label();
    mv.visitJumpInsn(GOTO, l2);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, CONCURRENT_HASH_MAP_SLASH, "segments",
                      "[Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitVarInsn(ILOAD, 1);
    mv.visitInsn(AALOAD);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$Segment", TC_CLEAR_METHOD_NAME,
                       TC_CLEAR_METHOD_DESC);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitIincInsn(1, 1);
    mv.visitLabel(l2);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, CONCURRENT_HASH_MAP_SLASH, "segments",
                      "[Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IF_ICMPLT, l3);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitInsn(RETURN);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /*
   * ConcurrentHashMap uses the hashcode of the key and identify the segment to use. Each segment is an ReentrantLock.
   * This prevents multiple threads to update the same segment at the same time. To support in DSO, we need to check if
   * the ConcurrentHashMap is a shared object. If it is, we check if the hashcode of the key is the same as the
   * System.identityHashCode. If it is, we will use the DSO ObjectID of the key to be the hashcode. Since the ObjectID
   * of the key is a cluster-wide constant, different node will identify the same segment based on the ObjectID of the
   * key. If the hashcode of the key is not the same as the System.identityHashCode, that would mean the application has
   * defined the hashcode of the key and in this case, we could use honor the application defined hashcode of the key.
   * The reason that we do not want to always use the ObjectID of the key is because if the application has defined the
   * hashcode of the key, map.get(key1) and map.get(key2) will return the same object if key1 and key2 has the same
   * application defined hashcode even though key1 and key2 has 2 different ObjectID. Using ObjectID as the hashcode in
   * this case will prevent map.get(key1) and map.get(key2) to return the same result. If the application has not
   * defined the hashcode of the key, key1 and key2 will have 2 different hashcode (due to the fact that they will have
   * different System.identityHashCode). Therefore, map.get(key1) and map.get(key2) will return different objects. In
   * this case, using ObjectID will have the proper behavior. One limitation is that if the application define the
   * hashcode as some combination of system specific data such as a combination of System.identityHashCode() and some
   * other data, the current support of ConcurrentHashMap does not support this scenario. Another limitation is that if
   * the application defined hashcode of the key happens to be the same as the System.identityHashCode, the current
   * support of ConcurrentHashMap does not support this scenario either.
   */
  private void createTCSharedHashMethod() {
    MethodVisitor mv = cv
        .visitMethod(ACC_PRIVATE + ACC_SYNTHETIC, TC_HASH_METHOD_NAME, TC_HASH_METHOD_DESC, null, null);
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ICONST_1);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_HASH_METHOD_NAME, TC_HASH_METHOD_CHECK_DESC);
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createTCForcedHashMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE + ACC_SYNTHETIC, TC_HASH_METHOD_NAME, TC_HASH_METHOD_CHECK_DESC,
                                         null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I");
    mv.visitVarInsn(ISTORE, 3);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 4);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
    mv.visitVarInsn(ILOAD, 3);
    Label l3 = new Label();
    mv.visitJumpInsn(IF_ICMPNE, l3);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ILOAD, 2);
    Label l5 = new Label();
    mv.visitJumpInsn(IFEQ, l5);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, CONCURRENT_HASH_MAP_SLASH, "__tc_managed", "()Lcom/tc/object/TCObject;");
    Label l7 = new Label();
    mv.visitJumpInsn(IFNONNULL, l7);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isCreationInProgress", "()Z");
    mv.visitJumpInsn(IFEQ, l3);
    mv.visitLabel(l7);
    mv.visitInsn(ICONST_1);
    mv.visitVarInsn(ISTORE, 4);
    mv.visitJumpInsn(GOTO, l3);
    mv.visitLabel(l5);
    mv.visitInsn(ICONST_1);
    mv.visitVarInsn(ISTORE, 4);
    mv.visitLabel(l3);
    mv.visitVarInsn(ILOAD, 4);
    Label l8 = new Label();
    mv.visitJumpInsn(IFEQ, l8);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "shareObjectIfNecessary",
                       "(Ljava/lang/Object;)Lcom/tc/object/TCObject;");
    mv.visitVarInsn(ASTORE, 5);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitJumpInsn(IFNULL, l8);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "getObjectID", "()Lcom/tc/object/ObjectID;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/tc/object/ObjectID", "hashCode", "()I");
    mv.visitVarInsn(ISTORE, 3);
    mv.visitLabel(l8);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitIntInsn(BIPUSH, 9);
    mv.visitInsn(ISHL);
    mv.visitInsn(ICONST_M1);
    mv.visitInsn(IXOR);
    mv.visitInsn(IADD);
    mv.visitVarInsn(ISTORE, 3);
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitIntInsn(BIPUSH, 14);
    mv.visitInsn(IUSHR);
    mv.visitInsn(IXOR);
    mv.visitVarInsn(ISTORE, 3);
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitInsn(ICONST_4);
    mv.visitInsn(ISHL);
    mv.visitInsn(IADD);
    mv.visitVarInsn(ISTORE, 3);
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitIntInsn(BIPUSH, 10);
    mv.visitInsn(IUSHR);
    mv.visitInsn(IXOR);
    mv.visitVarInsn(ISTORE, 3);
    Label l14 = new Label();
    mv.visitLabel(l14);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitInsn(IRETURN);
    Label l17 = new Label();
    mv.visitLabel(l17);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private static class EntrySetMethodAdapter extends MethodAdapter implements Opcodes {

    public EntrySetMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);

      if ((opcode == INVOKESPECIAL) && "java/util/concurrent/ConcurrentHashMap$EntrySet".equals(owner)
          && "<init>".equals(name) && "(Ljava/util/concurrent/ConcurrentHashMap;)V".equals(desc)) {
        mv.visitVarInsn(ASTORE, 1);
        mv.visitTypeInsn(NEW, "com/tcclient/util/ConcurrentHashMapEntrySetWrapper");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, "com/tcclient/util/ConcurrentHashMapEntrySetWrapper", "<init>",
                           "(Ljava/util/Map;Ljava/util/Set;)V");
      }
    }
  }

  private abstract static class AddCheckManagedKeyMethodAdapter extends MethodAdapter implements Opcodes {
    public AddCheckManagedKeyMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitCode() {
      super.visitCode();
      addCheckManagedKeyCode();
    }

    protected abstract void addCheckManagedKeyCode();
  }

  private static class ContainsKeyMethodAdapter extends AddCheckManagedKeyMethodAdapter {
    public ContainsKeyMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    protected void addCheckManagedKeyCode() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_IS_DSO_HASH_REQUIRED_METHOD_NAME,
                         TC_IS_DSO_HASH_REQUIRED_METHOD_DESC);
      Label l1 = new Label();
      mv.visitJumpInsn(IFNE, l1);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitInsn(ICONST_0);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l1);
    }
  }

  private static class GetMethodAdapter extends AddCheckManagedKeyMethodAdapter {
    public GetMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    protected void addCheckManagedKeyCode() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_IS_DSO_HASH_REQUIRED_METHOD_NAME,
                         TC_IS_DSO_HASH_REQUIRED_METHOD_DESC);
      Label l1 = new Label();
      mv.visitJumpInsn(IFNE, l1);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitInsn(ACONST_NULL);
      mv.visitInsn(ARETURN);
      mv.visitLabel(l1);
    }
  }

  private static class SimpleRemoveMethodAdapter extends GetMethodAdapter {
    public SimpleRemoveMethodAdapter(MethodVisitor mv) {
      super(mv);
    }
  }

  private static class SimpleReplaceMethodAdapter extends SimpleRemoveMethodAdapter {
    private Label target;

    public SimpleReplaceMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitCode() {
      mv.visitCode();
    }

    public void visitJumpInsn(int opcode, Label label) {
      super.visitJumpInsn(opcode, label);
      if (IFNONNULL == opcode) {
        target = label;
      }
    }

    public void visitLabel(Label label) {
      super.visitLabel(label);
      if (label.equals(target)) {
        addCheckManagedKeyCode();
      }
    }
  }

  private static class RemoveMethodAdapter extends AddCheckManagedKeyMethodAdapter {
    public RemoveMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    protected void addCheckManagedKeyCode() {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_IS_DSO_HASH_REQUIRED_METHOD_NAME,
                         TC_IS_DSO_HASH_REQUIRED_METHOD_DESC);
      Label l1 = new Label();
      mv.visitJumpInsn(IFNE, l1);
      Label l2 = new Label();
      mv.visitLabel(l2);
      mv.visitInsn(ICONST_0);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l1);
    }
  }

  private static class ReplaceMethodAdapter extends RemoveMethodAdapter {
    private Label target;

    public ReplaceMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitCode() {
      mv.visitCode();
    }

    public void visitJumpInsn(int opcode, Label label) {
      super.visitJumpInsn(opcode, label);
      if (IFNONNULL == opcode) {
        target = label;
      }
    }

    public void visitLabel(Label label) {
      super.visitLabel(label);
      if (label.equals(target)) {
        addCheckManagedKeyCode();
      }
    }
  }

  private static class ConcurrentHashMapMethodAdapter extends LocalVariablesSorter implements Opcodes {

    public ConcurrentHashMapMethodAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
    }

    public int newLocal(int size) {
      return super.newLocal(size);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if (INVOKEVIRTUAL == opcode && CONCURRENT_HASH_MAP_SLASH.equals(owner) && "segmentFor".equals(name)
          && "(I)Ljava/util/concurrent/ConcurrentHashMap$Segment;".equals(desc)) {
        mv.visitInsn(POP);
        ByteCodeUtil.pushThis(mv);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, TC_HASH_METHOD_NAME, TC_HASH_METHOD_DESC);
        super.visitMethodInsn(opcode, owner, name, desc);
      } else if (INVOKESPECIAL == opcode
                 && JavaUtilConcurrentHashMapSegmentAdapter.CONCURRENT_HASH_MAP_SEGMENT_SLASH.equals(owner)
                 && "<init>".equals(name) && "(IF)V".equals(desc)) {
        mv.visitInsn(POP);
        mv.visitInsn(POP);
        ByteCodeUtil.pushThis(mv);
        mv.visitVarInsn(ILOAD, 7);
        mv.visitVarInsn(FLOAD, 2);
        mv.visitMethodInsn(opcode, owner, name, JavaUtilConcurrentHashMapSegmentAdapter.INIT_DESC);
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }
  }

  private void invokeJdkHashMethod(MethodVisitor mv, int objectVarNumber) {
    mv.visitVarInsn(ALOAD, objectVarNumber);
    if (Vm.isJDK16()) {
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I");
      mv.visitMethodInsn(INVOKESTATIC, CONCURRENT_HASH_MAP_SLASH, HASH_METHOD_NAME, "(I)I");
    } else {
      mv.visitMethodInsn(INVOKESTATIC, CONCURRENT_HASH_MAP_SLASH, HASH_METHOD_NAME, "(Ljava/lang/Object;)I");
    }
  }
}