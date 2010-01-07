/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import org.apache.commons.io.IOUtils;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassReader;
import com.tc.asm.ClassWriter;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

public class DelegateMethodAdapterTest extends TestCase {

  public void testCrossLoader() throws Exception {
    Loader loader = new Loader();

    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    ClassAdapter adapter = new DelegateMethodAdapter(Base.class.getName(), "delegate").create(writer, loader);
    ClassReader reader = new ClassReader(getBytes(Wrapper.class.getName()));
    reader.accept(adapter, ClassReader.SKIP_FRAMES);

    byte[] adapted = writer.toByteArray();
    Class adaptedWrapper = loader.defineClass(Wrapper.class.getName(), adapted);

    Base delegate = new Base();
    Base wrapper = (Base) adaptedWrapper.getConstructor(Base.class).newInstance(delegate);

    if (wrapper.getClass().getClassLoader() != loader) {
      fail();
    }

    if (wrapper.getClass().getClassLoader() == delegate.getClass().getClassLoader()) {
      fail();
    }

    wrapper.protectedVoid();

    assertEquals(delegate, wrapper.protectedNonVoid());
    assertEquals(delegate, wrapper.foo(false, (byte) 1, 'c', 1D, 2F, 42, 54L, (short) 3));

    assertEquals("test", wrapper.unescape(null));

    assertEquals(true, wrapper.booleanMethod());
    assertEquals(1, wrapper.byteMethod());
    assertEquals('c', wrapper.charMethod());
    assertEquals(2D, wrapper.doubleMethod());
    assertEquals(3, wrapper.intMethod());
    assertEquals(4L, wrapper.longMethod());
    assertEquals(5, wrapper.shortMethod());

    assertEquals(delegate, wrapper.arrayMethod()[0]);
  }

  private byte[] getBytes(String name) throws IOException {
    InputStream in = getClass().getClassLoader().getResourceAsStream(name.replace('.', '/').concat(".class"));
    return IOUtils.toByteArray(in);
  }

  private static class Loader extends ClassLoader {
    Class defineClass(String name, byte[] data) {
      return defineClass(name, data, 0, data.length);
    }
  }

  public static class Base {

    protected void protectedVoid() {
      //
    }

    protected Object protectedNonVoid() {
      return this;
    }

    protected Object foo(boolean b, byte b2, char c, double d, float f, int i, long l, short s) {
      return this;
    }

    protected String unescape(String s) {
      return "test";
    }

    protected boolean booleanMethod() {
      return true;
    }

    protected byte byteMethod() {
      return 1;
    }

    protected char charMethod() {
      return 'c';
    }

    protected double doubleMethod() {
      return 2;
    }

    protected int intMethod() {
      return 3;
    }

    protected long longMethod() {
      return 4;
    }

    protected short shortMethod() {
      return 5;
    }

    protected Object[] arrayMethod() {
      return new Object[] { this };
    }

  }

  public static class Wrapper extends Base {

    static {
      // the presence of a <clinit> tripped up the first version of the adapter

      // there has to be something in here so the compiler will actually produce this method in the class
      System.err.println("<clinit>");
    }

    @SuppressWarnings("unused")
    private final Base delegate;

    public Wrapper(Base delegate) {
      this.delegate = delegate;
    }

  }

}
