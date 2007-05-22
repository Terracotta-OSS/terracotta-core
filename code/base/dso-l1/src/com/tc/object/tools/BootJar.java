/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tools;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.NotInBootJar;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class BootJar {
  private static final TCLogger logger               = TCLogging.getLogger(BootJar.class);

  private static final String   DSO_BOOT_JAR_PATTERN = ".+dso-boot.*\\.jar$";
  static final String           JAR_NAME_PREFIX      = "dso-boot-";

  private static final String   VERSION_1_1          = "1.1";

  private static final State    STATE_OPEN           = new State("OPEN");
  private static final State    STATE_CLOSED         = new State("CLOSED");

  public static final String    PREINSTRUMENTED_NAME = "Preinstrumented";

  private final File            file;
  private final Map             entries;
  private final boolean         openForWrite;
  private final Manifest        manifest;
  private final BootJarMetaData metaData;
  private final JarFile         jarFileInput;
  private final Set             classes              = new HashSet();
  private State                 state;
  private boolean               creationErrorOccurred;

  private BootJar(File file, boolean openForWrite, BootJarMetaData metaData, JarFile jarFile) {
    Assert.assertNotNull(file);

    this.file = file;
    this.openForWrite = openForWrite;
    this.metaData = metaData;
    this.jarFileInput = jarFile;
    this.manifest = new Manifest();
    this.entries = new HashMap();

    this.state = STATE_OPEN;
    this.creationErrorOccurred = false;
  }

  public static BootJar getBootJarForWriting(File bootJar) throws UnsupportedVMException {
    String vmSignature = BootJarSignature.getSignatureForThisVM().getSignature();
    return getBootJarForWriting(bootJar, vmSignature);
  }

  public static BootJar getBootJarForReading(File bootJar) throws IOException, BootJarException {
    return getBootJarForReading(bootJar, BootJarSignature.getSignatureForThisVM());
  }

  static BootJar getBootJarForWriting(File bootJar, String vmSignature) {
    return getBootJarForWriting(bootJar, vmSignature, VERSION_1_1);
  }

  static BootJar getBootJarForWriting(File bootJar, String vmSignature, String metaDataVersion) {
    return new BootJar(bootJar, true, new BootJarMetaData(vmSignature, metaDataVersion), null);
  }

  static BootJar getBootJarForReading(File bootJar, BootJarSignature expectedSignature) throws IOException,
      BootJarException {

    if (!existingFileIsAccessible(bootJar)) throw new FileNotFoundException("Cannot access file: "
                                                                            + bootJar.getAbsolutePath());

    JarFile jarFile = new JarFile(bootJar, false);
    Manifest manifest = jarFile.getManifest();
    BootJarMetaData metaData = new BootJarMetaData(manifest);

    // verify VM signature
    BootJarSignature signatureFromJar = new BootJarSignature(metaData.getVMSignature());

    if (!expectedSignature.isCompatibleWith(signatureFromJar)) {
      // make formatter sane
      throw new InvalidJVMVersionException(
                                           "Incompatible boot jar JVM version; expected '"
                                               + expectedSignature
                                               + "' but was (in boot jar) '"
                                               + signatureFromJar
                                               + "'; Please regenerate the DSO boot jar to match this VM, or switch to a VM compatible with this boot jar");
    }

    return new BootJar(bootJar, false, metaData, jarFile);
  }

  public static File findBootJar() throws FileNotFoundException {
    return findBootJarByPattern(DSO_BOOT_JAR_PATTERN);
  }

  private static File findBootJarByPattern(String pattern) throws FileNotFoundException {
    String bootClassPath = System.getProperty("sun.boot.class.path");
    StringTokenizer st = new StringTokenizer(bootClassPath, System.getProperty("path.separator"));
    while (st.hasMoreTokens()) {
      String element = st.nextToken();
      if (element.matches(pattern)) { return new File(element); }
    }
    throw new FileNotFoundException("Can't find boot jar matching pattern (" + pattern + ") in boot classpath: "
                                    + bootClassPath);
  }

  public static BootJar getDefaultBootJarForReading() throws BootJarException, IOException {
    return getBootJarForReading(findBootJar());
  }

  private static boolean existingFileIsAccessible(File file) {
    return file.exists() || file.isFile() || file.canRead();
  }

  public static String classNameToFileName(String className) {
    return className.replace('.', '/') + ".class";
  }
  
  public static String fileNameToClassName(String filename) {
    if (!filename.endsWith(".class")) throw new AssertionError("Invalid class file name: " + filename);
    return filename.substring(0, filename.lastIndexOf('.')).replace('/', '.');
  }

  public boolean classLoaded(String className) {
    return classes.contains(className);
  }

  public void loadClassIntoJar(String className, byte[] data, boolean isPreinstrumented) {
    boolean added = classes.add(className);

    // disallow duplicate entries into the boot jar. Even w/o this assertion, the jar
    // stuff will blow up later if an entry is duplicated
    Assert.assertTrue("Duplicate class added " + className, added);

    if (className.equals(NotInBootJar.class.getName())) {
      // make formatter sane
      throw new AssertionError("Invalid class for boot jar: " + className);
    }

    String cn = classNameToFileName(className);
    JarEntry jarEntry = new JarEntry(cn);
    basicLoadClassIntoJar(jarEntry, data, isPreinstrumented);
  }

  private void assertWrite() {
    if (!openForWrite) throw new AssertionError("boot jar not open for writing");
  }

  private synchronized void basicLoadClassIntoJar(JarEntry je, byte[] classBytes, boolean isPreinstrumented) {
    assertWrite();

    Attributes attributes = manifest.getAttributes(je.getName());
    if (attributes == null) {
      attributes = makeAttributesFor(je.getName());
    }
    attributes.put(new Attributes.Name(PREINSTRUMENTED_NAME), Boolean.toString(isPreinstrumented));
    entries.put(new JarEntryWrapper(je, new Boolean(isPreinstrumented)), classBytes);
  }

  private Attributes makeAttributesFor(String resource) {
    Attributes rv = new Attributes();
    manifest.getEntries().put(resource, rv);
    return rv;
  }

  private static final int QUERY_ALL = 0;
  private static final int QUERY_PREINSTRUMENTED = 1;
  private static final int QUERY_UNINSTRUMENTED = 2;
  
  private synchronized Set getBootJarClassNames(int query) throws IOException {
    assertOpen();
    Set rv = new HashSet();
    for (Enumeration e = jarFileInput.entries(); e.hasMoreElements();) {
      JarEntry entry = (JarEntry)e.nextElement();
      String entryName = entry.getName();
      
      // This condition used to only exclude "META-INF/MANIFEST.MF". Jar signing puts additional META-INF files into the
      // boot jar, which caused an assertion error. So, now only try reading specs from actual class files present in
      // the jar
      if (entryName.toLowerCase().endsWith(".class")) {
        switch(query) {
          case QUERY_ALL:
            rv.add(fileNameToClassName(entry.getName()));
            break;
          case QUERY_PREINSTRUMENTED:
            if (isPreInstrumentedEntry(entry)) {
              rv.add(fileNameToClassName(entry.getName()));
            }
            break;
          case QUERY_UNINSTRUMENTED:
            if (!isPreInstrumentedEntry(entry)) {
              rv.add(fileNameToClassName(entry.getName()));
            }
            break;
          default:
            Assert.failure("Query arg for getBootJarClasses() must be one of the following: QUERY_ALL, QUERY_PREINSTRUMENTED, QUERY_UNINSTRUMENTED");
            break;
        }
      }
    }
    return rv;
  }
  
  
  public synchronized Set getAllClasses() throws IOException {
    return getBootJarClassNames(QUERY_ALL);
  }
  
  public synchronized Set getAllUninstrumentedClasses() throws IOException {
    return getBootJarClassNames(QUERY_UNINSTRUMENTED);
  }

  public synchronized Set getAllPreInstrumentedClasses() throws IOException {
    return getBootJarClassNames(QUERY_PREINSTRUMENTED);
  }

  private void assertOpen() {
    if (state != STATE_OPEN) { throw new AssertionError("boot jar not open: " + state); }
  }

  private boolean isPreInstrumentedEntry(JarEntry entry) throws IOException {
    Attributes attributes = entry.getAttributes();
    if (attributes == null) throw new AssertionError("Invalid jar file: No attributes for jar entry: "
                                                     + entry.getName());
    String value = attributes.getValue(PREINSTRUMENTED_NAME);
    if (value == null) throw new AssertionError("Invalid jar file: No " + PREINSTRUMENTED_NAME
                                                + " attribute for jar entry: " + entry.getName());
    return Boolean.valueOf(value).booleanValue();
  }

  private void writeEntries(JarOutputStream out) throws IOException {
    for (Iterator i = entries.keySet().iterator(); i.hasNext();) {
      JarEntryWrapper je = (JarEntryWrapper) i.next();
      byte[] classBytes = (byte[]) entries.get(je);
      out.putNextEntry(je.getJarEntry());
      out.write(classBytes);
    }
  }

  public void setCreationErrorOccurred(boolean errorOccurred) {
    this.creationErrorOccurred = errorOccurred;
  }

  public synchronized void close() throws IOException {
    if (state == STATE_OPEN) {
      if (openForWrite && !this.creationErrorOccurred) {
        metaData.write(manifest);
        JarOutputStream out = new JarOutputStream(new FileOutputStream(file, false), manifest);
        writeEntries(out);
        out.flush();
        out.close();
      }

      if (jarFileInput != null) {
        jarFileInput.close();
      }
    }

    state = STATE_CLOSED;
  }

  /**
   * Check the embedded TC_VERSION information against current product version.
   * 
   * @return <code>true</code> TC_VERSION matches current product version.
   */
  public boolean checkSourceVersion() {
    return false;
  }

  static class BootJarMetaData {
    private static final String META_DATA_ATTRIBUTE_NAME = "DSO_BOOTJAR_METADATA";
    private static final String TC_VERSION               = "TC_VERSION";
    private static final String TC_MONIKER               = "TC_MONIKER";
    private static final String VERSION                  = "VERSION";
    private static final String VM_SIGNATURE             = "VM_SIGNATURE";

    private final String        vmSignature;
    private final String        version;
    private final String        tcversion;
    private final String        tcmoniker;

    BootJarMetaData(String vmSignature, String version) {
      Assert.assertNotNull(vmSignature);
      Assert.assertNotNull(version);
      this.vmSignature = vmSignature;
      this.version = version;
      this.tcversion = null;
      this.tcmoniker = null;
    }

    BootJarMetaData(Manifest manifest) throws BootJarException {
      Assert.assertNotNull(manifest);
      Attributes attributes = (Attributes) manifest.getEntries().get(META_DATA_ATTRIBUTE_NAME);
      if (attributes == null) throw new InvalidBootJarMetaDataException(
                                                                        "Missing attributes in jar manifest.  Please regenerate boot jar");

      version = attributes.getValue(VERSION);
      if (version == null) throw new InvalidBootJarMetaDataException("Missing metadata: version");

      String expect_version = VERSION_1_1;
      if (expect_version.equals(version)) {
        vmSignature = attributes.getValue(VM_SIGNATURE);
        if (vmSignature == null) throw new InvalidJVMVersionException("Missing vm signature");
      } else {
        throw new InvalidBootJarMetaDataException("Incompatible DSO meta data: version; expected '" + expect_version
                                                  + "' but was (in boot jar): '" + version
                                                  + "'; please regenerate the DSO boot jar");
      }

      tcversion = attributes.getValue(TC_VERSION);
      if (tcversion == null) throw new InvalidBootJarMetaDataException("Missing metadata: tcversion");

      tcmoniker = attributes.getValue(TC_MONIKER);
      if (tcmoniker == null) throw new InvalidBootJarMetaDataException("Missing metadata: tcmoniker");

      ProductInfo productInfo = ProductInfo.getInstance();
      String expect_tcversion = productInfo.buildVersion();

      if (productInfo.isDevMode()) logger
          .warn("The value for the DSO meta data, tcversion is: '"
                + expect_tcversion
                + "'; this might not be correct, this value is used only under development mode or when tests are being run.");

      if (!productInfo.isDevMode() && !expect_tcversion.equals(tcversion)) throw new InvalidBootJarMetaDataException(
                                                                                                                                     "Incompatible DSO meta data: tcversion; expected '"
                                                                                                                                         + expect_tcversion
                                                                                                                                         + "' but was (in boot jar): '"
                                                                                                                                         + tcversion
                                                                                                                                         + "'; please regenerate the DSO boot jar");
    }

    public void write(Manifest manifest) {
      if (VERSION_1_1.equals(version)) {
        ProductInfo productInfo = ProductInfo.getInstance();
        Attributes attributes = new Attributes();
        attributes.put(new Attributes.Name(TC_MONIKER), productInfo.moniker());
        attributes.put(new Attributes.Name(TC_VERSION), productInfo.buildVersion());
        attributes.put(new Attributes.Name(VERSION), getVersion());
        attributes.put(new Attributes.Name(VM_SIGNATURE), getVMSignature());
        Object prev = manifest.getEntries().put(META_DATA_ATTRIBUTE_NAME, attributes);
        Assert.assertNull(prev);
      } else {
        throw new AssertionError("Unexptected metadata for version, expecting '" + VERSION_1_1 + "', but was '" + version + "'");
      }
    }

    public String getTCVersion() {
      return this.tcversion;
    }

    public String getVMSignature() {
      return this.vmSignature;
    }

    public String getVersion() {
      return this.version;
    }
  }

  private static class JarEntryWrapper {
    private final JarEntry jarEntry;
    private final Boolean  isPreinstrumented;

    private JarEntryWrapper(JarEntry jarEntry, Boolean isPreinstrumented) {
      this.jarEntry = jarEntry;
      this.isPreinstrumented = isPreinstrumented;
    }

    public JarEntry getJarEntry() {
      return jarEntry;
    }

    public Boolean isPreistrumented() {
      return isPreinstrumented;
    }
  }

  private static class State {
    private final String name;

    private State(String name) {
      this.name = name;
    }

    public String toString() {
      return this.name;
    }
  }
}
