/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tools;

import com.tc.object.BaseDSOTestCase;
import com.tc.object.Portability;
import com.tc.object.PortabilityImpl;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.TransparencyClassSpecImpl;
import com.tc.util.Assert;
import com.tc.util.TCAssertionError;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BootJarTest extends BaseDSOTestCase {

  private final static String      STRING_CLASS  = "java.lang.String";
  private final ClassBytesProvider bytesProvider = new ClassLoaderBytesProvider(getClass().getClassLoader());

  public BootJarTest() {
    // 
  }

  public void testInvalidBootJarVersion() throws Exception {
    File jarFile = this.getTempFile("dso-boot.jar");
    BootJar bootJar = BootJar.getBootJarForWriting(jarFile, "bogus_jvm_signature_666");
    bootJar.loadClassIntoJar(STRING_CLASS, bytesProvider.getBytesForClass(STRING_CLASS), false);
    BootJar.closeQuietly(bootJar);
    try {
      BootJar.getBootJarForReading(jarFile);
      fail("The test was expecting an InvalidJVMVersionException, but it was not thrown.");
    } catch (InvalidJVMVersionException e) {
      // we expect this to happen since the vm signature won't
      // match the running VM signature)
    }
  }

  public void testFindBootJar() throws Exception {
    // use absolute file to make sure the path is fully qualified on windows
    // (ie. has drive letter)
    File bootJar = new File("/path/to/dso-boot/dso-boot.jar").getAbsoluteFile();

    String origBootClassPath = System.getProperty("sun.boot.class.path");

    System.setProperty("sun.boot.class.path", bootJar.getAbsolutePath() + System.getProperty("path.separator")
                                              + origBootClassPath);

    assertEquals(bootJar, BootJar.findBootJar());

    System.setProperty("sun.boot.class.path", origBootClassPath);

    bootJar = new File("/path/to/dso-boot/dso-boot-hotspot_win32_150.jar").getAbsoluteFile();

    System.setProperty("sun.boot.class.path", bootJar.getAbsolutePath() + System.getProperty("path.separator")
                                              + origBootClassPath);

    assertEquals(bootJar, BootJar.findBootJar());

    System.setProperty("sun.boot.class.path", origBootClassPath);
  }

  public void testAllSuperClassesPresentInBootJar() throws Exception {
    Portability portability = new PortabilityImpl(this.configHelper());
    BootJar bootJar = BootJar.getDefaultBootJarForReading();
    if (bootJar == null) { throw new TCAssertionError("Boot Jar Not Found !"); }
    Set allClasses = bootJar.getAllPreInstrumentedClasses();
    Set missingClasses = new HashSet();
    for (Iterator iter = allClasses.iterator(); iter.hasNext();) {
      String classname = (String) iter.next();
      Class clazz = Class.forName(classname);
      while (clazz != null && clazz != Object.class) {
        clazz = clazz.getSuperclass();
        if (!portability.isInstrumentationNotNeeded(clazz.getName()) && !allClasses.contains(clazz.getName())) {
          System.err.println(" Class " + classname + " is in the bootjar, but its super class " + clazz.getName()
                             + " not in the bootjar !");
          missingClasses.add(clazz.getName());
        }
      }
    }
    if (!missingClasses.isEmpty()) {
      System.err.println("\n\nThe Following classes should go into bootjar !");
      printClasses(missingClasses);
    }
    Assert.assertTrue(missingClasses.isEmpty());
  }

  private void printClasses(Set classes) {
    for (Iterator iter = classes.iterator(); iter.hasNext();) {
      String className = (String) iter.next();
      System.err.println(" " + className);
    }
  }

  protected boolean cleanTempDir() {
    return false;
  }

  public void tests() throws Exception {
    String vmSig = "vm signature";
    DSOClientConfigHelper config = configHelper();

    File jar = getTempFile("dso-boot.jar");

    BootJar bootJar = BootJar.getBootJarForWriting(jar, vmSig);

    // load a regular class into the jar.
    bootJar.loadClassIntoJar(STRING_CLASS, bytesProvider.getBytesForClass(STRING_CLASS), false);
    // load an "instrumented" class into the jar.
    String classname = Boolean.class.getName();
    TransparencyClassSpec spec = new TransparencyClassSpecImpl(classname, config);
    spec.markPreInstrumented();
    byte[] classBytes = bytesProvider.getBytesForClass(classname);
    assertNotNull(classBytes);
    bootJar.loadClassIntoJar(spec.getClassName(), classBytes, spec.isPreInstrumented());
    BootJar.closeQuietly(bootJar);

    bootJar = BootJar.getBootJarForReading(jar, new BootJarSignature(vmSig));

    Set allPreInstrumentedClasses = bootJar.getAllPreInstrumentedClasses();
    assertEquals(1, allPreInstrumentedClasses.size());
    assertTrue(allPreInstrumentedClasses.contains(classname));
    assertFalse(allPreInstrumentedClasses.contains(java.lang.String.class.getName()));

    BootJar.closeQuietly(bootJar);
  }

}
