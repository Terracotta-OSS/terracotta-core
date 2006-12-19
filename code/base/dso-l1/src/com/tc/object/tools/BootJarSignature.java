/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tools;

import com.tc.util.runtime.Vm;
import com.tc.util.runtime.Vm.UnknownJvmVersionException;

import java.util.Properties;

public class BootJarSignature {

  public static final char    SIGNATURE_SEPARATOR = '_';

  private static final String OS_WINDOWS          = "win32";
  private static final String OS_LINUX            = "linux";
  private static final String OS_SOLARIS_SPARC    = "solaris";
  private static final String OS_MAC_OSX          = "osx";
  private static final String OS_SOLARIS_X86      = "solaris-x86";

  private static final String VM_VENDOR_SUN       = "hotspot";
  private static final String VM_VENDOR_BEA       = "jrockit";

  private final String        signature;

  BootJarSignature(final Properties source) throws UnsupportedVMException {
    signature = generateBootJarSignature(source);
  }

  BootJarSignature(final String signature) {
    this.signature = signature;
  }

  private String generateBootJarSignature(final Properties source) throws UnsupportedVMException {
    String os = getOS(source);
    String version = getVMVersion(source);
    String vendor = getVendor(source);

    validateComponent(os);
    validateComponent(version);
    validateComponent(vendor);

    return vendor + SIGNATURE_SEPARATOR + os + SIGNATURE_SEPARATOR + version;
  }

  private String getVendor(final Properties source) throws UnsupportedVMException {
    String vendor = source.getProperty("java.vendor");

    if (vendor == null) {
      UnsupportedVMException uvme = new UnsupportedVMException("Cannot determine VM vendor: "
                                                               + "(\"java.vendor\" system property is null)");
      throw uvme;
    }

    if (vendor.toLowerCase().startsWith("bea ")) { return VM_VENDOR_BEA; }
    if (vendor.toLowerCase().startsWith("apple ")) { return VM_VENDOR_SUN; }
    if (vendor.toLowerCase().startsWith("sun ")) {
      final Vm.Version vmVersion;
      try {
        vmVersion = new Vm.Version(source);
      } catch (UnknownJvmVersionException ujve) {
        UnsupportedVMException uvme = new UnsupportedVMException("Unable to extract the JVM version with properties: "
                                                                 + source, ujve);
        throw uvme;
      }
      if (vmVersion.isJRockit()) {
        // In at least one case, jrockit 1.4.2_05 on linux, you get "Sun Microsystems Inc." as the vendor...err
        return VM_VENDOR_BEA;
      }
      return VM_VENDOR_SUN;
    }

    throw new UnsupportedVMException("Unknown or unsupported vendor string: " + vendor);
  }

  private static String getVMVersion(final Properties source) throws UnsupportedVMException {
    try {
      final Vm.Version vmVersion = new Vm.Version(source);
      return vmVersion.toString().replaceAll("\\.", "");
    } catch (final UnknownJvmVersionException ujve) {
      final UnsupportedVMException uvme = new UnsupportedVMException("Cannot determine VM version", ujve);
      throw uvme;
    }
  }

  private static String getOS(final Properties source) throws UnsupportedVMException {
    final String osProp = source.getProperty("os.name");
    if (osProp == null) {
      UnsupportedVMException uvme = new UnsupportedVMException("Cannot determine operating system: "
                                                               + "(\"os.name\" system property is null)");
      throw uvme;
    }
    final String lowerCaseOS = osProp.toLowerCase();

    if (lowerCaseOS.startsWith("windows")) { return OS_WINDOWS; }
    if (lowerCaseOS.startsWith("linux")) { return OS_LINUX; }
    if (lowerCaseOS.startsWith("mac")) { return OS_MAC_OSX; }
    if (lowerCaseOS.startsWith("sunos")) {
      final String arch = source.getProperty("os.arch");
      if (arch != null) {
        final String lowerCaseArch = arch.toLowerCase();
        if (lowerCaseArch.startsWith("sparc")) {
          return OS_SOLARIS_SPARC;
        } else if (lowerCaseArch.indexOf("86") > -1) {
          return OS_SOLARIS_X86;
        } else {
          throw new UnsupportedVMException("Unknown Solaris architecture: " + "(\"os.arch\" = " + arch + ")");
        }
      } else {
        throw new UnsupportedVMException("Cannot determine Solaris architecture: "
                                         + "(\"os.arch\" system property is null)");
      }
    }

    throw new UnsupportedVMException("Unknown or unsupported OS detected: " + osProp);
  }

  private static void validateComponent(final String component) {
    if (component == null || component.indexOf('.') >= 0) {
      final AssertionError ae = new AssertionError("Invalid component string: " + component);
      throw ae;
    }
  }

  String getSignature() {
    return signature;
  }

  public String toString() {
    return getSignature();
  }

  public boolean isCompatibleWith(final BootJarSignature compare) {
    // This can be a place to hide ugly compatibility stuff as needed
    // For now just do a regular string equality check on the signature
    return signature.equals(compare.signature);
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
      System.exit(1);
    }
    System.exit(0);
  }

}
