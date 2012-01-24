/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tools;

import org.apache.commons.io.IOUtils;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.NotInBootJar;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class BootJar {
  private static final TCLogger logger               = TCLogging.getLogger(BootJar.class);

  private static final String   DSO_BOOT_JAR_PATTERN = "(?i).+dso-boot.*\\.jar$";
  static final String           JAR_NAME_PREFIX      = "dso-boot-";

  private static final String   VERSION_1_1          = "1.1";

  private static final State    STATE_OPEN           = new State("OPEN");
  private static final State    STATE_CLOSED         = new State("CLOSED");

  public static final String    PREINSTRUMENTED_NAME = "Preinstrumented";
  public static final String    FOREIGN_NAME         = "Foreign";

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
    return getBootJarForWriting(bootJar, BootJarSignature.getSignatureForThisVM().getSignature());
  }

  public static BootJar getBootJarForReading(File bootJar) throws IOException, BootJarException {
    return getBootJarForReading(bootJar, BootJarSignature.getSignatureForThisVM());
  }

  public static void verifyTCVersion(URL bootJar) throws IOException, BootJarException {
    InputStream in = null;

    try {
      in = bootJar.openStream();
      JarInputStream jarIn = new JarInputStream(in);

      new BootJarMetaData(jarIn.getManifest());
    } finally {
      IOUtils.closeQuietly(in);
    }
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

    // verify VM signature (iff l1.jvm.check.compatibility == true, see: tc.properties)
    BootJarSignature signatureFromJar = new BootJarSignature(metaData.getVMSignature());
    final boolean checkJvmCompatibility = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L1_JVM_CHECK_COMPATIBILITY);
    if (checkJvmCompatibility && !expectedSignature.isCompatibleWith(signatureFromJar)) { throw new InvalidJVMVersionException(
                                                                                                                               "Incompatible boot jar JVM version; expected '"
                                                                                                                                   + expectedSignature
                                                                                                                                   + "' but was (in boot jar) '"
                                                                                                                                   + signatureFromJar
                                                                                                                                   + "'; Please regenerate the DSO boot jar to match this VM, or switch to a VM compatible with this boot jar"); }
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
    loadClassIntoJar(className, data, isPreinstrumented, false);
  }

  public void loadClassIntoJar(String className, byte[] data, boolean isPreinstrumented, boolean isForeign) {
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
    basicLoadClassIntoJar(jarEntry, data, isPreinstrumented, isForeign);
  }

  private void assertWrite() {
    if (!openForWrite) throw new AssertionError("boot jar not open for writing");
  }

  private synchronized void basicLoadClassIntoJar(JarEntry je, byte[] classBytes, boolean isPreinstrumented,
                                                  boolean isForeign) {
    assertWrite();

    Attributes attributes = manifest.getAttributes(je.getName());
    if (attributes == null) {
      attributes = makeAttributesFor(je.getName());
    }
    attributes.put(new Attributes.Name(PREINSTRUMENTED_NAME), Boolean.toString(isPreinstrumented));
    attributes.put(new Attributes.Name(FOREIGN_NAME), Boolean.toString(isForeign));
    entries.put(new JarEntryWrapper(je), classBytes);
  }

  private Attributes makeAttributesFor(String resource) {
    Attributes rv = new Attributes();
    manifest.getEntries().put(resource, rv);
    return rv;
  }

  private static final int QUERY_ALL             = 0;
  private static final int QUERY_PREINSTRUMENTED = 1;
  private static final int QUERY_UNINSTRUMENTED  = 2;
  private static final int QUERY_FOREIGN         = 3;
  private static final int QUERY_NOT_FOREIGN     = 4;

  public synchronized byte[] getBytesForClass(final String className) throws ClassNotFoundException {
    InputStream input = null;
    String resource = null;
    try {
      resource = BootJar.classNameToFileName(className);
      JarEntry entry = jarFileInput.getJarEntry(resource);
      input = jarFileInput.getInputStream(entry);
      if (input == null) throw new ClassNotFoundException("No resource found for class: " + className);
      return IOUtils.toByteArray(input);
    } catch (IOException e) {
      throw new ClassNotFoundException("Error reading bytes from " + resource, e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  private synchronized Set getBootJarClassNames(int query) throws IOException {
    assertOpen();
    Set rv = new HashSet();
    for (Enumeration e = jarFileInput.entries(); e.hasMoreElements();) {
      JarEntry entry = (JarEntry) e.nextElement();
      String entryName = entry.getName();

      // This condition used to only exclude "META-INF/MANIFEST.MF". Jar signing puts additional META-INF files into the
      // boot jar, which caused an assertion error. So, now only try reading specs from actual class files present in
      // the jar
      if (entryName.toLowerCase().endsWith(".class")) {
        switch (query) {
          case QUERY_ALL:
            rv.add(fileNameToClassName(entry.getName()));
            break;
          case QUERY_PREINSTRUMENTED:
            if (isPreInstrumentedEntry(entry)) rv.add(fileNameToClassName(entry.getName()));
            break;
          case QUERY_UNINSTRUMENTED:
            if (!isPreInstrumentedEntry(entry)) rv.add(fileNameToClassName(entry.getName()));
            break;
          case QUERY_FOREIGN:
            if (isForeign(entry)) rv.add(fileNameToClassName(entry.getName()));
            break;
          case QUERY_NOT_FOREIGN:
            if (!isForeign(entry)) rv.add(fileNameToClassName(entry.getName()));
            break;
          default:
            Assert
                .failure("Query arg for getBootJarClasses() must be one of the following: QUERY_ALL, QUERY_PREINSTRUMENTED, QUERY_UNINSTRUMENTED, QUERY_FOREIGN, QUERY_NOT_FOREIGN");
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

  public synchronized Set getAllForeignClasses() throws IOException {
    return getBootJarClassNames(QUERY_FOREIGN);
  }

  public synchronized Set getAllNonForeignClasses() throws IOException {
    return getBootJarClassNames(QUERY_NOT_FOREIGN);
  }

  private void assertOpen() {
    if (state != STATE_OPEN) { throw new AssertionError("boot jar not open: " + state); }
  }

  private String getJarEntryAttributeValue(JarEntry entry, String attributeName) throws IOException {
    Attributes attributes = entry.getAttributes();
    if (attributes == null) throw new AssertionError("Invalid jar file: No attributes for jar entry: "
                                                     + entry.getName());
    String value = attributes.getValue(attributeName);
    if (value == null) throw new AssertionError("Invalid jar file: No " + attributeName + " attribute for jar entry: "
                                                + entry.getName());
    return value;

  }

  private boolean isForeign(JarEntry entry) throws IOException {
    return Boolean.valueOf(getJarEntryAttributeValue(entry, FOREIGN_NAME)).booleanValue();
  }

  private boolean isPreInstrumentedEntry(JarEntry entry) throws IOException {
    return Boolean.valueOf(getJarEntryAttributeValue(entry, PREINSTRUMENTED_NAME)).booleanValue();
  }

  private void writeEntries(JarOutputStream out) throws IOException {
    for (Iterator i = entries.keySet().iterator(); i.hasNext();) {
      JarEntryWrapper je = (JarEntryWrapper) i.next();
      byte[] classBytes = (byte[]) entries.get(je);
      out.putNextEntry(je.getJarEntry());
      out.write(classBytes);
      out.flush();
    }
  }

  public void setCreationErrorOccurred(boolean errorOccurred) {
    this.creationErrorOccurred = errorOccurred;
  }

  public static void closeQuietly(BootJar bootJar) {
    if (bootJar != null) bootJar.close();
  }

  private synchronized void close() {
    JarOutputStream jarOutput = null;
    try {
      if (state == STATE_OPEN) {
        if (openForWrite && !this.creationErrorOccurred) {
          metaData.write(manifest);
          jarOutput = new JarOutputStream(new FileOutputStream(file, false), manifest);
          writeEntries(jarOutput);
        }
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    } finally {
      IOUtils.closeQuietly(jarOutput);
      if (jarFileInput != null) try {
        jarFileInput.close();
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      }
      state = STATE_CLOSED;
    }
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
      Attributes attributes = manifest.getEntries().get(META_DATA_ATTRIBUTE_NAME);
      if (attributes == null) { throw new InvalidBootJarMetaDataException("Missing attributes in jar manifest."); }

      version = attributes.getValue(VERSION);
      if (version == null) { throw new InvalidBootJarMetaDataException("Missing metadata: version."); }

      String expect_version = VERSION_1_1;
      if (expect_version.equals(version)) {
        vmSignature = attributes.getValue(VM_SIGNATURE);
        if (vmSignature == null) { throw new InvalidJVMVersionException("Missing vm signature."); }
      } else {
        throw new InvalidBootJarMetaDataException("Incompatible DSO meta data: version; expected '" + expect_version
                                                  + "' but was (in boot jar): '" + version);
      }

      tcversion = attributes.getValue(TC_VERSION);
      if (tcversion == null) { throw new InvalidBootJarMetaDataException("Missing metadata: tcversion."); }

      tcmoniker = attributes.getValue(TC_MONIKER);
      if (tcmoniker == null) { throw new InvalidBootJarMetaDataException("Missing metadata: tcmoniker."); }

      ProductInfo productInfo = ProductInfo.getInstance();
      String expect_tcversion = productInfo.version();

      if (productInfo.isDevMode()) {
        logger
            .warn("The value for the DSO meta data, tcversion is: '"
                  + expect_tcversion
                  + "'; this might not be correct, this value is used only under development mode or when tests are being run.");
      }
      if (!productInfo.isDevMode() && !expect_tcversion.equals(tcversion)) { throw new InvalidBootJarMetaDataException(
                                                                                                                       "Incompatible DSO meta data: tcversion; expected '"
                                                                                                                           + expect_tcversion
                                                                                                                           + "' but was (in boot jar): '"
                                                                                                                           + tcversion
                                                                                                                           + "'"); }
    }

    public void write(Manifest manifest) {
      if (VERSION_1_1.equals(version)) {
        ProductInfo productInfo = ProductInfo.getInstance();
        Attributes attributes = new Attributes();
        attributes.put(new Attributes.Name(TC_MONIKER), productInfo.moniker());
        attributes.put(new Attributes.Name(TC_VERSION), productInfo.version());
        attributes.put(new Attributes.Name(VERSION), getVersion());
        attributes.put(new Attributes.Name(VM_SIGNATURE), getVMSignature());
        Object prev = manifest.getEntries().put(META_DATA_ATTRIBUTE_NAME, attributes);
        Assert.assertNull(prev);
      } else {
        throw new AssertionError("Unexptected metadata for version, expecting '" + VERSION_1_1 + "', but was '"
                                 + version + "'");
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

    private JarEntryWrapper(JarEntry jarEntry) {
      this.jarEntry = jarEntry;
    }

    public JarEntry getJarEntry() {
      return jarEntry;
    }

  }

  private static class State {
    private final String name;

    private State(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

}
