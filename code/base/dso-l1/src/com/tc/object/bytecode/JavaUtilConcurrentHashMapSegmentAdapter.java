/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.SerializationUtil;

public class JavaUtilConcurrentHashMapSegmentAdapter extends ClassAdapter implements Opcodes {
  private static final String PARENT_CONCURRENT_HASH_MAP_FIELD_TYPE = "Ljava/util/concurrent/ConcurrentHashMap;";
  
  private static final String PARENT_CONCURRENT_HASH_MAP_FIELD_NAME = "parentMap";

  private static final String TC_PUT_METHOD_NAME                    = ByteCodeUtil.TC_METHOD_PREFIX + "put";
  private static final String TC_PUT_METHOD_DESC                    = "(Ljava/lang/Object;ILjava/lang/Object;Z)Ljava/lang/Object;";

  private final static String TC_CLEAR_METHOD_NAME                  = ByteCodeUtil.TC_METHOD_PREFIX + "clear";
  private final static String TC_CLEAR_METHOD_DESC                  = "()V";

  public static final String  INITIAL_TABLE_METHOD_NAME             = "initTable";
  private static final String INITIAL_TABLE_METHOD_DESC             = "(I)V";

  public final static String  CONCURRENT_HASH_MAP_SEGMENT_SLASH     = "java/util/concurrent/ConcurrentHashMap$Segment";
  public final static String  INIT_DESC                             = "(" + PARENT_CONCURRENT_HASH_MAP_FIELD_TYPE
                                                                      + "IF)V";

  public JavaUtilConcurrentHashMapSegmentAdapter(ClassVisitor cv) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    String description = desc;
    if ("<init>".equals(name) && "(IF)V".equals(desc)) {
      description = INIT_DESC;
    }
    MethodVisitor mv = super.visitMethod(access, name, description, signature, exceptions);

