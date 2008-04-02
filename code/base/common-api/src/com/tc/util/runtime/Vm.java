/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

/**
 * Utility class for understanding the current JVM version. Access the VM version information by looking at
 * {@link #VERSION} directly or calling the static helper methods.
 */
public class Vm {

  /**
   * Version info is parsed from system properties and stored here.
   */
  public static final VmVersion VERSION;
  static {
    try {
      VERSION = new VmVersion(System.getProperties());
    } catch (UnknownJvmVersionException mve) {
      throw new RuntimeException(mve);
    } catch (UnknownRuntimeVersionException mve) {
      throw new RuntimeException(mve);
    }
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
      // Our instrumentation for java.lang.reflect.Field can end up calling here while in <clinit> for
      // this class -- this avoids the NPE
      return VmVersion.thisVMisIBM();
    }
    return VERSION.isIBM();
  }

  public static void assertIsIbm() {
    if (!isIBM()) { throw new AssertionError("not ibm"); }
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

}
