/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode.hook;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassReader;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Instruments the java.lang.ClassLoader to plug in the Class PreProcessor mechanism.
 * <p/>
 * We are using a lazy initialization of the class preprocessor to allow all class pre processor logic to be in system
 * classpath and not in bootclasspath.
 * <p/>
 * This implementation should support IBM custom JRE
 */
public class ClassLoaderPreProcessorImpl {

  private final static String CLASSLOADER_CLASS_NAME   = "java/lang/ClassLoader";
  private final static String DEFINECLASS0_METHOD_NAME = "defineClass0";

  // For JDK5
  private final static String DEFINECLASS1_METHOD_NAME = "defineClass1";
  private final static String DEFINECLASS2_METHOD_NAME = "defineClass2";

  private static final String DESC_CORE                = "Ljava/lang/String;[BIILjava/security/ProtectionDomain;";
  private static final String DESC_PREFIX              = "(" + DESC_CORE;
  private static final String DESC_HELPER              = "(Ljava/lang/ClassLoader;" + DESC_CORE + ")[B";

  private static final String DESC_BYTEBUFFER_CORE     = "Ljava/lang/String;Ljava/nio/ByteBuffer;IILjava/security/ProtectionDomain;";
  private static final String DESC_BYTEBUFFER_PREFIX   = "(" + DESC_BYTEBUFFER_CORE;
  private static final String DESC_BYTEBUFFER_HELPER   = "(Ljava/lang/ClassLoader;" + DESC_BYTEBUFFER_CORE
                                                         + ")Ljava/nio/ByteBuffer;";

  public ClassLoaderPreProcessorImpl() {
    //
  }

  /**
   * Patch caller side of defineClass0
   * 
   * <pre>
   * byte[] weaved = ..hook.impl.ClassPreProcessorHelper.defineClass0Pre(this, args..);
   * klass = defineClass0(name, weaved, 0, weaved.length, protectionDomain);
   * </pre>
   * 
   * @param classLoaderBytecode
   * @return
   */
  public byte[] preProcess(byte[] classLoaderBytecode) {
    try {
      ClassReader cr = new ClassReader(classLoaderBytecode);
      ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      ClassVisitor cv = new ClassLoaderVisitor(cw);
      cr.accept(cv, ClassReader.SKIP_FRAMES);
      return cw.toByteArray();
    } catch (Exception e) {
      System.err.println("failed to patch ClassLoader:");
      e.printStackTrace();
      return classLoaderBytecode;
    }
  }

  private static class ClassLoaderVisitor extends ClassAdapter {
    private String  className;

    private boolean getClassLoadingLockExists = false;

