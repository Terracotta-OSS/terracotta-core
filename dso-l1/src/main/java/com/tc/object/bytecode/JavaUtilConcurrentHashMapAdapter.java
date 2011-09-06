/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

public class JavaUtilConcurrentHashMapAdapter extends ClassAdapter implements Opcodes {
  private final static String CONCURRENT_HASH_MAP_SLASH           = "java/util/concurrent/ConcurrentHashMap";
  private final static String TC_REHASH_METHOD_NAME               = ByteCodeUtil.TC_METHOD_PREFIX + "rehash";
  private final static String TC_REHASH_METHOD_DESC               = "()V";
  private final static String TC_CLEAR_METHOD_NAME                = ByteCodeUtil.TC_METHOD_PREFIX + "clear";
  private final static String TC_CLEAR_METHOD_DESC                = "()V";
  private final static String TC_ORIG_GET_METHOD_NAME             = ByteCodeUtil.TC_METHOD_PREFIX + "origGet";
  private final static String TC_ORIG_GET_METHOD_DESC             = "(Ljava/lang/Object;)Ljava/lang/Object;";
  private final static String TC_PUT_METHOD_NAME                  = ByteCodeUtil.TC_METHOD_PREFIX + "put";
  private final static String TC_PUT_METHOD_DESC                  = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
  private final static String TC_HASH_METHOD_NAME                 = ByteCodeUtil.TC_METHOD_PREFIX + "hash";
  private final static String TC_HASH_METHOD_DESC                 = "(Ljava/lang/Object;)I";
  private final static String TC_IS_POSSIBLE_KEY_METHOD_NAME      = ByteCodeUtil.TC_METHOD_PREFIX + "isPossibleKey";
  private final static String TC_IS_POSSIBLE_KEY_METHOD_DESC      = "(Ljava/lang/Object;)Z";
  private final static String TC_FULLY_READLOCK_METHOD_NAME       = ByteCodeUtil.TC_METHOD_PREFIX + "fullyReadLock";
  private final static String TC_FULLY_READLOCK_METHOD_DESC       = "()V";
  private final static String TC_FULLY_READUNLOCK_METHOD_NAME     = ByteCodeUtil.TC_METHOD_PREFIX + "fullyReadUnlock";
  private final static String TC_FULLY_READUNLOCK_METHOD_DESC     = "()V";
  private final static String TC_APPLICATOR_PUT_METHOD_NAME       = "__tc_applicator_put";
  private final static String TC_APPLICATOR_PUT_METHOD_DESC       = "(Ljava/lang/Object;Ljava/lang/Object;)V";
  private final static String TC_APPLICATOR_REMOVE_METHOD_NAME    = "__tc_applicator_remove";
  private final static String TC_APPLICATOR_REMOVE_METHOD_DESC    = "(Ljava/lang/Object;)V";
  private final static String TC_APPLICATOR_CLEAR_METHOD_NAME     = "__tc_applicator_clear";
  private final static String TC_APPLICATOR_CLEAR_METHOD_DESC     = "()V";
  private final static String TC_REMOVE_LOGICAL_METHOD_NAME       = "__tc_remove_logical";
  private final static String TC_REMOVE_LOGICAL_METHOD_DESC       = "(Ljava/lang/Object;)V";  
  private final static String TC_PUT_LOGICAL_METHOD_NAME          = "__tc_put_logical";
  private final static String TC_PUT_LOGICAL_METHOD_DESC          = "(Ljava/lang/Object;Ljava/lang/Object;)V";
  private final static String HASH_METHOD_NAME                    = "hash";

  public JavaUtilConcurrentHashMapAdapter(ClassVisitor cv) {
    super(cv);
  }

