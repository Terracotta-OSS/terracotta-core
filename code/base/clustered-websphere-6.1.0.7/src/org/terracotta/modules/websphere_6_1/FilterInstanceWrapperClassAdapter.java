package org.terracotta.modules.websphere_6_1;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class FilterInstanceWrapperClassAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {
  public FilterInstanceWrapperClassAdapter() {
    super(null);
  }

  public FilterInstanceWrapperClassAdapter(ClassVisitor cv) {
    super(cv);
  }
  
  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new FilterInstanceWrapperClassAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (name.equals("<init>")) {
      mv = new InitMethodAdapter(mv);
    }

    return mv;
  }

  private static class InitMethodAdapter extends MethodAdapter implements Opcodes {
    public InitMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (opcode == RETURN) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("In FilterInstanceWrapper: ");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
      }
      super.visitInsn(opcode);
    }
  }

}