    public ClassLoaderVisitor(ClassVisitor cv) {
      super(cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      super.visit(version, access, name, signature, superName, interfaces);
      this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
      if (CLASSLOADER_CLASS_NAME.equals(className) && "(Ljava/lang/String;)Ljava/lang/Object;".equals(desc)
          && "getClassLoadingLock".equals(name)) {
        getClassLoadingLockExists = true;
      }

      if ("initSystemClassLoader".equals(name)) {
        return new SclSetAdapter(mv);
      } else {
        return new ProcessingVisitor(mv, access, desc);
      }
    }

    @Override
    public void visitEnd() {
      /*
       * __tc_getClassLoadingLock(String className) is the method used to determine what to lock on when finding and
       * loading TC exported classes. If the new parallel class load enabling method getClassLoadingLock(String name)
       * exists then we simply delegate to it. Otherwise we return 'this' and get the old behavior of locking on the
       * classloader.
       */
      MethodVisitor mv = super.visitMethod(Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
                                           "__tc_getClassLoadingLock", "(Ljava/lang/String;)Ljava/lang/Object;", null,
                                           null);
      mv.visitCode();
      if (getClassLoadingLockExists) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ClassLoader", "getClassLoadingLock",
                           "(Ljava/lang/String;)Ljava/lang/Object;");
        mv.visitInsn(Opcodes.ARETURN);
      } else {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.ARETURN);
      }
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }
  }

  private static class SclSetAdapter extends MethodAdapter implements Opcodes {

    public SclSetAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);

      if ("sclSet".equals(name) && (PUTSTATIC == opcode)) {
        // The ClassLoader class sets a flag when it's finished setting the
        // system classloader, however this doesn't mean that the system
        // classloader has really been set since the same code path might
        // be followed by nested calls.
        // Inside the initSystemClassLoader method of ClassLoader, it calls out
        // to the sun.misc.Launcher class which can be looking up customer
        // protocol handlers through the URLs that are being used by the
        // ExtClassLoader. At that time, the system classloader is not yet
        // set, causing the system classloader to be null during the actual
        // initialization the system classloader itself. The flag itself
        // is set however, which can cause DSO to think that the initialization
        // was actually finished and the system classloader was set.
        // By checking that the system classloader is actually not null, we
        // make sure that during those nested calls, the DSO initialization
        // process doesn't blow up.
        //
        // If you have trouble to follow what happens, here's a structural
        // overview:
        //
        // ClassLoader.getSystemClassLoader (1st time, scl is null)
        // -> ClassLoader.initSystemClassLoader
        // -> sclSet is false, thus continues
        // -> Launcher class init
        // -> Launcher instance init
        // -> Launcher.getLauncher
        // -> Launcher.ExtClassLoader.getExtURLs
        // -> URL.getURLStreamHandler
        // -> ClassLoader.getSystemClassLoader (2nd time, scl is null)
        // -> ClassLoader.initSystemClassLoader (sclSet is false)
        // -> launcher.getClassLoader() returns null
        // -> scl is set to null
        // -> sclSet is set to true <=== this is where the extra check is needed
        // -> Launcher continues its initialization
        // -> launcher.getClassLoader() returns classloader
        // -> scl is set to the classloader instance
        // -> sclSet is set to true <=== this is where we want to init our stuff
        Label l = new Label();
        super.visitFieldInsn(GETSTATIC, owner, "scl", "Ljava/lang/ClassLoader;");
        super.visitJumpInsn(IFNULL, l);
        super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper",
                              "systemLoaderInitialized", "()V");
        super.visitLabel(l);
      }
    }
  }

  /**
   * Wraps calls to defineClass0, defineClass1 and <code>defineClass2</code> methods:
   * 
   * <pre>
   * byte[] newbytes = ClassProcessorHelper.defineClass0Pre(loader, name, b, off, len, pd);
   * if (b == newbytes) {
   *   defineClass0(loader, name, b, off, len, pd);
   * } else {
   *   defineClass0(loader, name, newbytes, off, newbytes.length, pd);
   * }
   * </pre>
   */
  private static class ProcessingVisitor extends RemappingMethodVisitor {
    public ProcessingVisitor(MethodVisitor cv, int access, String desc) {
      super(cv, access, desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      boolean insertPostCall = false;

      if ((DEFINECLASS0_METHOD_NAME.equals(name) || (DEFINECLASS1_METHOD_NAME.equals(name)))
          && CLASSLOADER_CLASS_NAME.equals(owner)) {
        insertPostCall = true;
        wrapCallToDefineClass01(opcode, owner, name, desc);
      } else if (DEFINECLASS2_METHOD_NAME.equals(name) && CLASSLOADER_CLASS_NAME.equals(owner)) {
        insertPostCall = true;
        wrapCallToDefineClass2(opcode, owner, name, desc);
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }

      if (insertPostCall) {
        super.visitInsn(Opcodes.DUP);
        super.visitVarInsn(Opcodes.ALOAD, 0);
        // The newly defined class object should be on the stack at this point
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper",
                              "defineClass0Post", "(Ljava/lang/Class;Ljava/lang/ClassLoader;)V");
      }
    }

    private void wrapCallToDefineClass01(int opcode, String owner, String name, String desc) throws Error {
      Type[] args = Type.getArgumentTypes(desc);
      if (args.length < 5 || !desc.startsWith(DESC_PREFIX)) { //
        throw new Error("non supported JDK, native call not supported: " + desc);
      }

      // store all args in local variables
      int[] locals = new int[args.length];
      for (int i = args.length - 1; i >= 0; i--) {
        mv.visitVarInsn(args[i].getOpcode(Opcodes.ISTORE), locals[i] = nextLocal(args[i].getSize()));
      }
      for (int i = 0; i < 5; i++) {
        mv.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), locals[i]);
      }
      super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper",
                            "defineClass0Pre", DESC_HELPER);
      int returnLocalByteArray = nextLocal(args[1].getSize());
      mv.visitVarInsn(Opcodes.ASTORE, returnLocalByteArray);
      mv.visitVarInsn(Opcodes.ALOAD, locals[1]); // bytes
      mv.visitVarInsn(Opcodes.ALOAD, returnLocalByteArray); // new bytes
      Label l1 = new Label();
      mv.visitJumpInsn(Opcodes.IF_ACMPEQ, l1);
      /*
       * If the return array is same as the input array, then there was no instrumentation done to the class. So
       * maintain the offsets
       */
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitVarInsn(Opcodes.ALOAD, locals[0]); // name
      mv.visitVarInsn(Opcodes.ALOAD, returnLocalByteArray); // instrumented bytes
      mv.visitInsn(Opcodes.ICONST_0); // offset
      mv.visitVarInsn(Opcodes.ALOAD, returnLocalByteArray);
      mv.visitInsn(Opcodes.ARRAYLENGTH); // length
      mv.visitVarInsn(Opcodes.ALOAD, locals[4]); // protection domain
      for (int i = 5; i < args.length; i++) {
        mv.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), locals[i]);
      }
      super.visitMethodInsn(opcode, owner, name, desc);
      Label l2 = new Label();
      mv.visitJumpInsn(Opcodes.GOTO, l2);
      mv.visitLabel(l1);
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      for (int i = 0; i < args.length; i++) {
        mv.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), locals[i]);
      }
      super.visitMethodInsn(opcode, owner, name, desc);
      mv.visitLabel(l2);
    }

    private void wrapCallToDefineClass2(int opcode, String owner, String name, String desc) throws Error {
      Type[] args = Type.getArgumentTypes(desc);
      if (args.length < 5 || !desc.startsWith(DESC_BYTEBUFFER_PREFIX)) { //
        throw new Error("non supported JDK, bytebuffer native call not supported: " + desc);
      }
      // store all args in local variables
      int[] locals = new int[args.length];
      for (int i = args.length - 1; i >= 0; i--) {
        mv.visitVarInsn(args[i].getOpcode(Opcodes.ISTORE), locals[i] = nextLocal(args[i].getSize()));
      }
      for (int i = 0; i < 5; i++) {
        mv.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), locals[i]);
      }
      super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelperJDK15",
                            "defineClass0Pre", DESC_BYTEBUFFER_HELPER);
      mv.visitVarInsn(Opcodes.ASTORE, locals[1]);
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitVarInsn(Opcodes.ALOAD, locals[0]); // name
      mv.visitVarInsn(Opcodes.ALOAD, locals[1]); // bytes
      mv.visitInsn(Opcodes.ICONST_0); // offset
      mv.visitVarInsn(Opcodes.ALOAD, locals[1]);
      mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "remaining", "()I");
      mv.visitVarInsn(Opcodes.ALOAD, locals[4]); // protection domain
      for (int i = 5; i < args.length; i++) {
        mv.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), locals[i]);
      }
      super.visitMethodInsn(opcode, owner, name, desc);
    }
  }

  // TODO replace this with LocalVariableSorterAdapter
  private static class RemappingMethodVisitor extends MethodAdapter {
    private final State  state;
    private final IntRef check = new IntRef();

    public RemappingMethodVisitor(MethodVisitor v, int access, String desc) {
      super(v);
      state = new State(access, Type.getArgumentTypes(desc));
    }

    public RemappingMethodVisitor(RemappingMethodVisitor wrap) {
      super(wrap.mv);
      this.state = wrap.state;
    }

    protected int nextLocal(int size) {
      int var = state.nextLocal;
      state.nextLocal += size;
      return var;
    }

    private int remap(int var, int size) {
      if (var < state.firstLocal) { return var; }
      check.key = (size == 2) ? ~var : var;
      Integer value = (Integer) state.locals.get(check);
      if (value == null) {
        IntRef ref = new IntRef();
        ref.key = check.key;
        state.locals.put(ref, value = Integer.valueOf(nextLocal(size)));
      }
      return value.intValue();
    }

    @Override
    public void visitIincInsn(int var, int increment) {
      mv.visitIincInsn(remap(var, 1), increment);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
      mv.visitLocalVariable(name, desc, signature, start, end, remap(index, 0));
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
      int size;
      switch (opcode) {
        case Opcodes.LLOAD:
        case Opcodes.LSTORE:
        case Opcodes.DLOAD:
        case Opcodes.DSTORE:
          size = 2;
          break;
        default:
          size = 1;
      }
      mv.visitVarInsn(opcode, remap(var, size));
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
      mv.visitMaxs(0, 0);
    }
  }

  private static class State {
    Map locals = new HashMap();
    int firstLocal;
    int nextLocal;

    State(int access, Type[] args) {
      nextLocal = ((Opcodes.ACC_STATIC & access) != 0) ? 0 : 1;
      for (Type arg : args) {
        nextLocal += arg.getSize();
      }
      firstLocal = nextLocal;
    }
  }

  private static class IntRef {
    int key;

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      } else if (!(o instanceof IntRef)) {
        return false;
      } else {
        return key == ((IntRef) o).key;
      }
    }

    @Override
    public int hashCode() {
      return key;
    }
  }

}