  /**
   * We need to instrument the size(), isEmpty(), and containsValue() methods because the original implementation in JDK
   * 1.5 has an optimization which uses a volatile variable and does not require locking of the segments. It resorts to
   * locking only after several unsuccessful attempts. For instance, the original implementation of the size() method
   * looks at the count and mod_count volatile variables of each segment and makes sure that there is no update during
   * executing the size() method. If it detects any update while the size() method is being executed, it will resort to
   * locking. Since ConcurrentHashMap is supported logically, it is possible that while the application is obtaining the
   * size of the map while there are still pending updates. Therefore, when ConcurrentHashMap is shared, the
   * instrumented code will always use an locking scheme to make sure all updates are applied before returning the size.
   * The same is true for isEmpty() and containsValue methods().
   */
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if ("size".equals(name) && "()I".equals(desc)) {
      return addWrapperMethod(access, name, desc, signature, exceptions);
    } else if ("isEmpty".equals(name) && "()Z".equals(desc)) {
      return addWrapperMethod(access, name, desc, signature, exceptions);
    } else if ("containsValue".equals(name) && "(Ljava/lang/Object;)Z".equals(desc)) {
      return addWrapperMethod(access, name, desc, signature, exceptions);
    }

    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("entrySet".equals(name) && "()Ljava/util/Set;".equals(desc)) {
      return new SetWrapperMethodAdapter(mv, "java/util/concurrent/ConcurrentHashMap$EntrySet",
                                         "com/tcclient/util/ConcurrentHashMapEntrySetWrapper");
    } else if ("keySet".equals(name) && "()Ljava/util/Set;".equals(desc)) {
      return new SetWrapperMethodAdapter(mv, "java/util/concurrent/ConcurrentHashMap$KeySet",
      "com/tcclient/util/ConcurrentHashMapKeySetWrapper");
    } else if ("segmentFor".equals(name) && "(I)Ljava/util/concurrent/ConcurrentHashMap$Segment;".equals(desc)) {
      return rewriteSegmentForMethod(mv);
    } else if ("get".equals(name) && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
      return new MulticastMethodVisitor(new MethodVisitor[] {
        new RetargetingMethodAdapter(super.visitMethod(ACC_SYNTHETIC|ACC_PUBLIC, TC_ORIG_GET_METHOD_NAME, TC_ORIG_GET_METHOD_DESC, null, null),
                                     "get", JavaUtilConcurrentHashMapSegmentAdapter.TC_ORIG_GET_METHOD_NAME,
                                     JavaUtilConcurrentHashMapSegmentAdapter.TC_ORIG_GET_METHOD_DESC),
        new ConcurrentHashMapMethodAdapter(new GetMethodAdapter(mv))});
    }

