/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility class for understanding the current JVM version. Access the VM
 * version information by looking at {@link #VERSION} directly or calling the
 * static helper methods.
 */
public class Vm {

  /**
   * Version info is parsed from system properties and stored here.
   */
  public static final VmVersion VERSION;
  static {
    VERSION = new VmVersion(System.getProperties());
  }

  private Vm() {
    // utility class
  }

  /**
   * Get mega version (ie 1 in 1.2.3)
   * 
   * @return Mega version
   */
  public static int getMegaVersion() {
    return VERSION.getMegaVersion();
  }

  /**
   * Get major version (ie 2 in 1.2.3)
   * 
   * @return Major version
   */
  public static int getMajorVersion() {
    return VERSION.getMajorVersion();
  }

  /**
   * Get minor version (ie 3 in 1.2.3)
   * 
   * @return Minor version
   */
  public static int getMinorVersion() {
    return VERSION.getMinorVersion();
  }

  /**
   * Get patch level (ie 12 in 1.4.2_12)
   * 
   * @return Patch level
   */
  public static String getPatchLevel() {
    return VERSION.getPatchLevel();
  }

  /**
   * True if mega/major is 1.4
   * 
   * @return True if 1.4
   */
  public static boolean isJDK14() {
    return VERSION.isJDK14();
  }

  /**
   * True if mega/major is 1.5
   * 
   * @return True if 1.5
   */
  public static boolean isJDK15() {
    return VERSION.isJDK15();
  }

  /**
   * True if mega/major is 1.6
   * 
   * @return True if 1.6
   */
  public static boolean isJDK16() {
    return VERSION.isJDK16();
  }

  /**
   * True if mega/major is 1.7
   * 
   * @return True if 1.7
   */
  public static boolean isJDK17() {
    return VERSION.isJDK17();
  }

  /**
   * True if JDK is 1.5+
   * 
   * @return True if JDK 1.5/1.6/1.7
   */
  public static boolean isJDK15Compliant() {
    return VERSION.getMajorVersion() >= 5;
  }

  /**
   * True if JDK is 1.6+
   * 
   * @return True if JDK 1.6/1.7
   */
  public static boolean isJDK16Compliant() {
    return VERSION.getMajorVersion() >= 6;
  }

  /**
   * True if IBM JDK
   * 
   * @return True if IBM JDK
   */
  public static boolean isIBM() {
    if (VERSION == null) {
      // Our instrumentation for java.lang.reflect.Field can end up calling here
      // while in <clinit> for
      // this class -- this avoids the NPE
      return VmVersion.thisVMisIBM();
    }
    return VERSION.isIBM();
  }

  public static void assertIsIbm() {
    if (!isIBM()) {
      throw new AssertionError("not ibm");
    }
  }

  /**
   * True if JRockit
   * 
   * @return True if BEA Jrockit VM
   */
  public static boolean isJRockit() {
    return VERSION.isJRockit();
  }

  /**
   * True if Azul
   * 
   * @return True if Azul VM
   */
  public static boolean isAzul() {
    return VERSION.isAzul();
  }
  
  /**
   * @return true if Sun or Oracle VM
   */
  public static boolean isHotSpot() {
    return VERSION.isHotSpot();
  }

  /**
   * @return true if OpenJDK BM
   */
  public static boolean isOpenJdk() {
    return VERSION.isOpenJdk();
  }

  /**
   * @return 32 for a 32 bits JVM, 64 for a 64 bits JVM, 0 if unknown.
   */
  public static int dataModel() {
    try {
      // we can safely assume Azul systems to run 64 bits JVMs
      if (isAzul()) {
        return 64;
      }

      // this isn't standard but works with Sun, IBM and JRockit VMs
      String dataModelString = System.getProperty("sun.arch.data.model", "0");
      return Integer.parseInt(dataModelString);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * return maxDirectMemory
   * if Sun, returns 66650112 if not set via jvmarg
   * if JRockit, returns Long.MAX_VALUE if not set via jvmarg
   * @return
   */
  @SuppressWarnings("restriction")
  public static long maxDirectMemory() {
    if (isJRockit()) {
      return extractMaxDirectMemoryJrockit();
    } 
    if (isHotSpot() || isOpenJdk() || isIBM()) {
      return sun.misc.VM.maxDirectMemory(); 
    }
    throw new RuntimeException("Don't know how to find maxDirectMemory for this VM");
  }

  /**
   * returns reserved (used) direct memory
   * @return reserved direct memory in bytes
   */
  public static long reservedDirectMemory() {
    if (isJRockit()) {
      return reservedDirectMemoryJrockit();
    } 
    if (isHotSpot() || isOpenJdk() || isIBM()) {
      return reservedDirectMemorySun();
    } 
    throw new RuntimeException("Don't know how to find reservedDirectMemory for this VM");
  }
  
  private static long extractMaxDirectMemoryJrockit() {
    //jrockit.vm.Memory.reserveDirectMemory(0); 
    //return reflectiveRead(jrockit.vm.Memory.maxDirectMemory); 
    Class<?> jrockitMemoryClass;
    try {
      jrockitMemoryClass = Class.forName("jrockit.vm.Memory");
      Method reserveDirectMemoryMethod = jrockitMemoryClass.getDeclaredMethod("reserveDirectMemory", long.class);
      reserveDirectMemoryMethod.invoke(null, Long.valueOf(0));
      Field maxDirectMemoryField = jrockitMemoryClass.getDeclaredField("maxDirectMemory");
      maxDirectMemoryField.setAccessible(true);
      return maxDirectMemoryField.getLong(null);
    } catch (Exception e) {
      throw new RuntimeException("Failed to read max direct memory", e);
    }
  }
  
  private static long reservedDirectMemorySun() {
    Class<?> bitsClass;
    try {
      bitsClass = Class.forName("java.nio.Bits");
      Field reservedMemoryField = bitsClass.getDeclaredField("reservedMemory");
      reservedMemoryField.setAccessible(true);
      return reservedMemoryField.getLong(null);
    } catch (Exception e) {
      throw new RuntimeException("Failed to read reservedMemory", e);
    }
  }
  
  private static long reservedDirectMemoryJrockit() {
    Class<?> jrockitMemoryClass;
    try {
      jrockitMemoryClass = Class.forName("jrockit.vm.Memory");
      Method getReservedDirectMemoryMethod = jrockitMemoryClass.getDeclaredMethod("getReservedDirectMemory", new Class[0]);
      Long rv = (Long)getReservedDirectMemoryMethod.invoke(null, new Object[0]);
      return rv.longValue();
    } catch (Exception e) {
      throw new RuntimeException("Failed to read reservedMemory", e);
    }
  }
}
