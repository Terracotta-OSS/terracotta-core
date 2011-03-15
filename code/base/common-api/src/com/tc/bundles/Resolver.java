/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.apache.commons.io.FileUtils;
import org.osgi.framework.BundleException;

import com.tc.bundles.exception.BundleSpecException;
import com.tc.bundles.exception.MissingBundleException;
import com.tc.bundles.exception.MissingDefaultRepositoryException;
import com.tc.bundles.exception.UnreadableBundleException;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.version.VersionMatcher;
import com.terracottatech.config.Module;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;

public class Resolver {

  static final String            BUNDLE_VERSION        = "Bundle-Version";
  static final String            BUNDLE_SYMBOLICNAME   = "Bundle-SymbolicName";

  private static final int       TOOLKIT_SEARCH_RANGE  = TCPropertiesImpl.getProperties()
                                                           .getInt(TCPropertiesConsts.L1_MODULES_TOOLKIT_SEARCH_RANGE);

  private static final String    TC_PROPERTIES_SECTION = "l1.modules";

  private static final TCLogger  logger                = TCLogging.getLogger(Resolver.class);

  // List of repositories
  private final List<Repository> repositories          = new ArrayList<Repository>();

  // List of Entry objects describing already resolved bundles
  private final List<Entry>      registry              = new ArrayList<Entry>();

  private final VersionMatcher   versionMatcher;

  private ToolkitVersion         maxToolkitVersion     = null;
  private final AtomicBoolean    toolkitVersionFrozen  = new AtomicBoolean();

  public Resolver(final String[] repositoryStrings, final String tcVersion, final String apiVersion)
      throws MissingDefaultRepositoryException {
    this(repositoryStrings, true, tcVersion, apiVersion);
  }

  public Resolver(final String[] repositoryStrings, final boolean injectDefault, final String tcVersion,
                  final String apiVersion) throws MissingDefaultRepositoryException {
    this(repositoryStrings, injectDefault, tcVersion, apiVersion, Collections.EMPTY_LIST);
  }

  public Resolver(final String[] repositoryStrings, final boolean injectDefault, final String tcVersion,
                  final String apiVersion, Collection<Repository> addlRepos) throws MissingDefaultRepositoryException {
    repositories.addAll(addlRepos);
    if (injectDefault) injectDefaultRepositories();

    for (String repositoryString : repositoryStrings) {
      String repository = repositoryString.trim();
      if (repository.length() == 0) continue;
      File repoFile = resolveRepositoryLocation(repository);
      if (repoFile != null) repositories.add(new FSRepository(repoFile, logger));
    }

    if (repositories.isEmpty()) {
      final String msg = "No valid TIM repository locations defined.";
      throw new MissingDefaultRepositoryException(msg);
    }

    versionMatcher = new VersionMatcher(tcVersion, apiVersion);
  }

  public ToolkitVersion getMaxToolkitVersion() {
    return maxToolkitVersion;
  }

  public URL attemptToolkitFreeze() throws BundleException {
    if (maxToolkitVersion == null) { return null; }

    boolean frozen = toolkitVersionFrozen.compareAndSet(false, true);
    if (frozen) {
      logger.info("Freezing toolkit major version from: " + maxToolkitVersion);
      ToolkitVersion toolkitVer = maxToolkitVersion;

      Module toolkitModule = toolkitVer.asModule();
      URL toolkitURL = null;

      for (int i = 0; i < TOOLKIT_SEARCH_RANGE; i++) {
        String resolvedVersion = findNewestVersion(toolkitModule.getGroupId(), toolkitModule.getName(), true);
        if (resolvedVersion != null) {
          toolkitModule.setVersion(resolvedVersion);
          toolkitURL = resolve(toolkitModule);
          if (toolkitURL == null) {
            logger.info("Toolkit version " + resolvedVersion + " isn't a match. Skipping");
          }
          break;
        } else {
          toolkitVer = toolkitVer.nextMinorVersion();
          toolkitModule = toolkitVer.asModule();
        }
      }

      if (toolkitURL == null) {
        //
        throw new BundleException("Cannot resolve a suitable toolkit module for " + maxToolkitVersion);
      }

      logger.info("Actual toolkit API version resolved: " + toolkitVer + " " + toolkitModule);

      Manifest tookitManifest = getManifest(toolkitURL);
      BundleSpec[] requirements = getRequirements(tookitManifest);
      if (requirements.length > 0) {
        // since we skip the toolkit during dependency traversal the toolkit itself must not depend on others
        throw new AssertionError(maxToolkitVersion + " is not allowed to have additional bundle dependencies ["
                                 + toolkitURL + "]");
      }

      return toolkitURL;
    }

    return null;
  }