    if ("put".equals(name) && "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
      mv = new MulticastMethodVisitor(new MethodVisitor[] {
        mv,
        new NoReturnRetargetingMethodAdapter(super.visitMethod(ACC_SYNTHETIC|ACC_PUBLIC, TC_APPLICATOR_PUT_METHOD_NAME, TC_APPLICATOR_PUT_METHOD_DESC, null, null),
                                             "put", JavaUtilConcurrentHashMapSegmentAdapter.TC_ORIG_PUT_METHOD_NAME,
                                             JavaUtilConcurrentHashMapSegmentAdapter.TC_ORIG_PUT_METHOD_DESC),
        new NoReturnRetargetingMethodAdapter(super.visitMethod(ACC_SYNTHETIC|ACC_PUBLIC, TC_PUT_LOGICAL_METHOD_NAME, TC_PUT_LOGICAL_METHOD_DESC, null, null),
                                             "put", JavaUtilConcurrentHashMapSegmentAdapter.TC_NULL_RETURN_PUT_METHOD_NAME,
                                             JavaUtilConcurrentHashMapSegmentAdapter.TC_NULL_RETURN_PUT_METHOD_DESC)});
    } else if ("clear".equals(name) && "()V".equals(desc)) {
      mv = new MulticastMethodVisitor(new MethodVisitor[] {
        mv,
        new RetargetingMethodAdapter(super.visitMethod(ACC_SYNTHETIC|ACC_PUBLIC, TC_APPLICATOR_CLEAR_METHOD_NAME, TC_APPLICATOR_CLEAR_METHOD_DESC, null, null),
                                     "clear", JavaUtilConcurrentHashMapSegmentAdapter.TC_ORIG_CLEAR_METHOD_NAME,
                                     JavaUtilConcurrentHashMapSegmentAdapter.TC_ORIG_CLEAR_METHOD_DESC)});
    } else if ("containsKey".equals(name) && "(Ljava/lang/Object;)Z".equals(desc)) {
      mv = new ContainsKeyMethodAdapter(mv);
    } else if ("remove".equals(name) && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
      mv = new MulticastMethodVisitor(new MethodVisitor[] {
        new SimpleRemoveMethodAdapter(mv),
        new NoReturnRetargetingMethodAdapter(super.visitMethod(ACC_SYNTHETIC|ACC_PUBLIC, TC_APPLICATOR_REMOVE_METHOD_NAME, TC_APPLICATOR_REMOVE_METHOD_DESC, null, null),
                                             "remove", JavaUtilConcurrentHashMapSegmentAdapter.TC_ORIG_REMOVE_METHOD_NAME,
                                             JavaUtilConcurrentHashMapSegmentAdapter.TC_ORIG_REMOVE_METHOD_DESC),
        new NoReturnRetargetingMethodAdapter(super.visitMethod(ACC_SYNTHETIC|ACC_PUBLIC, TC_REMOVE_LOGICAL_METHOD_NAME, TC_REMOVE_LOGICAL_METHOD_DESC, null, null),
                                             "remove", JavaUtilConcurrentHashMapSegmentAdapter.TC_NULL_RETURN_REMOVE_METHOD_NAME,
                                             JavaUtilConcurrentHashMapSegmentAdapter.TC_NULL_RETURN_REMOVE_METHOD_DESC)});
    } else if ("remove".equals(name) && "(Ljava/lang/Object;Ljava/lang/Object;)Z".equals(desc)) {
      mv = new RemoveMethodAdapter(mv);
    } else if ("replace".equals(name) && "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
      mv = new SimpleReplaceMethodAdapter(mv);
    } else if ("replace".equals(name) && "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z".equals(desc)) {
      mv = new ReplaceMethodAdapter(mv);
    } else if ("writeObject".equals(name) && "(Ljava/io/ObjectOutputStream;)V".equals(desc)) {
      mv = new JavaUtilConcurrentHashMapLazyValuesMethodAdapter(access, desc, mv, false);
    }

