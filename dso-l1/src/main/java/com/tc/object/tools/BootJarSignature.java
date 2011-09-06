/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tools;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.VendorVmSignature;
import com.tc.util.VendorVmSignatureException;

import java.util.Properties;

public class BootJarSignature {

  private final String signature;

  public BootJarSignature(final Properties props) throws UnsupportedVMException {
    try {
      VendorVmSignature vendorVmSignature = new VendorVmSignature(props);
      this.signature = vendorVmSignature.getSignature();
    } catch (VendorVmSignatureException e) {
      throw new UnsupportedVMException(e.getMessage());
    }
  }

  BootJarSignature(final String signature) {
    this.signature = signature;
  }

  public String getSignature() {
    return signature;
  }

  public String toString() {
    return getSignature();
  }

  /**
   * For now just do a regular string equality check on the signature; this can be a place to hide ugly compatibility
   * stuff as needed
   * 
   * @return <code>true</code> if <code>signature</code> is compatible with the current object instance.
   */
  public boolean isCompatibleWith(final BootJarSignature bootJarSignature) {
    TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor("l1");
    boolean isCheckRequired = props.getBoolean("jvm.check.compatibility");
    return isCheckRequired ? signature.equals(bootJarSignature.signature) : true;
  }

  static BootJarSignature getSignatureForThisVM() throws UnsupportedVMException {
    return new BootJarSignature(System.getProperties());
  }

  /**
   * Calculates the filename for the DSO Boot JAR for the current VM
   * 
   * @return A String representing DSO Boot JAR filename
   */
  public static String getBootJarNameForThisVM() throws UnsupportedVMException {
    BootJarSignature signatureForThisVM = getSignatureForThisVM();
    return BootJar.JAR_NAME_PREFIX + signatureForThisVM + ".jar";
  }

  public static String getBootJarName(Properties properties) throws UnsupportedVMException {
    BootJarSignature signature = new BootJarSignature(properties);
    return BootJar.JAR_NAME_PREFIX + signature + ".jar";
  }
  
  /**
   * README: This main() method is called from the dso-java[.bat] script. It isn't for simple test purposes or anything.
   * Specificallly, there is a contract here.....running main() should output the expected name of the dso boot jar for
   * the VM running this method. If you think you want to change what this method does, you better have a look at the
   * calling scripts ;-)
   */
  public static void main(String args[]) {
    try {
      System.out.println(getBootJarNameForThisVM());
    } catch (Throwable t) {
      System.err.println("ERROR: " + t.getMessage());
      System.exit(1);
    }
    System.exit(0);
  }

}
