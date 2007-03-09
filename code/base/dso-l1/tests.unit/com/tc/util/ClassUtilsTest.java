/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.config.schema.NewCommonL1Config;
import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.exception.ImplementMe;
import com.tc.object.Portability;
import com.tc.object.PortabilityImpl;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.TransparencyClassAdapter;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.config.DistributedMethodSpec;
import com.tc.object.config.Lock;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.ModuleSpec;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.schema.DSOInstrumentationLoggingOptions;
import com.tc.object.config.schema.DSORuntimeLoggingOptions;
import com.tc.object.config.schema.DSORuntimeOutputOptions;
import com.tc.object.config.schema.InstrumentedClass;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.util.ClassUtils.ClassSpec;
import com.terracottatech.config.Modules;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class ClassUtilsTest extends TestCase {

  // private StringWriter stringWriter;
  // private PrintWriter printWriter;
  // private NonPortableReasonFormatter reasonFormatter;

  public void setUp() {
    // stringWriter = new StringWriter();
    // printWriter = new PrintWriter(stringWriter);
    // reasonFormatter = newFormatter(printWriter);
  }

  public void testArrayMethods() {
    assertEquals(int.class, ClassUtils.baseComponetType(int[][][][][].class));
    assertEquals(Object.class, ClassUtils.baseComponetType(Object[].class));

    try {
      ClassUtils.baseComponetType(null);
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      ClassUtils.baseComponetType(int.class);
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
    Config config;
    Portability p;

    // NonPortableReason.CLASS_NOT_ADAPTABLE
    config = new Config();
    p = new PortabilityImpl(config);
    config.isNeverAdaptable.add(NotAdaptable.class.getName());
    reason = p.getNonPortableReason(NotAdaptable.class);
    assertEquals(NonPortableReason.CLASS_NOT_ADAPTABLE, reason.getReason());
    assertEquals(NotAdaptable.class.getName(), reason.getClassName());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    // This is here for visual inspection of the output.
    // printReason(reason);

    // NonPortableReason.SUPER_CLASS_NOT_ADAPTABLE
    config = new Config();
    p = new PortabilityImpl(config);
    config.isNeverAdaptable.add(NotAdaptable.class.getName());
    reason = p.getNonPortableReason(ExtendsNotAdaptable.class);
    assertEquals(NonPortableReason.SUPER_CLASS_NOT_ADAPTABLE, reason.getReason());
    assertEquals(ExtendsNotAdaptable.class.getName(), reason.getClassName());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousBootJarSuperClasses());
    assertEquals(Arrays.asList(new Object[] { NotAdaptable.class.getName() }), reason.getErroneousSuperClasses());
    // printReason(reason);

    // NonPortableReason.SUBCLASS_OF_LOGICALLY_MANAGED_CLASS
    config = new Config();
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(LogicalSubclass.class);
    assertEquals(NonPortableReason.SUBCLASS_OF_LOGICALLY_MANAGED_CLASS, reason.getReason());
    assertEquals(LogicalSubclass.class.getName(), reason.getClassName());
    assertEquals(Arrays.asList(new Object[] { HashMap.class.getName() }), reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    // printReason(reason);

    // NonPortableReason.CLASS_NOT_IN_BOOT_JAR
    // -- no supers
    config = new Config();
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(OutputStream.class);
    assertEquals(NonPortableReason.CLASS_NOT_IN_BOOT_JAR, reason.getReason());
    assertEquals(OutputStream.class.getName(), reason.getClassName());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    // printReason(reason);

    // NonPortableReason.CLASS_NOT_IN_BOOT_JAR
    // -- boot jar supers
    config = new Config();
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(ByteArrayOutputStream.class);
    assertEquals(NonPortableReason.CLASS_NOT_IN_BOOT_JAR, reason.getReason());
    assertEquals(ByteArrayOutputStream.class.getName(), reason.getClassName());
    assertEquals(Arrays.asList(new Object[] { OutputStream.class.getName() }), reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    // printReason(reason);

    // NOTE: NonPortableReason.CLASS_NOT_IN_BOOT_JAR cannot have non-boot jar super classes
    // (or a mix of boot-jar and non-boot-jar supers)

    // NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG
    // -- no supers
    config = new Config();
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(Pojo.class);
    assertEquals(NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG, reason.getReason());
    assertEquals(Pojo.class.getName(), reason.getClassName());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    // printReason(reason);

    // NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG
    // -- regular supers
    config = new Config();
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(SubPojo.class);
    assertEquals(NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG, reason.getReason());
    assertEquals(SubPojo.class.getName(), reason.getClassName());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousBootJarSuperClasses());
    assertEquals(Arrays.asList(new Object[] { Pojo.class.getName() }), reason.getErroneousSuperClasses());
    // printReason(reason);

    // NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG
    // -- boot jar supers
    config = new Config();
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(SubClassOfBootJarClass.class);
    assertEquals(NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG, reason.getReason());
    assertEquals(SubClassOfBootJarClass.class.getName(), reason.getClassName());
    assertEquals(Arrays.asList(new Object[] { ByteArrayOutputStream.class.getName(), OutputStream.class.getName() }),
                 reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    // printReason(reason);

    // NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG
    // -- both regular and non-boot jar supers
    config = new Config();
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(PojoExtendsBootJar.class);
    assertEquals(NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG, reason.getReason());
    assertEquals(PojoExtendsBootJar.class.getName(), reason.getClassName());
    assertEquals(Arrays.asList(new Object[] { ByteArrayOutputStream.class.getName(), OutputStream.class.getName() }),
                 reason.getErroneousBootJarSuperClasses());
    assertEquals(Arrays.asList(new Object[] { SubClassOfBootJarClass.class.getName() }), reason
        .getErroneousSuperClasses());
    // printReason(reason);

    // NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED
    // -- regular supers
    config = new Config();
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(InstrumentedExtendsRegularNotInstrumented.class);
    assertEquals(NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED, reason.getReason());
    assertEquals(InstrumentedExtendsRegularNotInstrumented.class.getName(), reason.getClassName());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousBootJarSuperClasses());
    assertEquals(Arrays.asList(new Object[] { Pojo.class.getName() }), reason.getErroneousSuperClasses());
    // printReason(reason);

    // NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED
    // -- boot jar supers
    config = new Config();
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(InstrumentedExtendsBootJarNotInstrumented.class);
    assertEquals(NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED, reason.getReason());
    assertEquals(InstrumentedExtendsBootJarNotInstrumented.class.getName(), reason.getClassName());
    assertEquals(Arrays.asList(new Object[] { OutputStream.class.getName() }), reason.getErroneousBootJarSuperClasses());
    assertEquals(Collections.EMPTY_LIST, reason.getErroneousSuperClasses());
    // printReason(reason);

    // NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED
    // -- both regular and non-boot jar supers
    config = new Config();
    p = new PortabilityImpl(config);
    reason = p.getNonPortableReason(InstrumentedExtendsBothNotInstrumented.class);
    assertEquals(NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED, reason.getReason());
    assertEquals(InstrumentedExtendsBothNotInstrumented.class.getName(), reason.getClassName());
    assertEquals(Arrays.asList(new Object[] { ByteArrayOutputStream.class.getName(), OutputStream.class.getName() }),
                 reason.getErroneousBootJarSuperClasses());
    assertEquals(Arrays.asList(new Object[] { SubClassOfBootJarClass.class.getName() }), reason
        .getErroneousSuperClasses());
    // printReason(reason);

  }

  // private void printReason(NonPortableReason reason) {
  // reasonFormatter.formatReasonTypeName(reason.getReason());
  // printWriter.println();
  // reason.accept(reasonFormatter);
  // reasonFormatter.flush();
  // printWriter.flush();
  //
  // System.err.println("******************************************************************");
  // System.err.println(stringWriter.getBuffer());
  // System.err.println();
  // stringWriter.getBuffer().delete(0, stringWriter.getBuffer().length());
  // }

  // private NonPortableReasonFormatter newFormatter(PrintWriter out) {
  // ConsoleParagraphFormatter paragraphFormatter = new ConsoleParagraphFormatter(120, new StringFormatter());
  // NonPortableReasonFormatter formatter = new ConsoleNonPortableReasonFormatter(out, ": ", new StringFormatter(),
  // paragraphFormatter);
  // return formatter;
  // }

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

  private static class Config implements DSOClientConfigHelper {

    final Set isNeverAdaptable = new HashSet();

    public NewCommonL1Config getNewCommonL1Config() {
      return null;
    }

    public boolean shouldBeAdapted(ClassInfo classInfo) {
      return true;
    }

    public boolean isNeverAdaptable(ClassInfo classInfo) {
      return isNeverAdaptable.contains(classInfo.getName());
    }

    public boolean isLogical(String theClass) {
      return theClass.startsWith("java.util.");
    }

    public boolean isPhysical(String className) {
      throw new ImplementMe();
    }

    public DSOInstrumentationLoggingOptions getInstrumentationLoggingOptions() {
      throw new ImplementMe();
    }

    public TransparencyClassSpec[] getAllSpecs() {
      throw new ImplementMe();
    }

    public void verifyBootJarContents() {
      throw new ImplementMe();
    }

    public Iterator getAllUserDefinedBootSpecs() {
      throw new ImplementMe();
    }

    public ClassAdapter createClassAdapterFor(ClassWriter writer, ClassInfo classInfo, InstrumentationLogger lgr,
                                              ClassLoader caller) {
      throw new ImplementMe();
    }

    public ClassAdapter createClassAdapterFor(ClassWriter writer, ClassInfo classInfo, InstrumentationLogger lgr,
                                              ClassLoader caller, boolean disableSuperClassTypeChecking) {
      throw new ImplementMe();
    }

    public boolean isCallConstructorOnLoad(String className) {
      throw new ImplementMe();
    }

    public String getChangeApplicatorClassNameFor(String className) {
      throw new ImplementMe();
    }

    public TransparencyClassSpec getOrCreateSpec(String className) {
      throw new ImplementMe();
    }

    public TransparencyClassSpec getOrCreateSpec(String className, String applicator) {
      throw new ImplementMe();
    }

    public LockDefinition[] lockDefinitionsFor(int access, String className, String methodName, String description,
                                               String[] exceptions) {
      throw new ImplementMe();
    }

    public boolean isRoot(String className, String fieldName) {
      throw new ImplementMe();
    }

    public boolean isTransient(int modifiers, String classname, String field) {
      throw new ImplementMe();
    }

    public String rootNameFor(String className, String fieldName) {
      throw new ImplementMe();
    }

    public boolean isLockMethod(int access, String className, String methodName, String description, String[] exceptions) {
      throw new ImplementMe();
    }

    public DistributedMethodSpec getDmiSpec(int modifiers, String className, String methodName, String description,
                                            String[] exceptions) {
      throw new ImplementMe();
    }

    public TransparencyClassSpec getSpec(String className) {
      throw new ImplementMe();
    }

    public boolean isDSOSessions(String name) {
      throw new ImplementMe();
    }

    public DSORuntimeLoggingOptions runtimeLoggingOptions() {
      throw new ImplementMe();
    }

    public DSORuntimeOutputOptions runtimeOutputOptions() {
      throw new ImplementMe();
    }

    public DSOInstrumentationLoggingOptions instrumentationLoggingOptions() {
      throw new ImplementMe();
    }

    public int getClientInMemoryObjectCount() {
      throw new ImplementMe();
    }

    public int getFaultCount() {
      throw new ImplementMe();
    }

    public void addWriteAutolock(String methodPattern) {
      throw new ImplementMe();
    }

    public void addLock(String methodPattern, LockDefinition lockDefinition) {
      throw new ImplementMe();
    }

    public void addReadAutolock(String methodPattern) {
      throw new ImplementMe();
    }

    public void addAutolock(String methodPattern, ConfigLockLevel type) {
      throw new ImplementMe();
    }

    public void setFaultCount(int count) {
      throw new ImplementMe();
    }

    public void addRoot(String className, String fieldName, String rootName, boolean addSpecForClass) {
      throw new ImplementMe();
    }

    public boolean matches(Lock lock, MemberInfo methodInfo) {
      throw new ImplementMe();
    }

    public boolean matches(String expression, MemberInfo methodInfo) {
      throw new ImplementMe();
    }

    public void addTransient(String className, String fieldName) {
      throw new ImplementMe();
    }

    public void addTransientType(String className, String fieldName) {
      throw new ImplementMe();
    }

    public String getOnLoadScriptIfDefined(String className) {
      throw new ImplementMe();
    }

    public String getOnLoadMethodIfDefined(String className) {
      throw new ImplementMe();
    }

    public boolean isUseNonDefaultConstructor(Class clazz) {
      throw new ImplementMe();
    }

    public void addIncludePattern(String expression) {
      throw new ImplementMe();
    }

    public void addIncludePattern(String expression, boolean honorTransient) {
      throw new ImplementMe();
    }

    public void addExcludePattern(String expression) {
      throw new ImplementMe();
    }

    public boolean hasIncludeExcludePatterns() {
      throw new ImplementMe();
    }

    public boolean hasIncludeExcludePattern(ClassInfo classInfo) {
      throw new ImplementMe();
    }

    public void addAspectModule(String pattern, String moduleName) {
      throw new ImplementMe();
    }

    public Map getAspectModules() {
      throw new ImplementMe();
    }

    public void addDSOSpringConfig(DSOSpringConfigHelper config) {
      throw new ImplementMe();
    }

    public Collection getDSOSpringConfigs() {
      throw new ImplementMe();
    }

    public void addIncludePattern(String expression, boolean honorTransient, boolean oldStyleCallConstructorOnLoad) {
      throw new ImplementMe();
    }

    public void addIncludePattern(String expression, boolean honorTransient, boolean oldStyleCallConstructorOnLoad,
                                  boolean honorVolatile) {
      throw new ImplementMe();
    }

    public void addIncludeAndLockIfRequired(String expression, boolean honorTransient,
                                            boolean oldStyleCallConstructorOnLoad, boolean honorVolatile,
                                            String lockExpression) {
      throw new ImplementMe();
    }

    public boolean isVolatile(int modifiers, String classname, String field) {
      throw new ImplementMe();
    }

    public void addRoot(String rootName, String rootFieldName) {
      throw new ImplementMe();

    }

    public void writeTo(DSOApplicationConfigBuilder appConfigBuilder) {
      throw new ImplementMe();

    }

    public boolean isRootDSOFinal(String className, String fieldName, boolean isPrimitive) {
      throw new ImplementMe();
    }

    public void addRoot(String className, String fieldName, String rootName, boolean dsoFinal, boolean addSpecForClass) {
      throw new ImplementMe();
    }

    public String getPostCreateMethodIfDefined(String className) {
      throw new ImplementMe();
    }

    public Portability getPortability() {
      throw new ImplementMe();
    }

    public void removeSpec(String className) {
      throw new ImplementMe();

    }

    public String getLogicalExtendingClassName(String className) {
      return null;
    }

    public TransparencyClassAdapter createDsoClassAdapterFor(ClassVisitor writer, ClassInfo classInfo,
                                                             InstrumentationLogger lgr, ClassLoader caller,
                                                             boolean forcePortable) {
      return null;
    }

    public void addSynchronousWriteAutolock(String methodPattern) {
      throw new ImplementMe();
    }

    public void addApplicationName(String name) {
      throw new ImplementMe();
    }

    public void addInstrumentationDescriptor(InstrumentedClass classDesc) {
      throw new ImplementMe();
    }

    public void addUserDefinedBootSpec(String className, TransparencyClassSpec spec) {
      throw new ImplementMe();
    }

    public boolean hasCustomAdapter(String fullName) {
      throw new ImplementMe();
    }

    public Class getChangeApplicator(Class clazz) {
      throw new ImplementMe();
    }

    public void setModuleSpecs(ModuleSpec[] pluginSpecs) {
      throw new ImplementMe();
    }

    public void addNewModule(String name, String version) {
      throw new ImplementMe();
    }

    public Modules getModulesForInitialization() {
      return null;
    }

    public boolean isPortableModuleClass(Class clazz) {
      return false;
    }

    public void addCustomAdapter(String name, ClassAdapterFactory factory) {
      throw new ImplementMe();
    }

    public int getSessionLockType(String appName) {
      throw new ImplementMe();
    }

    public void addSynchronousWriteApplication(String name) {
      throw new ImplementMe();
    }

    public void addDistributedMethodCall(DistributedMethodSpec dms) {
      throw new ImplementMe();
    }

    public Class getTCPeerClass(Class clazz) {
      throw new ImplementMe();
    }
  }

}