    return new ConcurrentHashMapMethodAdapter(mv);
  }

  public void visitEnd() {
    createTCPutMethod();
    createTCRehashAndSupportMethods();
    createTCFullyReadLockMethod();
    createTCFullyReadUnlockMethod();

    super.visitEnd();
  }

  private String getNewName(String methodName) {
    return ByteCodeUtil.TC_METHOD_PREFIX + methodName;
  }

  private MethodVisitor addWrapperMethod(int access, String name, String desc, String signature, String[] exceptions) {
    createWrapperMethod(access, name, desc, signature, exceptions);
    return new TurnIntoReadLocksMethodAdapter(cv
        .visitMethod(ACC_PRIVATE, getNewName(name), desc, signature, exceptions));
  }

  private void createWrapperMethod(int access, String name, String desc, String signature, String[] exceptions) {
    Type[] params = Type.getArgumentTypes(desc);
    Type returnType = Type.getReturnType(desc);

    LocalVariablesSorter mv = new LocalVariablesSorter(access, desc, cv.visitMethod(access, name, desc, signature,
                                                                                    exceptions));

    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l1, l2, null);

    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
    int isManagedVar = mv.newLocal(Type.BOOLEAN_TYPE);
    mv.visitVarInsn(ISTORE, isManagedVar);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ILOAD, isManagedVar);
    mv.visitJumpInsn(IFEQ, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_FULLY_READLOCK_METHOD_NAME,
                       TC_FULLY_READLOCK_METHOD_DESC);
    mv.visitLabel(l0);

    mv.visitVarInsn(ALOAD, 0);
    for (int i = 0; i < params.length; i++) {
      mv.visitVarInsn(params[i].getOpcode(ILOAD), i + 1);
    }
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, getNewName(name), desc);
    int valueVar = mv.newLocal(returnType);
    mv.visitVarInsn(returnType.getOpcode(ISTORE), valueVar);

    mv.visitLabel(l1);
    mv.visitVarInsn(ILOAD, isManagedVar);
    Label l6 = new Label();
    mv.visitJumpInsn(IFEQ, l6);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_FULLY_READUNLOCK_METHOD_NAME,
                       TC_FULLY_READUNLOCK_METHOD_DESC);
    mv.visitLabel(l6);
    mv.visitVarInsn(returnType.getOpcode(ILOAD), valueVar);
    mv.visitInsn(returnType.getOpcode(IRETURN));

    mv.visitLabel(l2);
    int exceptionVar = mv.newLocal(Type.getObjectType("java/lang/Exception"));
    mv.visitVarInsn(ASTORE, exceptionVar);
    mv.visitVarInsn(ILOAD, isManagedVar);
    Label l9 = new Label();
    mv.visitJumpInsn(IFEQ, l9);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_FULLY_READUNLOCK_METHOD_NAME,
                       TC_FULLY_READUNLOCK_METHOD_DESC);
    mv.visitLabel(l9);
    mv.visitVarInsn(ALOAD, exceptionVar);
    mv.visitInsn(ATHROW);
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitLocalVariable("this", "Ljava/util/concurrent/ConcurrentHashMap;",
                          "Ljava/util/concurrent/ConcurrentHashMap<TK;TV;>;", l3, l11, 0);
    mv.visitLocalVariable("value", "Ljava/lang/Object;", null, l3, l11, valueVar);
    mv.visitLocalVariable("isManaged", "Z", null, l4, l11, isManagedVar);
    mv.visitMaxs(2, 5);
    mv.visitEnd();
  }

  private MethodVisitor rewriteSegmentForMethod(MethodVisitor mv) {
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
    return null;
  }

  private void createTCFullyReadLockMethod() {
    MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, TC_FULLY_READLOCK_METHOD_NAME, TC_FULLY_READLOCK_METHOD_DESC, null,
                                      null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, CONCURRENT_HASH_MAP_SLASH, "segments",
                      "[Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitVarInsn(ASTORE, 1);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 2);
    Label l2 = new Label();
    mv.visitLabel(l2);
    Label l3 = new Label();
    mv.visitJumpInsn(GOTO, l3);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitInsn(AALOAD);
    mv.visitMethodInsn(INVOKEVIRTUAL, JavaUtilConcurrentHashMapSegmentAdapter.CONCURRENT_HASH_MAP_SEGMENT_SLASH,
                       JavaUtilConcurrentHashMapSegmentAdapter.TC_READLOCK_METHOD_NAME,
                       JavaUtilConcurrentHashMapSegmentAdapter.TC_READLOCK_METHOD_DESC);
    mv.visitIincInsn(2, 1);
    mv.visitLabel(l3);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IF_ICMPLT, l4);
    Label l6 = new Label();
    mv.visitLabel(l6);
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

  private void createTCFullyReadUnlockMethod() {
    MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, TC_FULLY_READUNLOCK_METHOD_NAME, TC_FULLY_READUNLOCK_METHOD_DESC,
                                      null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, CONCURRENT_HASH_MAP_SLASH, "segments",
                      "[Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitVarInsn(ASTORE, 1);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 2);
    Label l2 = new Label();
    mv.visitLabel(l2);
    Label l3 = new Label();
    mv.visitJumpInsn(GOTO, l3);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitInsn(AALOAD);
    mv.visitMethodInsn(INVOKEVIRTUAL, JavaUtilConcurrentHashMapSegmentAdapter.CONCURRENT_HASH_MAP_SEGMENT_SLASH,
                       JavaUtilConcurrentHashMapSegmentAdapter.TC_READUNLOCK_METHOD_NAME,
                       JavaUtilConcurrentHashMapSegmentAdapter.TC_READUNLOCK_METHOD_DESC);
    mv.visitIincInsn(2, 1);
    mv.visitLabel(l3);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IF_ICMPLT, l4);
    Label l6 = new Label();
    mv.visitLabel(l6);
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

  private void createTCRehashAndSupportMethods() {
    createTCRehashMethod();
    createTCClearMethod();
  }

  private void createTCRehashMethod() {
    int access = ACC_PUBLIC + ACC_SYNTHETIC;
    String name = TC_REHASH_METHOD_NAME;
    String desc = TC_REHASH_METHOD_DESC;
    MethodVisitor mv = super.visitMethod(access, name, desc, null, null);
    mv = new JavaUtilConcurrentHashMapLazyValuesMethodAdapter(access, desc, mv, false);

    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    mv.visitTryCatchBlock(l0, l1, l1, "java/lang/Throwable");
    Label l2 = new Label();
    mv.visitTryCatchBlock(l0, l2, l2, null);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, CONCURRENT_HASH_MAP_SLASH, "size", "()I");
    Label l4 = new Label();
    mv.visitJumpInsn(IFLE, l4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_FULLY_READLOCK_METHOD_NAME,
                       TC_FULLY_READLOCK_METHOD_DESC);
    mv.visitLabel(l0);
    mv.visitTypeInsn(NEW, "java/util/ArrayList");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
    mv.visitVarInsn(ASTORE, 1);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 2);
    Label l7 = new Label();
    mv.visitLabel(l7);
    Label l8 = new Label();
    mv.visitJumpInsn(GOTO, l8);
    Label l9 = new Label();
    mv.visitLabel(l9);
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
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 4);
    Label l11 = new Label();
    mv.visitLabel(l11);
    Label l12 = new Label();
    mv.visitJumpInsn(GOTO, l12);
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitInsn(AALOAD);
    Label l14 = new Label();
    mv.visitJumpInsn(IFNULL, l14);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitInsn(AALOAD);
    mv.visitVarInsn(ASTORE, 5);
    Label l16 = new Label();
    mv.visitLabel(l16);
    Label l17 = new Label();
    mv.visitJumpInsn(GOTO, l17);
    Label l18 = new Label();
    mv.visitLabel(l18);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z");
    mv.visitInsn(POP);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "next",
                      "Ljava/util/concurrent/ConcurrentHashMap$HashEntry;");
    mv.visitVarInsn(ASTORE, 5);
    mv.visitLabel(l17);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitJumpInsn(IFNONNULL, l18);
    mv.visitLabel(l14);
    mv.visitIincInsn(4, 1);
    mv.visitLabel(l12);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IF_ICMPLT, l13);
    Label l20 = new Label();
    mv.visitLabel(l20);
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
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, CONCURRENT_HASH_MAP_SLASH, "__tc_clear", "()V");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;");
    mv.visitVarInsn(ASTORE, 2);
    Label l23 = new Label();
    mv.visitLabel(l23);
    Label l24 = new Label();
    mv.visitJumpInsn(GOTO, l24);
    Label l25 = new Label();
    mv.visitLabel(l25);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;");
    mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/ConcurrentHashMap$HashEntry");
    mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/ConcurrentHashMap$HashEntry");
    mv.visitVarInsn(ASTORE, 3);
    Label l26 = new Label();
    mv.visitLabel(l26);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "key", "Ljava/lang/Object;");
    mv.visitVarInsn(ASTORE, 4);
    Label l27 = new Label();
    mv.visitLabel(l27);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "value", "Ljava/lang/Object;");
    mv.visitVarInsn(ASTORE, 5);
    Label l28 = new Label();
    mv.visitLabel(l28);
    invokeJdkHashMethod(mv, 4);
    mv.visitVarInsn(ISTORE, 6);
    Label l29 = new Label();
    mv.visitLabel(l29);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_HASH_METHOD_NAME, TC_HASH_METHOD_DESC);
    mv.visitMethodInsn(INVOKEVIRTUAL, CONCURRENT_HASH_MAP_SLASH, "segmentFor",
                       "(I)Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ILOAD, 6);
    mv.visitVarInsn(ALOAD, 5);
    mv.visitInsn(ICONST_0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$Segment",
                       JavaUtilConcurrentHashMapSegmentAdapter.TC_PUT_METHOD_NAME,
                       JavaUtilConcurrentHashMapSegmentAdapter.TC_PUT_METHOD_DESC);
    mv.visitInsn(POP);
    mv.visitLabel(l24);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
    mv.visitJumpInsn(IFNE, l25);
    Label l30 = new Label();
    mv.visitLabel(l30);
    Label l31 = new Label();
    mv.visitJumpInsn(GOTO, l31);
    mv.visitLabel(l1);
    mv.visitVarInsn(ASTORE, 1);
    Label l32 = new Label();
    mv.visitLabel(l32);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "(Ljava/io/PrintStream;)V");
    Label l33 = new Label();
    mv.visitLabel(l33);
    mv.visitJumpInsn(GOTO, l31);
    mv.visitLabel(l2);
    mv.visitVarInsn(ASTORE, 7);
    Label l34 = new Label();
    mv.visitLabel(l34);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_FULLY_READUNLOCK_METHOD_NAME,
                       TC_FULLY_READUNLOCK_METHOD_DESC);
    mv.visitVarInsn(ALOAD, 7);
    mv.visitInsn(ATHROW);
    mv.visitLabel(l31);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_FULLY_READUNLOCK_METHOD_NAME,
                       TC_FULLY_READUNLOCK_METHOD_DESC);
    mv.visitLabel(l4);
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
    mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_HASH_METHOD_NAME, TC_HASH_METHOD_DESC);
    mv.visitMethodInsn(INVOKEVIRTUAL, CONCURRENT_HASH_MAP_SLASH, "segmentFor",
                       "(I)Ljava/util/concurrent/ConcurrentHashMap$Segment;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ICONST_0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$Segment",
                       JavaUtilConcurrentHashMapSegmentAdapter.TC_PUT_METHOD_NAME,
                       JavaUtilConcurrentHashMapSegmentAdapter.TC_PUT_METHOD_DESC);
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

  private static class SetWrapperMethodAdapter extends MethodAdapter implements Opcodes {
    private final String wrapperClass;
    private final String targetSet;
    
    public SetWrapperMethodAdapter(MethodVisitor mv, String targetSet, String wrapperClass) {
      super(mv);
      this.targetSet = targetSet;
      this.wrapperClass = wrapperClass;
    }
    
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      
      if (INVOKESPECIAL == opcode && targetSet.equals(owner) && "<init>".equals(name)
          && "(Ljava/util/concurrent/ConcurrentHashMap;)V".equals(desc)) {
        mv.visitVarInsn(ASTORE, 1);
        mv.visitTypeInsn(NEW, wrapperClass);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, wrapperClass, "<init>", "(Ljava/util/Map;Ljava/util/Set;)V");
      }
    }
  }
  
  private static class RetargetingMethodAdapter extends MethodAdapter implements Opcodes {
    private final String target;
    private final String replacement;
    private final String descriptor;
    
    private int matches;
    
    public RetargetingMethodAdapter(MethodVisitor mv, String target, String replacement, String descriptor) {
      super(mv);
      this.target = target;
      this.replacement = replacement;
      this.descriptor = descriptor;
      this.matches = 0;
    }
    
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if (INVOKEVIRTUAL == opcode && "java/util/concurrent/ConcurrentHashMap$Segment".equals(owner)
          && target.equals(name) && descriptor.equals(desc)) {
        // use the segment remove method that doesn't fault in the old values
        super.visitMethodInsn(opcode, owner, replacement, desc);
        matches++;
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }
    
    public void visitEnd() {
      super.visitEnd();
      Assert.assertTrue("Adapter not applied", matches != 0);
    }
  }

  private static class NoReturnRetargetingMethodAdapter extends RetargetingMethodAdapter {
    private int matches;
    
    public NoReturnRetargetingMethodAdapter(MethodVisitor mv, String target, String replacement, String descriptor) {
      super(mv, target, replacement, descriptor);
      this.matches = 0;
    }
    
    public void visitInsn(int opcode) {
      if (ARETURN == opcode) {
        // change the old value return to a void return and swallow the normally returned value
        super.visitInsn(POP);
        super.visitInsn(RETURN);
        matches++;
      } else {
        super.visitInsn(opcode);
      }
    }
    
    public void visitEnd() {
      super.visitEnd();
      Assert.assertTrue("Adapter not applied", matches != 0);      
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
      mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_IS_POSSIBLE_KEY_METHOD_NAME,
                         TC_IS_POSSIBLE_KEY_METHOD_DESC);
      Label l1 = new Label();
      mv.visitJumpInsn(IFNE, l1);
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
      mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_IS_POSSIBLE_KEY_METHOD_NAME,
                         TC_IS_POSSIBLE_KEY_METHOD_DESC);
      Label l1 = new Label();
      mv.visitJumpInsn(IFNE, l1);
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
      mv.visitMethodInsn(INVOKESPECIAL, CONCURRENT_HASH_MAP_SLASH, TC_IS_POSSIBLE_KEY_METHOD_NAME,
                         TC_IS_POSSIBLE_KEY_METHOD_DESC);
      Label l1 = new Label();
      mv.visitJumpInsn(IFNE, l1);
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

  private static class ConcurrentHashMapMethodAdapter extends MethodAdapter implements Opcodes {

    public ConcurrentHashMapMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if (INVOKEVIRTUAL == opcode && CONCURRENT_HASH_MAP_SLASH.equals(owner) && "segmentFor".equals(name)
          && "(I)Ljava/util/concurrent/ConcurrentHashMap$Segment;".equals(desc)) {
        super.visitInsn(POP);
        super.visitVarInsn(ALOAD, 0);
        super.visitVarInsn(ALOAD, 1);
        super.visitMethodInsn(INVOKEVIRTUAL, owner, TC_HASH_METHOD_NAME, TC_HASH_METHOD_DESC);
        super.visitMethodInsn(opcode, owner, name, desc);
      } else if (INVOKESPECIAL == opcode
                 && JavaUtilConcurrentHashMapSegmentAdapter.CONCURRENT_HASH_MAP_SEGMENT_SLASH.equals(owner)
                 && "<init>".equals(name) && "(IF)V".equals(desc)) {
        super.visitInsn(POP);
        super.visitInsn(POP);
        super.visitVarInsn(ALOAD, 0);
        super.visitVarInsn(ILOAD, 7);
        super.visitVarInsn(FLOAD, 2);
        super.visitMethodInsn(opcode, owner, name, JavaUtilConcurrentHashMapSegmentAdapter.INIT_DESC);
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }
  }

  private static class TurnIntoReadLocksMethodAdapter extends MethodAdapter implements Opcodes {

    public TurnIntoReadLocksMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if (INVOKEVIRTUAL == opcode
          && JavaUtilConcurrentHashMapSegmentAdapter.CONCURRENT_HASH_MAP_SEGMENT_SLASH.equals(owner)
          && "()V".equals(desc)) {
        if ("lock".equals(name)) {
          name = JavaUtilConcurrentHashMapSegmentAdapter.TC_READLOCK_METHOD_NAME;
        } else if ("unlock".equals(name)) {
          name = JavaUtilConcurrentHashMapSegmentAdapter.TC_READUNLOCK_METHOD_NAME;
        }
      }

      super.visitMethodInsn(opcode, owner, name, desc);
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