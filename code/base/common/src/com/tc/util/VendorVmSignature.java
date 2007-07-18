/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.util.runtime.UnknownJvmVersionException;
import com.tc.util.runtime.UnknownRuntimeVersionException;
import com.tc.util.runtime.Vm;

import java.util.Properties;

public class VendorVmSignature {
  public static final char    SIGNATURE_SEPARATOR = '_';

  private static final String OS_WINDOWS          = "win32";
  private static final String OS_LINUX            = "linux";
  private static final String OS_SOLARIS_SPARC    = "solaris";
  private static final String OS_MAC_OSX          = "osx";
  private static final String OS_SOLARIS_X86      = "solaris-x86";

  private static final String VM_VENDOR_SUN       = "hotspot";
  private static final String VM_VENDOR_IBM       = "ibm";
  private static final String VM_VENDOR_BEA       = "jrockit";

  private final String        signature;

  public VendorVmSignature(final Properties props) throws VendorVmSignatureException {
    signature = generateSignature(props);
  }
  
  public VendorVmSignature() throws VendorVmSignatureException {
    this(System.getProperties());
  }

  private String generateSignature(final Properties source) throws VendorVmSignatureException {
    String os = getOS(source);
    String version = getVMVersion(source);
    String vendor = getVendor(source);

    validateComponent(os);
    validateComponent(version);
    validateComponent(vendor);

    return vendor + SIGNATURE_SEPARATOR + os + SIGNATURE_SEPARATOR + version;
  }

  private String getVendor(final Properties source) throws VendorVmSignatureException {
    String vendor = source.getProperty("java.vendor");

    if (vendor == null) { throw new VendorVmSignatureException("Cannot determine VM vendor: "
        + "(\"java.vendor\" system property is null)"); }

    if (vendor.toLowerCase().startsWith("bea ")) { return VM_VENDOR_BEA; }
    if (vendor.toLowerCase().startsWith("apple ")) { return VM_VENDOR_SUN; }
    if (vendor.toLowerCase().startsWith("ibm ")) { return VM_VENDOR_IBM; }
    if (vendor.toLowerCase().startsWith("sun ")) {
      final Vm.Version vmVersion;
      try {
        vmVersion = new Vm.Version(source);
      } catch (UnknownJvmVersionException ujve) {
        throw new VendorVmSignatureException("Unable to extract the JVM version with properties: " + source, ujve);
      } catch (UnknownRuntimeVersionException urve) {
        throw new VendorVmSignatureException("Unable to extract the JVM version with properties: " + source, urve);
      }
      if (vmVersion.isJRockit()) {
        // In at least one case, jrockit 1.4.2_05 on linux, you get "Sun Microsystems Inc." as the vendor...err
        return VM_VENDOR_BEA;
      }
      return VM_VENDOR_SUN;
    }

    throw new VendorVmSignatureException("Unknown or unsupported vendor string: " + vendor);
  }

  private static String getVMVersion(final Properties source) throws VendorVmSignatureException {
    try {
      final Vm.Version vmVersion = new Vm.Version(source);
      return vmVersion.toString().replaceAll("\\.", "");
    } catch (final UnknownJvmVersionException ujve) {
      throw new VendorVmSignatureException("Cannot determine VM version", ujve);
    } catch (final UnknownRuntimeVersionException urve) {
      throw new VendorVmSignatureException("Cannot determine VM version", urve);
    }
  }

  private static String getOS(final Properties source) throws VendorVmSignatureException {
    final String osProp = source.getProperty("os.name");
    if (osProp == null) { throw new VendorVmSignatureException("Cannot determine operating system: "
        + "(\"os.name\" system property is null)"); }
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
          throw new VendorVmSignatureException("Unknown Solaris architecture: " + "(\"os.arch\" = " + arch + ")");
        }
      } else {
        throw new VendorVmSignatureException("Cannot determine Solaris architecture: "
            + "(\"os.arch\" system property is null)");
      }
    }

    throw new VendorVmSignatureException("Unknown or unsupported OS detected: " + osProp);
  }

  private static void validateComponent(final String component) {
    if (component == null || component.indexOf('.') >= 0) { throw new AssertionError("Invalid component string: "
        + component); }
  }

  public final String getSignature() {
    return signature;
  }

  public String toString() {
    return getSignature();
  }
}