  private void injectDefaultRepositories() throws MissingDefaultRepositoryException {
    final String installRoot = System.getProperty("tc.install-root");
    if (installRoot != null) {
      final File defaultRepository = new File(installRoot, "platform" + File.separator + "modules");
      if (resolveRepositoryLocation(defaultRepository.getPath()) == null) {
        final String msg = "The default TIM repository does not exist.";
        throw new MissingDefaultRepositoryException(msg, defaultRepository);
      }
      consoleLogger.debug("Appending default TIM repository: '" + defaultRepository + "'");
      repositories.add(new FSRepository(defaultRepository, logger));
    }

    final TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor(TC_PROPERTIES_SECTION);
    final String reposProp = props != null ? props.getProperty("repositories", true) : null;
    if (reposProp == null) return;

    final String[] entries = reposProp.split(",");
    for (String entry : entries) {
      final String trimmed = entry.trim();
      if (trimmed.length() == 0) continue;
      final File repoFile = resolveRepositoryLocation(trimmed);
      if (repoFile == null) {
        consoleLogger.warn("Ignored non-existent TIM repository: '" + ResolverUtils.canonicalize(trimmed) + "'");
        continue;
      }
      consoleLogger.debug("Prepending default TIM repository: '" + ResolverUtils.canonicalize(repoFile) + "'");
      repositories.add(new FSRepository(repoFile, logger));
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

  public final URL resolve(Module module) throws BundleException {
    final String name = module.getName();
    String version = module.getVersion();
    final String groupId = module.getGroupId();

    inspectToolkit(groupId + "." + name, true);

    // Resolve null versions by finding newest candidate in available repositories
    if (version == null) {
      version = findNewestVersion(groupId, name, false);

      if (version == null) {
        String msg = "No version was specified for "
                     + groupId
                     + ":"
                     + name
                     + " in the Terracotta configuration file and no versions were found in the available repositories.";
        throw new BundleException(msg);
      }

      module.setVersion(version);
    }

    // CDV-691: If you are defining a module in the tc-config.xml, the schema requires that you specify
    // a name and version, so this will never happen (although version could still be invalid).
    // But if you define programmatically in a TIM or in a test, it is possible to screw this up.
    if (name == null) {
      String msg = "Invalid module specification (name is required): name=null, version=" + version + ", groupId="
                   + groupId;
      throw new BundleException(msg);
    }

    final URL location;
    try {
      location = resolveLocation(name, version, groupId);
    } catch (Exception e) {
      String msg = "Invalid module specification: name=" + name + ", version=" + version + ", groupId=" + groupId;
      throw new BundleException(msg, e);
    }
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

  private String findNewestVersion(String groupId, String name, boolean verbose) {
    final String symName = MavenToOSGi.artifactIdToSymbolicName(groupId, name);
    String newestVersion = null;

    if (verbose) {
      logger.info("Looking for latest version of " + symName);
    }

    for (Repository repo : repositories) {
      Collection<URL> possibles = repo.search(groupId, name);

      for (URL possible : possibles) {
        Manifest manifest = getManifest(possible);
        if (manifest == null) {
          warn(Message.WARN_FILE_IGNORED_MISSING_MANIFEST, new Object[] { possible });
          continue;
        }

        if (symName.equals(manifest.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME))) {
          String moduleTcVersion = manifest.getMainAttributes().getValue("Terracotta-RequireVersion");

          String timApiVersion = manifest.getMainAttributes().getValue("Terracotta-TIM-API");

          String bundleVersion = manifest.getMainAttributes().getValue(BUNDLE_VERSION);

          if (timApiVersion == null) {
            if (moduleTcVersion != null) {
              timApiVersion = VersionMatcher.ANY_VERSION;
            } else {
              // no TIM API or specific core version specified, ignore this jar
              continue;
            }
          }

          if (moduleTcVersion == null) {
            moduleTcVersion = VersionMatcher.ANY_VERSION;
          }

          if (versionMatcher.matches(moduleTcVersion, timApiVersion)) {
            // logger.info("found matching bundle, version = " + manifest.getMainAttributes().getValue(BUNDLE_VERSION));
            newestVersion = newerVersion(newestVersion, bundleVersion);
            logger.info("new version = " + newestVersion);
          } else {
            if (verbose) {
              logger.info("Skipping module " + symName + " version " + bundleVersion
                          + " because its Terracotta-RequireVersion [" + moduleTcVersion + "] or Terracotta-TIM-API ["
                          + timApiVersion + "] doesn't match current Terracotta version");
            }
          }
        }
      }
    }

    return newestVersion;
  }

  static String newerVersion(String v1, String v2) {
    // Deal with nulls
    if (v1 == null) {
      if (v2 == null) {
        return null;
      } else {
        return v2;
      }
    } else if (v2 == null) { return v1; }

    org.osgi.framework.Version v1v = new org.osgi.framework.Version(v1);
    org.osgi.framework.Version v2v = new org.osgi.framework.Version(v2);

    if (v1v.compareTo(v2v) > 0) {
      return v1;
    } else {
      return v2;
    }
  }

  public final URL[] resolve(Module[] modules) throws BundleException {
    resolveDefaultModules();
    resolveAdditionalModules();
    for (int i = 0; (modules != null) && (i < modules.length); i++)
      resolve(modules[i]);
    return getResolvedURLs();
  }

  public final URL[] getResolvedURLs() {
    int i = 0;
    final URL[] files = new URL[registry.size()];
    for (Entry entry : registry) {
      files[i++] = entry.getLocation();
    }
    return files;
  }

  private URL findJar(String groupId, String name, String version, Check check) {
    if (logger.isDebugEnabled()) logger.debug("Resolving location of " + groupId + ":" + name + ":" + version);

    URL best = null;
    org.osgi.framework.Version bestVersion = null;

    final List<URL> urls = ResolverUtils.searchRepos(repositories, groupId, name, version);
    for (URL url : urls) {
      final Manifest manifest = getManifest(url);
      if (manifest == null) {
        warn(Message.WARN_FILE_IGNORED_MISSING_MANIFEST, new Object[] { url });
        continue;
      }
      if (check.check(url, manifest)) {
        String currentVersionStr = manifest.getMainAttributes().getValue(BUNDLE_VERSION);
        if (best == null) {
          best = url;
          bestVersion = new org.osgi.framework.Version(currentVersionStr);
        } else {
          org.osgi.framework.Version currentVersion = new org.osgi.framework.Version(currentVersionStr);
          if (currentVersion.compareTo(bestVersion) > 0) {
            best = url;
            bestVersion = currentVersion;
          }
        }
      }
    }

    return best;
  }

  protected URL resolveBundle(final BundleSpec spec) {
    final String groupId = spec.getGroupId();
    final String name = spec.getName();
    final String version = spec.getVersion();
    Check check = new Check() {
      public boolean check(URL bundle, Manifest manifest) {
        final String n = manifest.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
        final String v = manifest.getMainAttributes().getValue(BUNDLE_VERSION);
        return spec.isCompatible(n, v);
      }
    };
    return findJar(groupId, name, version, check);
  }

  protected URL resolveLocation(final String name, final String version, final String groupId) {
    final String symname = MavenToOSGi.artifactIdToSymbolicName(groupId, name);
    final Version osgiVersion = Version.parse(MavenToOSGi.projectVersionToBundleVersion(version));
    Check check = new Check() {
      public boolean check(URL bundle, Manifest manifest) {
        if (!isBundleMatch(bundle, manifest, symname, osgiVersion)) return false;
        return true;
      }
    };
    URL jar = findJar(groupId, name, version, check);
    if (jar != null) addToRegistry(jar, getManifest(jar));
    return jar;
  }

  private boolean isBundleMatch(URL bundle, Manifest manifest, String symname, Version version) {
    Assert.assertNotNull(manifest);
    if (logger.isDebugEnabled()) logger.debug("Checking " + bundle + " for " + symname + ":" + version);

    final String bundlesymname = manifest.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
    final String bundleversion = manifest.getMainAttributes().getValue(BUNDLE_VERSION);
    try {
      return BundleSpecUtil.isMatchingSymbolicName(symname, bundlesymname)
             && version.equals(Version.parse(bundleversion));
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
      for (String defaultSpec : defaultModulesSpec) {
        BundleSpec spec = BundleSpec.newInstance(defaultSpec);
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
    for (String addlSpec : additionalModulesSpec) {
      BundleSpec spec = BundleSpec.newInstance(addlSpec);
      DependencyStack dependencyStack = new DependencyStack();
      dependencyStack.push(spec.getSymbolicName(), spec.getVersion());
      ensureBundle(spec, dependencyStack);
    }
  }

  private BundleSpec[] getRequirements(Manifest manifest) {
    List requirementList = new ArrayList();
    String[] manifestRequirements = BundleSpec.getRequirements(manifest);
    for (String manifestRequirement : manifestRequirements) {
      requirementList.add(BundleSpec.newInstance(manifestRequirement));
    }
    return (BundleSpec[]) requirementList.toArray(new BundleSpec[0]);
  }

  private void resolveDependencies(final URL location, DependencyStack dependencyStack) throws BundleException {
    final Manifest manifest = getManifest(location);
    if (manifest == null) {
      String msg = formatMessage(Message.ERROR_BUNDLE_UNREADABLE, new Object[] { ResolverUtils.canonicalize(location) });
      throw new UnreadableBundleException(msg, location);
    }
    final BundleSpec[] requirements = getRequirements(manifest);
    DependencyStack stack = dependencyStack.push(new DependencyStack());
    for (final BundleSpec spec : requirements) {
      stack.push(spec.getSymbolicName(), spec.getVersion());
      try {
        ensureBundle(spec, stack);
      } catch (MissingBundleException e) {
        throw new MissingBundleException(e.getMessage(), spec.getGroupId(), spec.getName(), spec.getVersion(),
                                         repositories, dependencyStack);
      }
    }
    addToRegistry(location, manifest);
  }

  static void validateBundleSpec(final BundleSpec spec) throws BundleException {
    if (!spec.isVersionSpecified()) throw BundleSpecException.unspecifiedVersion(spec);
    // if (!spec.isVersionSpecifiedAbsolute()) throw BundleSpecException.absoluteVersionRequired(spec);
  }

  private void ensureBundle(final BundleSpec spec, DependencyStack stack) throws BundleException {
    if (inspectToolkit(spec.getSymbolicName(), false)) { return; }

    validateBundleSpec(spec);
    URL required = findInRegistry(spec);
    if (required == null) {
      required = resolveBundle(spec);
      if (required == null) {
        String msg = formatMessage(Message.ERROR_BUNDLE_DEPENDENCY_UNRESOLVED,
                                   new Object[] { spec.getName(), spec.getVersion(), spec.getGroupId() });
        throw new MissingBundleException(msg, spec.getGroupId(), spec.getName(), spec.getVersion(), repositories, stack);
      }
      addToRegistry(required, getManifest(required));
      resolveDependencies(required, stack);
    }
  }

  private boolean inspectToolkit(String symbolicName, boolean topLevelReference) throws BundleException {
    Matcher toolkitMatcher = ToolkitConstants.TOOLKIT_SYMBOLIC_NAME_PATTERN.matcher(symbolicName);
    if (toolkitMatcher.matches()) {
      if (topLevelReference && toolkitVersionFrozen.get()) { return false; }

      ToolkitVersion ver = new ToolkitVersion(toolkitMatcher.group(1), toolkitMatcher.group(2), toolkitMatcher.group(3));

      if (this.maxToolkitVersion == null) {
        logger.info("First toolkit reference found: " + ver);
        maxToolkitVersion = ver;
        if (topLevelReference) {
          attemptToolkitFreeze();
        }
      } else {
        if (maxToolkitVersion.getMajor() != ver.getMajor()) {
          // Major versions of all toolkit references must be equal
          throw new ConflictingModuleException(ToolkitConstants.TOOLKIT_SYMBOLIC_NAME_PATTERN.pattern(),
                                               maxToolkitVersion.toString(), ver.toString());
        }

        // prefer the EE artifact if found
        if (!maxToolkitVersion.isEE() && ver.isEE()) {
          logger.info("EE tookit reference found: " + ver);
          maxToolkitVersion = new ToolkitVersion(maxToolkitVersion.getMajor(), maxToolkitVersion.getMinor(), true);
        }

        if (maxToolkitVersion.getMinor() < ver.getMinor()) {
          if (toolkitVersionFrozen.get()) {
            // once the version is frozen we can't be asked to load another (higher) version
            throw new ConflictingModuleException(ToolkitConstants.TOOLKIT_SYMBOLIC_NAME_PATTERN.pattern(),
                                                 maxToolkitVersion.toString(), ver.toString());
          }

          logger.info("Higher toolkit version reference found: " + ver);
          maxToolkitVersion = new ToolkitVersion(ver.getMajor(), ver.getMinor(), maxToolkitVersion.isEE() || ver.isEE());
        }
      }

      return true;
    }

    return false;
  }

  private URL addToRegistry(final URL location, final Manifest manifest) {
    final Entry entry = new Entry(location, manifest);
    if (!registry.contains(entry)) {
      // Check if we're somehow resolving more than one version of the "same" TIM
      for (Entry existing : registry) {
        if (existing.getSymbolicName().equals(entry.getSymbolicName())) {
          // XXX: It'd be nice to provide deeper context here (ie. something like
          // mvn dependency:tree that would show how we got to this state)
          throw new ConflictingModuleException(existing.getSymbolicName(), existing.getVersion(), entry.getVersion());
        }
      }

      registry.add(entry);
    }
    return entry.getLocation();
  }

  private URL findInRegistry(BundleSpec spec) {
    URL location = null;
    for (Entry entry : registry) {
      if (spec.isCompatible(entry.getSymbolicName(), entry.getVersion())) {
        location = entry.getLocation();
        break;
      }
    }
    return location;
  }

  static Manifest getManifest(final URL location) {
    JarInputStream in = null;
    try {
      in = new JarInputStream(new BufferedInputStream(location.openStream()));
      return in.getManifest();
    } catch (IOException e) {
      logger.warn("Exception reading " + location, e);
      return null;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ioe) {
          // ignore
        }
      }
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

  private interface Check {
    boolean check(URL bundle, Manifest manifest);
  }

  private static class Entry {
    private final URL      location;
    private final Manifest manifest;

    public Entry(final URL location, final Manifest manifest) {
      this.location = location;
      this.manifest = manifest;
    }

    public String getVersion() {
      return manifest.getMainAttributes().getValue(BUNDLE_VERSION);
    }

    public String getSymbolicName() {
      return manifest.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
    }

    public URL getLocation() {
      return location;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) return true;
      if (!(object instanceof Entry)) return false;
      final Entry entry = (Entry) object;
      return location.equals(entry.getLocation()) && getVersion().equals(entry.getVersion())
             && getSymbolicName().equals(entry.getSymbolicName());
    }

    private static final int SEED1 = 18181;
    private static final int SEED2 = 181081;

    @Override
    public int hashCode() {
      int result = SEED1;
      result = hash(result, this.location);
      result = hash(result, this.manifest);
      return result;
    }

    private static int hash(int seed, int value) {
      return SEED2 * seed + value;
    }

    private static int hash(int seed, Object object) {
      int result = seed;
      if (object == null) {
        result = hash(result, 0);
      } else {
        result = hash(result, object);
      }
      return result;
    }
  }

  private static final class Message {
    static final Message WARN_FILE_IGNORED_MISSING_MANIFEST = new Message("warn.file.ignored.missing-manifest");
    // static final Message WARN_REPOSITORY_PROTOCOL_UNSUPPORTED = new Message("warn.repository.protocol.unsupported");
    static final Message ERROR_BUNDLE_UNREADABLE            = new Message("error.bundle.unreadable");
    static final Message ERROR_BUNDLE_UNRESOLVED            = new Message("error.bundle.unresolved");
    static final Message ERROR_BUNDLE_DEPENDENCY_UNRESOLVED = new Message("error.bundle-dependency.unresolved");

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
      resourceBundle = ResourceBundle.getBundle(Resolver.class.getName(), Locale.getDefault(),
                                                Resolver.class.getClassLoader());
    } catch (MissingResourceException mre) {
      throw new RuntimeException("No resource bundle exists for " + Resolver.class.getName());
    } catch (Throwable t) {
      throw new RuntimeException("Unexpected error loading resource bundle for " + Resolver.class.getName(), t);
    }
  }
}
