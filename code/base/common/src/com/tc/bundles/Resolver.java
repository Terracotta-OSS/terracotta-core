/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.apache.commons.io.FileUtils;
import org.osgi.framework.BundleException;

import com.tc.bundles.exception.BundleSpecException;
import com.tc.bundles.exception.MissingDefaultRepositoryException;
import com.tc.bundles.exception.UnreadableBundleException;
import com.tc.bundles.exception.MissingBundleException;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.terracottatech.config.Module;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList; // import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Resolver {

  private static final String   BUNDLE_VERSION        = "Bundle-Version";
  private static final String   BUNDLE_SYMBOLICNAME   = "Bundle-SymbolicName";

  private static final String   TC_PROPERTIES_SECTION = "l1.modules";

  private static final TCLogger logger                = TCLogging.getLogger(Resolver.class);

  // List of File where each is a repository root
  private final List            repositories          = new ArrayList();

  // List of Entry objects describing already resolved bundles
  private final List            registry              = new ArrayList();

  public Resolver(final String[] repositoryStrings) throws MissingDefaultRepositoryException {
    this(repositoryStrings, true);
  }

  public Resolver(final String[] repositoryStrings, final boolean injectDefault)
      throws MissingDefaultRepositoryException {
    if (injectDefault) injectDefaultRepositories();

    for (int i = 0; i < repositoryStrings.length; i++) {
      String repository = repositoryStrings[i].trim();
      if (repository.length() == 0) continue;
      File repoFile = resolveRepositoryLocation(repository);
      if (repoFile != null) repositories.add(repoFile);
    }

    if (repositories.isEmpty()) {
      final String msg = "No valid TIM repository locations defined.";
      throw new MissingDefaultRepositoryException(msg);
    }
  }

  private void injectDefaultRepositories() throws MissingDefaultRepositoryException {
    final String installRoot = System.getProperty("tc.install-root");
    if (installRoot != null) {
      final File defaultRepository = new File(installRoot, "modules");
      if (resolveRepositoryLocation(defaultRepository.getPath()) == null) {
        final String msg = "The default TIM repository does not exist.";
        throw new MissingDefaultRepositoryException(msg, defaultRepository);
      }
      consoleLogger.debug("Appending default TIM repository: '" + defaultRepository + "'");
      repositories.add(defaultRepository);
    }

    final TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor(TC_PROPERTIES_SECTION);
    final String reposProp = props != null ? props.getProperty("repositories", true) : null;
    if (reposProp == null) return;

    final String[] entries = reposProp.split(",");
    for (int i = 0; i < entries.length; i++) {
      final String entry = entries[i].trim();
      if (entry.length() == 0) continue;
      final File repoFile = resolveRepositoryLocation(entry);
      if (repoFile == null) {
        consoleLogger.warn("Ignored non-existent TIM repository: '" + ResolverUtils.canonicalize(entry) + "'");
        continue;
      }
      consoleLogger.debug("Prepending default TIM repository: '" + ResolverUtils.canonicalize(repoFile) + "'");
      repositories.add(repoFile);
    }
  }

  /**
   * Resolve string as repository location - try to understand as both file path and as URL.
   * 
   * @param repository Repository file path or URL
   */
  static File resolveRepositoryLocation(String repository) {
    Assert.assertNotNull(repository);

    // try as file
    File file = new File(repository);
    if (file.isDirectory()) return file;

    // try as URL
    URL url = null;
    try {
      url = new URL(repository);
    } catch (MalformedURLException e) {
      // handle later
    }

    if (url == null) {
      consoleLogger.warn("Skipping repository location: '" + repository
                         + "', it either does not exist or is not a directory; make sure that the path '"
                         + ResolverUtils.canonicalize(repository) + "' actually exists.");
      return null;
    }

    // if URL format, then make sure it's using the file: protocol
    if (!url.getProtocol().equalsIgnoreCase("file")) {
      consoleLogger.warn("Skipping repository URL: '" + repository + "', only the 'file:' protocol is supported.");
      return null;
    }

    // deprecated but allowed file URL
    file = FileUtils.toFile(url);
    if (!file.isDirectory()) {
      consoleLogger.warn("Skipping repository URL: '" + repository
                         + "', it either does not exist nor resolve to a directory.");
      return null;
    }

    consoleLogger.warn("Repository location: '" + repository
                       + "' defined as URL, this usage is deprecated and will be removed in the future.");
    return file;
  }

  public final File resolve(Module module) throws BundleException {
    final String name = module.getName();
    final String version = module.getVersion();
    final String groupId = module.getGroupId();

    // CDV-691: If you are defining a module in the tc-config.xml, the schema requires that you specify
    // a name and version, so this will never happen (although version could still be invalid).
    // But if you define programmatically in a TIM or in a test, it is possible to screw this up.
    if (name == null || version == null) {
      String msg = "Invalid module specification (name and version are required): name=" + name + ", version="
                   + version + ", groupId=" + groupId;
      throw new BundleException(msg);
    }

    final File location = resolveLocation(name, version, groupId);
    if (location == null) {
      final String msg = formatMessage(Message.ERROR_BUNDLE_UNRESOLVED, new Object[] { name, version, groupId });
      throw new MissingBundleException(msg, groupId, name, version, repositories);
    }

    logger.info("Resolved TIM " + groupId + ":" + name + ":" + version + " from " + location);
    DependencyStack dependencyStack = new DependencyStack();
    dependencyStack.push(module.getGroupId(), module.getName(), module.getVersion());
    resolveDependencies(location, dependencyStack);
    return location;
  }

  public final File[] resolve(Module[] modules) throws BundleException {
    resolveDefaultModules();
    resolveAdditionalModules();
    for (int i = 0; (modules != null) && (i < modules.length); i++)
      resolve(modules[i]);
    return getResolvedFiles();
  }

  public final File[] getResolvedFiles() {
    int j = 0;
    final File[] files = new File[registry.size()];
    for (Iterator i = registry.iterator(); i.hasNext();) {
      final Entry entry = (Entry) i.next();
      files[j++] = entry.getLocation();
    }
    return files;
  }

  private File findJar(String groupId, String name, String version, Locator locator) {
    if (logger.isDebugEnabled()) logger.debug("Resolving location of " + groupId + ":" + name + ":" + version);
    final List paths = ResolverUtils.searchPathnames(repositories, groupId, name, version);
    for (Iterator i = paths.iterator(); i.hasNext();) {

      final File bundle = new File((String) i.next());
      if (!bundle.exists() || !bundle.isFile()) continue;

      final Manifest manifest = getManifest(bundle);
      if (manifest == null) {
        warn(Message.WARN_FILE_IGNORED_MISSING_MANIFEST, new Object[] { bundle.getName() });
        continue;
      }
      if (locator.check(bundle, manifest)) return bundle;
    }
    return null;
  }

  protected File resolveBundle(final BundleSpec spec) {
    final String groupId = spec.getGroupId();
    final String name = spec.getName();
    final String version = spec.getVersion();
    Locator locator = new Locator() {
      public boolean check(File bundle, Manifest manifest) {
        final String n = manifest.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
        final String v = manifest.getMainAttributes().getValue(BUNDLE_VERSION);
        return spec.isCompatible(n, v);
      }
    };
    return findJar(groupId, name, version, locator);
  }

  protected File resolveLocation(final String name, final String version, final String groupId) {
    final String symname = MavenToOSGi.artifactIdToSymbolicName(groupId, name);
    final Version osgiVersion = Version.parse(MavenToOSGi.projectVersionToBundleVersion(version));
    Locator locator = new Locator() {
      public boolean check(File bundle, Manifest manifest) {
        if (!isBundleMatch(bundle, manifest, symname, osgiVersion)) return false;
        addToRegistry(bundle, manifest);
        return true;
      }
    };
    return findJar(groupId, name, version, locator);
  }

  private boolean isBundleMatch(File bundle, Manifest manifest, String symname, Version version) {
    Assert.assertNotNull(manifest);
    if (logger.isDebugEnabled()) logger.debug("Checking " + bundle + " for " + symname + ":" + version);

    final String bundlesymname = manifest.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
    final String bundleversion = manifest.getMainAttributes().getValue(BUNDLE_VERSION);
    try {
      return BundleSpec.isMatchingSymbolicName(symname, bundlesymname) && version.equals(Version.parse(bundleversion));
    } catch (NumberFormatException e) { // thrown by parseVersion()
      consoleLogger.warn("Bad version attribute in TIM manifest from jar file: '" + ResolverUtils.canonicalize(bundle)
                         + "', version='" + bundleversion + "'.  Skipping...", e);
      return false;
    }
  }

  private void resolveDefaultModules() throws BundleException {
    final TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor(TC_PROPERTIES_SECTION);
    final String defaultModulesProp = props != null ? props.getProperty("default") : null;

    if (defaultModulesProp == null) {
      consoleLogger.debug("No implicit modules were loaded because the l1.modules.default property is not set.");
      return;
    }

    final String[] defaultModulesSpec = BundleSpec.getRequirements(defaultModulesProp);
    if (defaultModulesSpec.length > 0) {
      for (int i = 0; i < defaultModulesSpec.length; i++) {
        BundleSpec spec = BundleSpec.newInstance(defaultModulesSpec[i]);
        DependencyStack dependencyStack = new DependencyStack();
        dependencyStack.push(spec.getSymbolicName(), spec.getVersion());
        ensureBundle(spec, dependencyStack);
      }
      return;
    }
    consoleLogger.debug("No implicit modules were loaded because the l1.modules.default property is empty.");
  }

  private void resolveAdditionalModules() throws BundleException {
    final TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor(TC_PROPERTIES_SECTION);
    final String additionalModulesProp = props != null ? props.getProperty("additional") : null;
    if (additionalModulesProp == null) return;
    String[] additionalModulesSpec = BundleSpec.getRequirements(additionalModulesProp);
    for (int i = 0; i < additionalModulesSpec.length; i++) {
      BundleSpec spec = BundleSpec.newInstance(additionalModulesSpec[i]);
      DependencyStack dependencyStack = new DependencyStack();
      dependencyStack.push(spec.getSymbolicName(), spec.getVersion());
      ensureBundle(spec, dependencyStack);
    }
  }

  private BundleSpec[] getRequirements(Manifest manifest) {
    List requirementList = new ArrayList();
    String[] manifestRequirements = BundleSpec.getRequirements(manifest);
    for (int i = 0; i < manifestRequirements.length; i++) {
      requirementList.add(BundleSpec.newInstance(manifestRequirements[i]));
    }
    return (BundleSpec[]) requirementList.toArray(new BundleSpec[0]);
  }

  private void resolveDependencies(final File location, Stack dependencyStack) throws BundleException {
    final Manifest manifest = getManifest(location);
    if (manifest == null) {
      String msg = formatMessage(Message.ERROR_BUNDLE_UNREADABLE, new Object[] { location.getName(),
          ResolverUtils.canonicalize(location.getParentFile()) });
      throw new UnreadableBundleException(msg, location);
    }
    final BundleSpec[] requirements = getRequirements(manifest);
    DependencyStack stack = (DependencyStack) dependencyStack.push(new DependencyStack());
    for (int i = 0; i < requirements.length; i++) {
      final BundleSpec spec = requirements[i];
      stack.push(spec.getSymbolicName(), spec.getVersion());
      // try {
      ensureBundle(spec, stack);
      // } catch (MissingBundleException e) {
      // throw new MissingBundleException(e.getMessage(), spec.getGroupId(), spec.getName(), spec.getVersion(),
      // repositories, dependencyStack);
      // }
    }
    addToRegistry(location, manifest);
  }

  static void validateBundleSpec(final BundleSpec spec) throws BundleException {
    if (!spec.isVersionSpecified()) throw BundleSpecException.unspecifiedVersion(spec);
    if (!spec.isVersionSpecifiedAbsolute()) throw BundleSpecException.absoluteVersionRequired(spec);
  }

  private void ensureBundle(final BundleSpec spec, Stack dependencyStack) throws BundleException {
    validateBundleSpec(spec);
    File required = findInRegistry(spec);
    if (required == null) {
      required = resolveBundle(spec);
      if (required == null) {
        String msg = formatMessage(Message.ERROR_BUNDLE_DEPENDENCY_UNRESOLVED, new Object[] { spec.getName(),
            spec.getVersion(), spec.getGroupId() });
        throw new MissingBundleException(msg, spec.getGroupId(), spec.getName(), spec.getVersion(), repositories,
                                         dependencyStack);
      }
      addToRegistry(required, getManifest(required));
      resolveDependencies(required, dependencyStack);
    }
  }

  private File addToRegistry(final File location, final Manifest manifest) {
    final Entry entry = new Entry(location, manifest);
    if (!registry.contains(entry)) registry.add(entry);
    return entry.getLocation();
  }

  private File findInRegistry(BundleSpec spec) {
    File location = null;
    for (Iterator i = registry.iterator(); i.hasNext();) {
      final Entry entry = (Entry) i.next();
      if (spec.isCompatible(entry.getSymbolicName(), entry.getVersion())) {
        location = entry.getLocation();
        break;
      }
    }
    return location;
  }

  private Manifest getManifest(final File file) {
    try {
      return getManifest(file.toURI().toURL());
    } catch (MalformedURLException e) {
      return null;
    }
  }

  private Manifest getManifest(final URL location) {
    try {
      final JarFile bundle = new JarFile(FileUtils.toFile(location));
      return bundle.getManifest();
    } catch (IOException e) {
      return null;
    }
  }

  private String warn(final Message message, final Object[] arguments) {
    final String msg = formatMessage(message, arguments);
    logger.warn(msg);
    return msg;
  }

  private static String formatMessage(final Message message, final Object[] arguments) {
    return MessageFormat.format(resourceBundle.getString(message.key()), arguments);
  }

  private interface Locator {
    boolean check(File bundle, Manifest manifest);
  }

  private final class Entry {
    private File     location;
    private Manifest manifest;

    public Entry(final File location, final Manifest manifest) {
      this.location = location;
      this.manifest = manifest;
    }

    public String getVersion() {
      return manifest.getMainAttributes().getValue(BUNDLE_VERSION);
    }

    public String getSymbolicName() {
      return manifest.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
    }

    public File getLocation() {
      return location;
    }

    public boolean equals(Object object) {
      if (this == object) return true;
      if (!(object instanceof Entry)) return false;
      final Entry entry = (Entry) object;
      return location.equals(entry.getLocation()) && getVersion().equals(entry.getVersion())
             && getSymbolicName().equals(entry.getSymbolicName());
    }

    private static final int SEED1 = 18181;
    private static final int SEED2 = 181081;

    public int hashCode() {
      int result = SEED1;
      result = hash(result, this.location);
      result = hash(result, this.manifest);
      return result;
    }

    private int hash(int seed, int value) {
      return SEED2 * seed + value;
    }

    private int hash(int seed, Object object) {
      int result = seed;
      if (object == null) {
        result = hash(result, 0);
      } else {
        result = hash(result, object);
      }
      return result;
    }
  }

  private final class DependencyStack extends Stack {

    public void push(String groupId, String artifactId, String version) {
      StringBuffer buf = new StringBuffer(artifactId);
      buf.append(" version ");
      buf.append(OSGiToMaven.bundleVersionToProjectVersion(version)).append(" (");
      if (groupId.length() > 0) buf.append("group-id: ").append(groupId).append(", ");
      buf.append("file: ").append(OSGiToMaven.makeBundleFilename(artifactId, version, false)).append(")");
      push(buf.toString());
    }

    public void push(String symbolicName, String version) {
      push(OSGiToMaven.groupIdFromSymbolicName(symbolicName), OSGiToMaven.artifactIdFromSymbolicName(symbolicName),
           version);
    }
  }

  private static final class Message {

    static final Message WARN_FILE_IGNORED_MISSING_MANIFEST   = new Message("warn.file.ignored.missing-manifest");
    static final Message WARN_REPOSITORY_PROTOCOL_UNSUPPORTED = new Message("warn.repository.protocol.unsupported");
    static final Message ERROR_BUNDLE_UNREADABLE              = new Message("error.bundle.unreadable");
    static final Message ERROR_BUNDLE_UNRESOLVED              = new Message("error.bundle.unresolved");
    static final Message ERROR_BUNDLE_DEPENDENCY_UNRESOLVED   = new Message("error.bundle-dependency.unresolved");

    private final String resourceBundleKey;

    private Message(final String resourceBundleKey) {
      this.resourceBundleKey = resourceBundleKey;
    }

    String key() {
      return resourceBundleKey;
    }
  }

  private static final TCLogger       consoleLogger = CustomerLogging.getConsoleLogger();
  private static final ResourceBundle resourceBundle;

  static {
    try {
      resourceBundle = ResourceBundle.getBundle(Resolver.class.getName(), Locale.getDefault(), Resolver.class
          .getClassLoader());
    } catch (MissingResourceException mre) {
      throw new RuntimeException("No resource bundle exists for " + Resolver.class.getName());
    } catch (Throwable t) {
      throw new RuntimeException("Unexpected error loading resource bundle for " + Resolver.class.getName(), t);
    }
  }
}
