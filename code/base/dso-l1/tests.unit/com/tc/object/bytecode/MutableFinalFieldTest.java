/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import org.apache.commons.io.IOUtils;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassReader;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.process.Exec;
import com.tc.process.Exec.Result;
import com.tc.test.TCTestCase;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * This test is to get the monkeys to run this on all of the VM and OS combinations that we bless. Essentially it
 * verifies that we can in fact leave the final modifier in classes, but generate methods that mutate those final
 * fields. TC instrumentation will make mutations of final fields in the various __tc_get<field> methods that do
 * lazy field resolving in physical objects
 */
public class MutableFinalFieldTest extends TCTestCase {

  public void testFinalFields() throws Exception {
    ClassReader reader = new ClassReader(FinalFieldType.class.getName());
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    Adapter fooAdapter = new Adapter(writer);
    reader.accept(fooAdapter, ClassReader.SKIP_FRAMES);

    File tempDir = getTempDirectory();
    File classDir = new File(tempDir, FinalFieldType.class.getPackage().getName().replace('.', File.separatorChar));

    boolean created = classDir.mkdirs();
    if (!created) { throw new IOException("Could not create " + classDir.getAbsolutePath()); }

    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(new File(classDir, FinalFieldType.class.getSimpleName().concat(".class")));
      fos.write(writer.toByteArray());
      fos.close();
    } finally {
      IOUtils.closeQuietly(fos);
    }

    String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
                  + (Os.isWindows() ? ".exe" : "");

    List<String> cmd = new ArrayList<String>();
    cmd.add(java);
    cmd.add("-Xfuture");
    cmd.add("-Xverify:all");
    cmd.add("-cp");
    cmd.add(tempDir.getAbsolutePath());
    cmd.add(FinalFieldType.class.getName());

    Result result = Exec.execute(cmd.toArray(new String[cmd.size()]));

    System.err.println(result);

    if (!result.getStdout().trim().equals("(null) (mutated)")) { throw new AssertionError(result.toString()); }
  }

  private static class Adapter extends ClassAdapter implements Opcodes {

    public Adapter(ClassVisitor cv) {
      super(cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

      if ("mutate".equals(name)) {
        // This new method body simulates what a lazy field resolver will do
        // in TC instrumented physical objects (ie. mutate a final field)
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, FinalFieldType.class.getName().replace('.', '/'), "ref", "Ljava/lang/Object;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return null;
      }

      return mv;
    }
  }
}

class FinalFieldType {
  private final Object ref = null;

  public void mutate(Object value) {
    // filled in by class adapter
  }

  public Object getRef() {
    return ref;
  }

  @Override
  public String toString() {
    return "(" + ref + ")";
  }

  public static void main(String[] args) {
    try {
      String output = "";
      FinalFieldType m = new FinalFieldType();
      Object val = m.getRef();
      if (null != val) {
        throw new AssertionError("unexpected initial value: " + val);
      }

      verifyFinalFields(m.getClass());

      output += m + " ";
      m.mutate("mutated");

      val = m.getRef();
      if (! "mutated".equals(val)) {
        throw new AssertionError("unexpected value: " + val);
      }

      output += m;
      System.out.println(output);
    } catch (Throwable t) {
      t.printStackTrace(new PrintStream(System.out));
      System.exit(1);
    }
    System.exit(0);
  }

  private static void verifyFinalFields(Class c) throws Exception {
    for (Field f : c.getDeclaredFields()) {
      if (!Modifier.isFinal(f.getModifiers())) { throw new AssertionError("Field is not final: " + f); }
    }
  }
}
