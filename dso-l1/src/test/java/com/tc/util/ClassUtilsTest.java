/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.exception.ImplementMe;
import com.tc.object.Portability;
import com.tc.object.PortabilityImpl;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.text.ConsoleNonPortableReasonFormatter;
import com.tc.text.ConsoleParagraphFormatter;
import com.tc.text.NonPortableReasonFormatter;
import com.tc.text.StringFormatter;
import com.tc.util.ClassUtils.ClassSpec;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class ClassUtilsTest extends TestCase {

  private static final boolean       PRINT_REASONS = false;
  private StringWriter               stringWriter;
  private PrintWriter                printWriter;
  private NonPortableReasonFormatter reasonFormatter;

  @Override
  public void setUp() {
    stringWriter = new StringWriter();
    printWriter = new PrintWriter(stringWriter);
    reasonFormatter = newFormatter(printWriter);
  }

  public void testArrayMethods() {
    assertEquals(int.class, ClassUtils.baseComponentType(int[][][][][].class));
    assertEquals(Object.class, ClassUtils.baseComponentType(Object[].class));

    try {
      ClassUtils.baseComponentType(null);
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      ClassUtils.baseComponentType(int.class);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }

    assertEquals(5, ClassUtils.arrayDimensions(int[][][][][].class));
    assertEquals(1, ClassUtils.arrayDimensions(Object[].class));

    try {
      ClassUtils.arrayDimensions(null);
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      ClassUtils.arrayDimensions(int.class);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }

  }

  public void testNonPortableReason() {
    NonPortableReason reason = null;
    DSOClientConfigHelper config;
    Portability p;

    // NonPortableReason.CLASS_NOT_ADAPTABLE
    config = getConfig(Collections.singleton(NotAdaptable.class.getName()));
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(NotAdaptable.class);
    assertEquals(NonPortableReason.CLASS_NOT_ADAPTABLE, reason.getReason());
    assertEquals(NotAdaptable.class.getName(), reason.getClassName());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    // This is here for visual inspection of the output.
    printReason(reason);

    // NonPortableReason.SUPER_CLASS_NOT_ADAPTABLE
    config = getConfig(Collections.singleton(NotAdaptable.class.getName()));
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(ExtendsNotAdaptable.class);
    assertEquals(NonPortableReason.SUPER_CLASS_NOT_ADAPTABLE, reason.getReason());
    assertEquals(ExtendsNotAdaptable.class.getName(), reason.getClassName());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousBootJarSuperClasses());
    assertEquals(Arrays.asList(new Object[] { NotAdaptable.class.getName() }), reason.getErroneousSuperClasses());
    printReason(reason);

    // NonPortableReason.SUBCLASS_OF_LOGICALLY_MANAGED_CLASS
    config = getConfig(Collections.EMPTY_SET);
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(LogicalSubclass.class);
    assertEquals(NonPortableReason.SUBCLASS_OF_LOGICALLY_MANAGED_CLASS, reason.getReason());
    assertEquals(LogicalSubclass.class.getName(), reason.getClassName());
    assertEquals(Arrays.asList(new Object[] { HashMap.class.getName() }), reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    printReason(reason);

    // NonPortableReason.CLASS_NOT_IN_BOOT_JAR
    // -- no supers
    config = getConfig(Collections.EMPTY_SET);
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(OutputStream.class);
    assertEquals(NonPortableReason.CLASS_NOT_IN_BOOT_JAR, reason.getReason());
    assertEquals(OutputStream.class.getName(), reason.getClassName());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    printReason(reason);

    // NonPortableReason.CLASS_NOT_IN_BOOT_JAR
    // -- boot jar supers
    config = getConfig(Collections.EMPTY_SET);
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(ByteArrayOutputStream.class);
    assertEquals(NonPortableReason.CLASS_NOT_IN_BOOT_JAR, reason.getReason());
    assertEquals(ByteArrayOutputStream.class.getName(), reason.getClassName());
    assertEquals(Arrays.asList(new Object[] { OutputStream.class.getName() }), reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    printReason(reason);

    // NOTE: NonPortableReason.CLASS_NOT_IN_BOOT_JAR cannot have non-boot jar super classes
    // (or a mix of boot-jar and non-boot-jar supers)

    // NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG
    // -- no supers
    config = getConfig(Collections.EMPTY_SET);
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(Pojo.class);
    assertEquals(NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG, reason.getReason());
    assertEquals(Pojo.class.getName(), reason.getClassName());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    printReason(reason);

    // NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG
    // -- regular supers
    config = getConfig(Collections.EMPTY_SET);
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(SubPojo.class);
    assertEquals(NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG, reason.getReason());
    assertEquals(SubPojo.class.getName(), reason.getClassName());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousBootJarSuperClasses());
    assertEquals(Arrays.asList(new Object[] { Pojo.class.getName() }), reason.getErroneousSuperClasses());
    printReason(reason);

    // NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG
    // -- boot jar supers
    config = getConfig(Collections.EMPTY_SET);
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(SubClassOfBootJarClass.class);
    assertEquals(NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG, reason.getReason());
    assertEquals(SubClassOfBootJarClass.class.getName(), reason.getClassName());
    assertEquals(Arrays.asList(new Object[] { ByteArrayOutputStream.class.getName(), OutputStream.class.getName() }),
                 reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    printReason(reason);

    // NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG
    // -- both regular and non-boot jar supers
    config = getConfig(Collections.EMPTY_SET);
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(PojoExtendsBootJar.class);
    assertEquals(NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG, reason.getReason());
    assertEquals(PojoExtendsBootJar.class.getName(), reason.getClassName());
    assertEquals(Arrays.asList(new Object[] { ByteArrayOutputStream.class.getName(), OutputStream.class.getName() }),
                 reason.getErroneousBootJarSuperClasses());
    assertEquals(Arrays.asList(new Object[] { SubClassOfBootJarClass.class.getName() }), reason
        .getErroneousSuperClasses());
    printReason(reason);

    // NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED
    // -- regular supers
    config = getConfig(Collections.EMPTY_SET);
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(InstrumentedExtendsRegularNotInstrumented.class);
    assertEquals(NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED, reason.getReason());
    assertEquals(InstrumentedExtendsRegularNotInstrumented.class.getName(), reason.getClassName());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousBootJarSuperClasses());
    assertEquals(Arrays.asList(new Object[] { Pojo.class.getName() }), reason.getErroneousSuperClasses());
    printReason(reason);

    // NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED
    // -- boot jar supers
    config = getConfig(Collections.EMPTY_SET);
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(InstrumentedExtendsBootJarNotInstrumented.class);
    assertEquals(NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED, reason.getReason());
    assertEquals(InstrumentedExtendsBootJarNotInstrumented.class.getName(), reason.getClassName());
    assertEquals(Arrays.asList(new Object[] { OutputStream.class.getName() }), reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    printReason(reason);

    // NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED
    // -- both regular and non-boot jar supers
    config = getConfig(Collections.EMPTY_SET);
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(InstrumentedExtendsBothNotInstrumented.class);
    assertEquals(NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED, reason.getReason());
    assertEquals(InstrumentedExtendsBothNotInstrumented.class.getName(), reason.getClassName());
    assertEquals(Arrays.asList(new Object[] { ByteArrayOutputStream.class.getName(), OutputStream.class.getName() }),
                 reason.getErroneousBootJarSuperClasses());
    assertEquals(Arrays.asList(new Object[] { SubClassOfBootJarClass.class.getName() }), reason
        .getErroneousSuperClasses());
    printReason(reason);

  }

  private DSOClientConfigHelper getConfig(final Set neverAdaptaed) {
    InvocationHandler handler = new InvocationHandler() {
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if ("getNewCommonL1Config".equals(name)) {
          return null;
        } else if ("getLogicalExtendingClassName".equals(name)) {
          return null;
        } else if ("createDsoClassAdapterFor".equals(name)) {
          return null;
        } else if ("getModulesForInitialization".equals(name)) {
          return null;
        } else if ("shouldBeAdapted".equals(name)) {
          return Boolean.TRUE;
        } else if ("isPortableModuleClass".equals(name)) {
          return Boolean.FALSE;
        } else if ("getSpec".equals(name)) {
          return null;
        } else if ("isNeverAdaptable".equals(name)) {
          return neverAdaptaed.contains(((ClassInfo) args[0]).getName()) ? Boolean.TRUE : Boolean.FALSE;
        } else if ("isLogical".equals(name)) { //
          return ((String) args[0]).startsWith("java.util.") ? Boolean.TRUE : Boolean.FALSE;
        }

        throw new ImplementMe();
      }
    };
    ClassLoader loader = getClass().getClassLoader();
    Object proxy = Proxy.newProxyInstance(loader, new Class[] { DSOClientConfigHelper.class }, handler);
    return (DSOClientConfigHelper) proxy;
  }

  private void printReason(NonPortableReason reason) {
    if (!PRINT_REASONS) { return; }
    reasonFormatter.formatReasonTypeName(reason.getReason());
    printWriter.println();
    reason.accept(reasonFormatter);
    reasonFormatter.flush();
    printWriter.flush();

    System.err.println("******************************************************************");
    System.err.println(stringWriter.getBuffer());
    System.err.println();
    stringWriter.getBuffer().delete(0, stringWriter.getBuffer().length());
  }

  private NonPortableReasonFormatter newFormatter(PrintWriter out) {
    ConsoleParagraphFormatter paragraphFormatter = new ConsoleParagraphFormatter(120, new StringFormatter());
    NonPortableReasonFormatter formatter = new ConsoleNonPortableReasonFormatter(out, ": ", new StringFormatter(),
                                                                                 paragraphFormatter);
    return formatter;
  }

  public void testIsPrimitiveArray() {

    assertTrue(ClassUtils.isPrimitiveArray(new byte[0]));
    assertTrue(ClassUtils.isPrimitiveArray(new boolean[1]));
    assertTrue(ClassUtils.isPrimitiveArray(new char[2]));
    assertTrue(ClassUtils.isPrimitiveArray(new double[3]));
    assertTrue(ClassUtils.isPrimitiveArray(new float[4]));
    assertTrue(ClassUtils.isPrimitiveArray(new int[5]));
    assertTrue(ClassUtils.isPrimitiveArray(new long[6]));
    assertTrue(ClassUtils.isPrimitiveArray(new short[7]));

    assertFalse(ClassUtils.isPrimitiveArray(new Object[0]));
    assertFalse(ClassUtils.isPrimitiveArray(new TestCase[42]));
    assertFalse(ClassUtils.isPrimitiveArray(new Socket[][] { {} }));
    assertFalse(ClassUtils.isPrimitiveArray(new byte[][] { {} }));

    assertFalse(ClassUtils.isPrimitiveArray(null));
    assertFalse(ClassUtils.isPrimitiveArray(new Object()));
  }

  public void testParseFullyQualifiedFieldName() throws Exception {
    String shortFieldName = "baz";
    String fullyQualifiedClassname = "foo.Bar";
    String fullyQualifiedFieldname = fullyQualifiedClassname + "." + shortFieldName;
    ClassUtils.parseFullyQualifiedFieldName(fullyQualifiedFieldname);

    ClassSpec spec = ClassUtils.parseFullyQualifiedFieldName(fullyQualifiedFieldname);
    assertEquals(shortFieldName, spec.getShortFieldName());
    assertEquals(fullyQualifiedClassname, spec.getFullyQualifiedClassName());

    try {
      spec = ClassUtils.parseFullyQualifiedFieldName(shortFieldName);
      fail("Expected a ParseException");
    } catch (ParseException e) {
      // expected
    }

    try {
      spec = ClassUtils.parseFullyQualifiedFieldName("foo.");
      fail("Excpected a ParseException");
    } catch (ParseException e) {
      // expected
    }

    try {
      spec = ClassUtils.parseFullyQualifiedFieldName(".foo");
      fail("Excpected a ParseException");
    } catch (ParseException e) {
      // expected
    }

  }

  private static class NotAdaptable {
    //
  }

  private static class ExtendsNotAdaptable extends NotAdaptable {
    //
  }

  private static class LogicalSubclass extends java.util.HashMap {
    //
  }

  private static class SubClassOfBootJarClass extends ByteArrayOutputStream {
    //
  }

  private static class Pojo extends Object {
    //
  }

  private static class SubPojo extends Pojo {
    //
  }

  private static class PojoExtendsBootJar extends SubClassOfBootJarClass {
    //
  }

  private static class InstrumentedExtendsBothNotInstrumented extends SubClassOfBootJarClass implements
      TransparentAccess {
    public void __tc_getallfields(Map map) {
      throw new ImplementMe();
    }

    public void __tc_setfield(String name, Object value) {
      throw new ImplementMe();
    }

    public Object __tc_getmanagedfield(String name) {
      throw new ImplementMe();
    }

    public void __tc_setmanagedfield(String name, Object value) {
      throw new ImplementMe();
    }
  }

  private static class InstrumentedExtendsBootJarNotInstrumented extends OutputStream implements TransparentAccess {

    @Override
    public void write(int b) {
      throw new ImplementMe();
    }

    public void __tc_getallfields(Map map) {
      throw new ImplementMe();
    }

    public void __tc_setfield(String name, Object value) {
      throw new ImplementMe();
    }

    public Object __tc_getmanagedfield(String name) {
      throw new ImplementMe();
    }

    public void __tc_setmanagedfield(String name, Object value) {
      throw new ImplementMe();
    }
  }

  private static class InstrumentedExtendsRegularNotInstrumented extends Pojo implements TransparentAccess {
    public void __tc_getallfields(Map map) {
      throw new ImplementMe();
    }

    public void __tc_setfield(String name, Object value) {
      throw new ImplementMe();
    }

    public Object __tc_getmanagedfield(String name) {
      throw new ImplementMe();
    }

    public void __tc_setmanagedfield(String name, Object value) {
      throw new ImplementMe();
    }
  }

}
