/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tools;

import java.util.Properties;

public class BootJarSignature {
  public static final char    SEP              = '_';

  private static final String OS_WINDOWS       = "win32";
  private static final String OS_LINUX         = "linux";
  private static final String OS_SOLARIS_SPARC = "solaris";
  private static final String OS_MAC_OSX       = "osx";
  private static final String OS_SOLARIS_X86   = "solaris-x86";

  private static final String VM_VENDOR_SUN    = "hotspot";
  private static final String VM_VENDOR_BEA    = "jrockit";

  private final String        signature;

  BootJarSignature(Properties source) throws UnsupportedVMException {
    this.signature = generateBootJarSignature(source);
  }

  BootJarSignature(String signature) {
    this.signature = signature;
  }

  private String generateBootJarSignature(Properties source) throws UnsupportedVMException {
    String os = getOS(source);
    String version = getVMVersion(source);
    String vendor = getVendor(source);

    validateComponent(os);
    validateComponent(version);
    validateComponent(vendor);

    return vendor + SEP + os + SEP + version;
  }

  private String getVendor(Properties source) throws UnsupportedVMException {
    String vendor = source.getProperty("java.vendor");

    if (vendor == null) {
      // make formatter sane
      throw new UnsupportedVMException("Cannot determine VM vendor (\"java.vendor\" system property is null)");
    }

    if (vendor.toLowerCase().startsWith("bea ")) { return VM_VENDOR_BEA; }
    if (vendor.toLowerCase().startsWith("apple ")) { return VM_VENDOR_SUN; }
    if (vendor.toLowerCase().startsWith("sun ")) {
      if (isJRockit(source)) {
        // In at least one case, jrockit 1.4.2_05 on linux, you get "Sun Microsystems Inc." as the vendor...err
        return VM_VENDOR_BEA;
      }
      return VM_VENDOR_SUN;
    }

    throw new UnsupportedVMException("Unknown or unsupported vendor string: " + vendor);
  }

  private String getVMVersion(Properties source) throws UnsupportedVMException {
    String version = source.getProperty("java.version");
    if (version == null) {
      // make formatter sane
      throw new UnsupportedVMException("Cannot determine VM version (\"java.version\" system property is null)");
    }

    if (version.matches("^\\d\\.\\d\\.\\d_\\d\\d$")) { return version.replaceAll("\\.", ""); }

    throw new UnsupportedVMException("Cannot parse VM version string: " + version);
  }

  private static boolean isJRockit(Properties source) {
    return (source.getProperty("java.vm.name", "").toLowerCase().indexOf("jrockit") >= 0)
           || (source.getProperty("jrockit.version") != null);
  }

  private String getOS(Properties source) throws UnsupportedVMException {
    String osProp = source.getProperty("os.name");

    if (osProp == null) {
      // make formatter sane
      throw new UnsupportedVMException("Cannot determine operating system (\"os.name\" system property is null)");
    }

    if (osProp.toLowerCase().startsWith("windows")) { return OS_WINDOWS; }
    if (osProp.toLowerCase().startsWith("linux")) { return OS_LINUX; }
    if (osProp.toLowerCase().startsWith("mac")) { return OS_MAC_OSX; }

    if (osProp.toLowerCase().startsWith("sunos")) {
      String arch = source.getProperty("os.arch");
      if (arch != null) {
        if (arch.toLowerCase().startsWith("sparc")) {
          return OS_SOLARIS_SPARC;
        } else if (arch.toLowerCase().indexOf("86") > -1) {
          return OS_SOLARIS_X86;
        } else {
          throw new UnsupportedVMException("Unknown Solaris architecture (\"os.arch\" = " + arch + ")");
        }
      } else {
        throw new UnsupportedVMException("Cannot determine Solaris architecture (\"os.arch\" system property is null)");
      }
    }

    throw new UnsupportedVMException("Unknown or unsupported OS detected: " + osProp);
  }

  private static void validateComponent(String component) {
    if ((component == null) || (component.indexOf('.') >= 0)) {
      // make formatter sane
      throw new AssertionError("Invalid component string: " + component);
    }
  }

  String getSignature() {
    return this.signature;
  }

  public String toString() {
    return getSignature();
  }

  public boolean isCompatibleWith(BootJarSignature compare) {
    // This can be a place to hide ugly compatibility stuff as needed
    // For now just do a regular string equality check on the signature
    return this.signature.equals(compare.signature);
  }

  private static void exit(int code) {
    System.exit(code);
  }

  static BootJarSignature getSignatureForThisVM() throws UnsupportedVMException {
    return new BootJarSignature(System.getProperties());
  }

  public static String getBootJarNameForThisVM() throws UnsupportedVMException {
    BootJarSignature signatureForThisVM = getSignatureForThisVM();
    return BootJar.JAR_NAME_PREFIX + signatureForThisVM + ".jar";
  }

  public static void main(String args[]) {
    // README: This main() method is called from the dso-java[.bat] script. It isn't for simple test purposes or
    // anything. Specificallly, there is a contract here.....running main() should output the expected name of the dso
    // boot jar for the VM running this method. If you think you want to change what this method does, you better have a
    // look at the calling scripts ;-)

    try {
      System.out.println(getBootJarNameForThisVM());
    } catch (Throwable t) {
      System.err.println("ERROR: " + t.getMessage());
      exit(1);
    }
    exit(0);
  }
}