    if ("put".equals(name) && "(Ljava/lang/Object;ILjava/lang/Object;Z)Ljava/lang/Object;".equals(desc)) {
      return new PutMethodAdapter(mv);
    } else if ("clear".equals(name) && "()V".equals(desc)) {
      return new ClearMethodAdapter(mv);
    } else if ("remove".equals(name) && "(Ljava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
      return new RemoveMethodAdapter(mv);
    } else if ("replace".equals(name) && "(Ljava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
      return new ReplaceMethodAdapter(mv);
    } else if ("replace".equals(name) && "(Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/Object;)Z".equals(desc)) {
      return new ReplaceIfValueEqualMethodAdapter(mv);
    } else if ("<init>".equals(name) && "(IF)V".equals(desc)) {
      return new InitMethodAdapter(mv);
    } else {
      return mv;
    }
  }

  public void visitEnd() {
    createDefaultConstructor();
    createInitTableMethod();
    createTCPutMethod();
    createTCClearMethod();
    super.visitField(ACC_FINAL + ACC_SYNTHETIC, PARENT_CONCURRENT_HASH_MAP_FIELD_NAME, "Ljava/util/concurrent/ConcurrentHashMap;", null, null);
    super.visitEnd();
  }
  
  private void createDefaultConstructor() {
    MethodVisitor mv = cv.visitMethod(0, "<init>", "()V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(232, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/locks/ReentrantLock", "<init>", "()V");
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLineNumber(233, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ICONST_1);
    mv.visitTypeInsn(ANEWARRAY, "java/util/concurrent/ConcurrentHashMap$HashEntry");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$Segment", "setTable", "([Ljava/util/concurrent/ConcurrentHashMap$HashEntry;)V");
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(234, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(FCONST_0);
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "loadFactor", "F");
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(235, l3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ACONST_NULL);
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "parentMap", "Ljava/util/concurrent/ConcurrentHashMap;");
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(236, l4);
    mv.visitInsn(RETURN);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLocalVariable("this", "Ljava/util/concurrent/ConcurrentHashMap$Segment;", "Ljava/util/concurrent/ConcurrentHashMap<TK;TV;>.Segment<TK;TV;>;", l0, l5, 0);
    mv.visitMaxs(2, 1);
    mv.visitEnd();
  }

  // This is the identical copy of the put() method of Segments except that it does not lock and does
  // not invoke the instrumented version of the logicalInvoke(). The reason that it does not require
  // locking is because it is called from the __tc_rehash() instrumented method and the lock is grabed
  // at the __tc_rehash() method already.
  private void createTCPutMethod() {
    MethodVisitor mv = super.visitMethod(0 + ACC_SYNTHETIC, TC_PUT_METHOD_NAME, TC_PUT_METHOD_DESC, null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "count", "I");
    mv.visitVarInsn(ISTORE, 5);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitVarInsn(ILOAD, 5);
    mv.visitIincInsn(5, 1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "threshold", "I");
    Label l2 = new Label();
    mv.visitJumpInsn(IF_ICMPLE, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$Segment", "rehash", "()V");
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "table",
                      "[Ljava/util/concurrent/ConcurrentHashMap$HashEntry;");
    mv.visitVarInsn(ASTORE, 6);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 6);
    mv.visitInsn(ARRAYLENGTH);
    mv.visitInsn(ICONST_1);
    mv.visitInsn(ISUB);
    mv.visitInsn(IAND);
    mv.visitVarInsn(ISTORE, 7);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 6);
    mv.visitVarInsn(ILOAD, 7);
    mv.visitInsn(AALOAD);
    mv.visitVarInsn(ASTORE, 8);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitVarInsn(ALOAD, 8);
    mv.visitVarInsn(ASTORE, 9);
    Label l6 = new Label();
    mv.visitLabel(l6);
    Label l7 = new Label();
    mv.visitJumpInsn(GOTO, l7);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitVarInsn(ALOAD, 9);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "next",
                      "Ljava/util/concurrent/ConcurrentHashMap$HashEntry;");
    mv.visitVarInsn(ASTORE, 9);
    mv.visitLabel(l7);
    mv.visitVarInsn(ALOAD, 9);
    Label l9 = new Label();
    mv.visitJumpInsn(IFNULL, l9);
    mv.visitVarInsn(ALOAD, 9);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "hash", "I");
    mv.visitVarInsn(ILOAD, 2);
    mv.visitJumpInsn(IF_ICMPNE, l8);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 9);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "key", "Ljava/lang/Object;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z");
    mv.visitJumpInsn(IFEQ, l8);
    mv.visitLabel(l9);
    mv.visitVarInsn(ALOAD, 9);
    Label l10 = new Label();
    mv.visitJumpInsn(IFNULL, l10);
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitVarInsn(ALOAD, 9);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "value", "Ljava/lang/Object;");
    mv.visitVarInsn(ASTORE, 10);
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitVarInsn(ILOAD, 4);
    Label l13 = new Label();
    mv.visitJumpInsn(IFNE, l13);
    Label l14 = new Label();
    mv.visitLabel(l14);
    mv.visitVarInsn(ALOAD, 9);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "value", "Ljava/lang/Object;");
    mv.visitJumpInsn(GOTO, l13);
    mv.visitLabel(l10);
    mv.visitInsn(ACONST_NULL);
    mv.visitVarInsn(ASTORE, 10);
    Label l15 = new Label();
    mv.visitLabel(l15);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(DUP);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "modCount", "I");
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IADD);
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "modCount", "I");
    Label l16 = new Label();
    mv.visitLabel(l16);
    mv.visitVarInsn(ALOAD, 6);
    mv.visitVarInsn(ILOAD, 7);
    mv.visitTypeInsn(NEW, "java/util/concurrent/ConcurrentHashMap$HashEntry");
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 8);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/ConcurrentHashMap$HashEntry", "<init>",
                       "(Ljava/lang/Object;ILjava/util/concurrent/ConcurrentHashMap$HashEntry;Ljava/lang/Object;)V");
    mv.visitInsn(AASTORE);
    Label l17 = new Label();
    mv.visitLabel(l17);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ILOAD, 5);
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "count", "I");
    mv.visitLabel(l13);
    mv.visitVarInsn(ALOAD, 10);
    mv.visitInsn(ARETURN);
    Label l18 = new Label();
    mv.visitLabel(l18);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  // Again, this method does not require lock as it is called by __tc_rehash() which grabs the
  // lock already.
  private void createTCClearMethod() {
    MethodVisitor mv = super.visitMethod(0, TC_CLEAR_METHOD_NAME, TC_CLEAR_METHOD_DESC, null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "count", "I");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "table",
                      "[Ljava/util/concurrent/ConcurrentHashMap$HashEntry;");
    mv.visitVarInsn(ASTORE, 1);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, 2);
    Label l4 = new Label();
    mv.visitLabel(l4);
    Label l5 = new Label();
    mv.visitJumpInsn(GOTO, l5);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitInsn(ACONST_NULL);
    mv.visitInsn(AASTORE);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitIincInsn(2, 1);
    mv.visitLabel(l5);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IF_ICMPLT, l6);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(DUP);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "modCount", "I");
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IADD);
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "modCount", "I");
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ICONST_0);
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", "count", "I");
    mv.visitLabel(l1);
    mv.visitInsn(RETURN);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void createInitTableMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE + ACC_FINAL + ACC_SYNTHETIC, INITIAL_TABLE_METHOD_NAME,
                                         INITIAL_TABLE_METHOD_DESC, null, null);
    mv.visitCode();
    ByteCodeUtil.pushThis(mv);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitTypeInsn(ANEWARRAY, "java/util/concurrent/ConcurrentHashMap$HashEntry");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/ConcurrentHashMap$Segment", "setTable",
                       "([Ljava/util/concurrent/ConcurrentHashMap$HashEntry;)V");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private static class InitMethodAdapter extends MethodAdapter implements Opcodes {
    public InitMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (RETURN == opcode) {
        // ByteCodeUtil.pushThis(mv);
        // mv.visitVarInsn(ALOAD, 1);
        // mv.visitFieldInsn(PUTFIELD, CONCURRENT_HASH_MAP_SEGMENT_SLASH, INITIAL_CAPACITY_FIELD_NAME,
        // INITIAL_CAPACITY_FIELD_TYPE);

        ByteCodeUtil.pushThis(mv);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", PARENT_CONCURRENT_HASH_MAP_FIELD_NAME,
                          "Ljava/util/concurrent/ConcurrentHashMap;");

      }
      super.visitInsn(opcode);
    }

    public void visitVarInsn(int opcode, int var) {
      if (var == 1) {
        var = 2;
      } else if (var == 2) {
        var = 3;
      }
      super.visitVarInsn(opcode, var);
    }
  }
  
  private static class PutMethodAdapter extends MethodAdapter implements Opcodes {

    public PutMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (PUTFIELD == opcode && "java/util/concurrent/ConcurrentHashMap$HashEntry".equals(owner)
          && "value".equals(name) && "Ljava/lang/Object;".equals(desc)) {
        addFoundLogicalInvokePutMethodCall();
      } else if (PUTFIELD == opcode && "java/util/concurrent/ConcurrentHashMap$Segment".equals(owner)
                 && "count".equals(name) && "I".equals(desc)) {
        addNotFoundLogicalInvokePutMethodCall();
      }
    }

    private void addNotFoundLogicalInvokePutMethodCall() {
      Label endBlock = new Label();
      Label logicalInvokeLabel = new Label();
      
      mv.visitLabel(logicalInvokeLabel);
      
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", PARENT_CONCURRENT_HASH_MAP_FIELD_NAME,
                        "Ljava/util/concurrent/ConcurrentHashMap;");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
      mv.visitJumpInsn(IFEQ, endBlock);
      mv.visitVarInsn(ILOAD, 4);
      Label l0 = new Label();
      mv.visitJumpInsn(IFEQ, l0);
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", PARENT_CONCURRENT_HASH_MAP_FIELD_NAME,
                        "Ljava/util/concurrent/ConcurrentHashMap;");
      mv.visitLdcInsn(SerializationUtil.PUT_SIGNATURE);
      mv.visitInsn(ICONST_2);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitJumpInsn(GOTO, endBlock);
      mv.visitLabel(l0);
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", PARENT_CONCURRENT_HASH_MAP_FIELD_NAME,
                        "Ljava/util/concurrent/ConcurrentHashMap;");
      mv.visitLdcInsn(SerializationUtil.PUT_SIGNATURE);
      mv.visitInsn(ICONST_2);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(endBlock);
    }

    private void addFoundLogicalInvokePutMethodCall() {
      Label notManaged = new Label();
      Label logicalInvokeLabel = new Label();
      
      mv.visitLabel(logicalInvokeLabel);
      
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", PARENT_CONCURRENT_HASH_MAP_FIELD_NAME,
                        "Ljava/util/concurrent/ConcurrentHashMap;");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
      mv.visitJumpInsn(IFEQ, notManaged);
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", PARENT_CONCURRENT_HASH_MAP_FIELD_NAME,
                        "Ljava/util/concurrent/ConcurrentHashMap;");
      mv.visitLdcInsn(SerializationUtil.PUT_SIGNATURE);
      mv.visitInsn(ICONST_2);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 9);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "key", "Ljava/lang/Object;");
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(notManaged);
    }
  }

  private static class ReplaceMethodAdapter extends MethodAdapter implements Opcodes {

    public ReplaceMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (PUTFIELD == opcode && "java/util/concurrent/ConcurrentHashMap$HashEntry".equals(owner)
          && "value".equals(name) && "Ljava/lang/Object;".equals(desc)) {
        addLogicalInvokeReplaceMethodCall();
      }
    }

    public void addLogicalInvokeReplaceMethodCall() {
      Label notManaged = new Label();
      ByteCodeUtil.pushThis(this);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", PARENT_CONCURRENT_HASH_MAP_FIELD_NAME,
                        "Ljava/util/concurrent/ConcurrentHashMap;");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
      mv.visitJumpInsn(IFEQ, notManaged);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", PARENT_CONCURRENT_HASH_MAP_FIELD_NAME,
                        "Ljava/util/concurrent/ConcurrentHashMap;");
      mv.visitLdcInsn(SerializationUtil.PUT_SIGNATURE);
      mv.visitInsn(ICONST_2);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 4);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "key", "Ljava/lang/Object;");
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(notManaged);
    }
  }

  private static class ReplaceIfValueEqualMethodAdapter extends MethodAdapter implements Opcodes {

    public ReplaceIfValueEqualMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (PUTFIELD == opcode && "java/util/concurrent/ConcurrentHashMap$HashEntry".equals(owner)
          && "value".equals(name) && "Ljava/lang/Object;".equals(desc)) {
        addLogicalInvokeReplaceMethodCall();
      }
      super.visitFieldInsn(opcode, owner, name, desc);
    }

    public void addLogicalInvokeReplaceMethodCall() {
      Label notManaged = new Label();
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", PARENT_CONCURRENT_HASH_MAP_FIELD_NAME,
                        "Ljava/util/concurrent/ConcurrentHashMap;");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
      mv.visitJumpInsn(IFEQ, notManaged);
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", PARENT_CONCURRENT_HASH_MAP_FIELD_NAME,
                        "Ljava/util/concurrent/ConcurrentHashMap;");
      mv.visitLdcInsn(SerializationUtil.PUT_SIGNATURE);
      mv.visitInsn(ICONST_2);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 5);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "key", "Ljava/lang/Object;");
      mv.visitInsn(AASTORE);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      mv.visitVarInsn(ALOAD, 4);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(notManaged);
    }
  }

  private static class RemoveMethodAdapter extends MethodAdapter implements Opcodes {

    public RemoveMethodAdapter(MethodVisitor mv) {
      super(mv);
    }
    /*
    public void visitCode() {
      super.visitCode();
      Label l0 = new Label();
      mv.visitFieldInsn(GETSTATIC, "com/tc/util/DebugUtil", "DEBUG", "Z");
      mv.visitJumpInsn(IFEQ, l0);
      Label l5 = new Label();
      mv.visitLabel(l5);
      mv.visitLineNumber(479, l5);
      mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
      mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
      mv.visitInsn(DUP);
      mv.visitLdcInsn("Segment.remove: client id: ");
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "getClientID", "()Ljava/lang/String;");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
      mv.visitLdcInsn(", key: ");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
      mv.visitLdcInsn(", hash: ");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
      Label l6 = new Label();
      mv.visitLabel(l6);
      mv.visitLineNumber(480, l6);
      mv.visitVarInsn(ILOAD, 2);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
      Label l7 = new Label();
      mv.visitLabel(l7);
      mv.visitLineNumber(479, l7);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
      mv.visitLabel(l0);
    }
  */
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (PUTFIELD == opcode && "java/util/concurrent/ConcurrentHashMap$Segment".equals(owner) && "count".equals(name)
          && "I".equals(desc)) {
        addLogicalInvokeRemoveMethodCall();
      }
    }

    public void addLogicalInvokeRemoveMethodCall() {
      Label notManaged = new Label();
      Label logicalInvokeLabel = new Label();
      
      mv.visitLabel(logicalInvokeLabel);
      
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", PARENT_CONCURRENT_HASH_MAP_FIELD_NAME,
                        "Ljava/util/concurrent/ConcurrentHashMap;");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
      mv.visitJumpInsn(IFEQ, notManaged);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", PARENT_CONCURRENT_HASH_MAP_FIELD_NAME,
                        "Ljava/util/concurrent/ConcurrentHashMap;");
      mv.visitLdcInsn(SerializationUtil.REMOVE_SIGNATURE);
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 8);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$HashEntry", "key", "Ljava/lang/Object;");
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(notManaged);
    }
  }

  private static class ClearMethodAdapter extends MethodAdapter implements Opcodes {

    public ClearMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if ("lock".equals(name) && "()V".equals(desc)) {
        addLogicalInvokeMethodCall();
      }
    }

    public void addLogicalInvokeMethodCall() {
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/ConcurrentHashMap$Segment", PARENT_CONCURRENT_HASH_MAP_FIELD_NAME,
                        "Ljava/util/concurrent/ConcurrentHashMap;");
      mv.visitLdcInsn(SerializationUtil.CLEAR_SIGNATURE);

      mv.visitLdcInsn(new Integer(0));
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
    }
  }
}