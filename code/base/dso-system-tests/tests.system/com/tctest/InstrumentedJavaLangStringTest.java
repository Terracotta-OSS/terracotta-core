/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassReader;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.commons.EmptyVisitor;
import com.tc.object.bytecode.hook.impl.JavaLangArrayHelpers;
import com.tc.test.TCTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class InstrumentedJavaLangStringTest extends TCTestCase {

  public InstrumentedJavaLangStringTest() {
    //
  }

  public void testGetBytes() {
    String s = "Timmy Teck";
    byte[] b = new byte[4];
    s.getBytes(6, 10, b, 0);
    assertEquals((byte) 'T', b[0]);
    assertEquals((byte) 'e', b[1]);
    assertEquals((byte) 'c', b[2]);
    assertEquals((byte) 'k', b[3]);
  }

  public void testGetChars() {
    String s = "Timmy Teck";
    char[] c = new char[s.length() + 1];
    s.getChars(0, 5, c, 2);
    assertEquals((char) 0, c[0]);
    assertEquals((char) 0, c[1]);
    assertEquals('T', c[2]);
    assertEquals('i', c[3]);
    assertEquals('m', c[4]);
    assertEquals('m', c[5]);
    assertEquals('y', c[6]);

    try {
      s.getChars(0, 15, c, 0);
      fail();
    } catch (StringIndexOutOfBoundsException e) {
      // expected
    }

    try {
      s.getChars(-1, 3, c, 0);
      fail();
    } catch (StringIndexOutOfBoundsException e) {
      // expected
    }

    try {
      s.getChars(3, 1, c, 0);
      fail();
    } catch (StringIndexOutOfBoundsException e) {
      // expected
    }

  }

  public void testStringIsInstrumented() throws IOException {
    ClassReader reader = new ClassReader(getStringBytes());
    StringVisitor visitor = new StringVisitor(new EmptyVisitor());
    reader.accept(visitor, ClassReader.SKIP_FRAMES);

    assertEquals(visitor.verified.toString(), 2, visitor.verified.size());
    assertTrue(visitor.verified.contains("getBytes"));
    assertTrue(visitor.verified.contains("getChars"));
  }

  private byte[] getStringBytes() throws IOException {
    InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(
                                                                            String.class.getName().replace('.', '/')
                                                                                .concat(".class"));
    return IOUtils.toByteArray(is);
  }

  private static class StringVisitor extends ClassAdapter {

    private final Set verified = new HashSet();

    public StringVisitor(ClassVisitor cv) {
      super(cv);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      if ((name.equals("getChars") && "(II[CI)V".equals(desc)) || (name.equals("getBytes") && desc.equals("(II[BI)V"))) {
        // make formatter sane
        return new VerifyArrayManagerAccess(super.visitMethod(access, name, desc, signature, exceptions), name);
      }
      return null;
    }

    private class VerifyArrayManagerAccess extends MethodAdapter {

      private final String method;

      public VerifyArrayManagerAccess(MethodVisitor mv, String method) {
        super(mv);
        this.method = method;
      }

      public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        if ((opcode == Opcodes.INVOKESTATIC) && JavaLangArrayHelpers.CLASS.equals(owner)) {
          verified.add(method);
        }
      }

    }

  }

}
