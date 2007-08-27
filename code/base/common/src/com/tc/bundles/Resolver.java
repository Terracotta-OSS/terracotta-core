/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.apache.commons.io.FileUtils;
import org.osgi.framework.BundleException;

import com.tc.bundles.exception.InvalidBundleManifestException;
import com.tc.bundles.exception.MissingBundleException;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.terracottatech.config.Module;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Resolver {

  private static final String BUNDLE_VERSION            = "Bundle-Version";
  private static final String BUNDLE_SYMBOLICNAME       = "Bundle-SymbolicName";

  private static final String BUNDLE_FILENAME_EXT       = ".jar";
  private static final String BUNDLE_PATH               = "{0}-{1}" + BUNDLE_FILENAME_EXT;
  private static final String BUNDLE_VERSION_REGEX      = "[0-9]+\\.[0-9]+\\.[0-9]+";
  private static final String BUNDLE_FILENAME_REGEX     = ".+-";
  private static final String BUNDLE_FILENAME_EXT_REGEX = "\\" + BUNDLE_FILENAME_EXT;
  private static final String BUNDLE_FILENAME_PATTERN   = "^" + BUNDLE_FILENAME_REGEX + BUNDLE_VERSION_REGEX
                                                            + BUNDLE_FILENAME_EXT_REGEX + "$";

  private URL[]               repositories;
  private Module[]            modules;
  private List                registry;

  public Resolver(final URL[] repositories, final Module[] modules) {
    this.repositories = repositories;
    this.modules = modules;
    this.registry = new ArrayList();
  }

  public final URL[] resolve() throws BundleException {
    for (int i = 0; i < modules.length; i++) {
      final URL location = resolveLocation(modules[i]);
      if (location == null) {
        final String msg = error(Message.ERROR_BUNDLE_UNRESOLVED, new Object[] { modules[i].getName(),
            modules[i].getVersion(), modules[i].getGroupId() });
        throw new MissingBundleException(msg);
      }
      resolveDependencies(location);
    }
    return getResolvedUrls();
  }

  private final URL[] getResolvedUrls() {
    int j = 0;
    final URL[] urls = new URL[registry.size()];
    for (Iterator i = registry.iterator(); i.hasNext();) {
      final Entry entry = (Entry) i.next();
      urls[j++] = entry.getLocation();
    }
    return urls;
  }

  private final void resolveDependencies(final URL location) throws BundleException {
    final Manifest manifest = getManifest(location);
    if (manifest == null) {
      final String msg = error(Message.ERROR_BUNDLE_UNREADABLE, new Object[] { FileUtils.toFile(location).getName(),
          FileUtils.toFile(location).getParent() });
      throw new InvalidBundleManifestException(msg);
    }

    final String[] requirements = BundleSpec.getRequirements(manifest);
    for (int i = 0; i < requirements.length; i++) {
      final BundleSpec spec = new BundleSpec(requirements[i]);
      URL required = findInRegistry(spec);
      if (required == null) {
        required = addToRegistry(spec);
        if (required == null) {
          final String msg = error(Message.ERROR_BUNDLE_DEPENDENCY_UNRESOLVED, new Object[] { spec.getName(),
              spec.getVersion(), spec.getGroupId() });
          throw new MissingBundleException(msg);
        }
      }
      resolveDependencies(required);
    }
    
    addToRegistry(location, manifest);
  }

  private final URL addToRegistry(final URL location, final Manifest manifest) {
    final Entry entry = new Entry(location, manifest);
    if (!registry.contains(entry)) registry.add(entry);
    return entry.getLocation();
  }

  private final URL addToRegistry(BundleSpec spec) {
    for (int i = 0; i < repositories.length; i++) {
      final URL location = repositories[i];
      // TODO: support other protocol besides file://
      if (location.getProtocol().equalsIgnoreCase("file")) {
        final File repository = new File(location.getFile(), spec.getGroupId().replace('.', File.separatorChar));
        if (!repository.exists() || !repository.isDirectory()) {
          warn(Message.WARN_REPOSITORY_UNRESOLVED, new Object[] { location });
          continue;
        }

        final Collection jarfiles = FileUtils.listFiles(repository, new String[] { "jar" }, true);
        for (Iterator j = jarfiles.iterator(); j.hasNext();) {
          final File bundleFile = (File) j.next();
          if (!bundleFile.isFile() || !bundleFile.getName().matches(BUNDLE_FILENAME_PATTERN)) {
            warn(Message.WARN_FILE_IGNORED_INVALID_NAME, new Object[] { bundleFile.getName(), BUNDLE_FILENAME_PATTERN });
            continue;
          }

          final Manifest manifest = getManifest(bundleFile);
          if (manifest == null) {
            warn(Message.WARN_FILE_IGNORED_MISSING_MANIFEST, new Object[] { bundleFile.getName() });
            continue;
          }

          final String symname = manifest.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
          final String version = manifest.getMainAttributes().getValue(BUNDLE_VERSION);
          if (spec.isCompatible(symname, version)) {
            try {
              return addToRegistry(bundleFile.toURL(), manifest);
            } catch (MalformedURLException e) {
              error(Message.ERROR_BUNDLE_MALFORMED_URL, new Object[] { bundleFile.getName() }); // should be fatal???
              return null;
            }
          }
        }
      } else {
        warn(Message.WARN_REPOSITORY_PROTOCOL_UNSUPPORTED, new Object[] { location.getProtocol() });
      }
    }
    return null;
  }

  private final URL findInRegistry(BundleSpec spec) {
    URL location = null;
    for (Iterator i = registry.iterator(); i.hasNext();) {
      final Entry entry = (Entry) i.next();
      if (spec.isCompatible(entry.getSymbolicName(), entry.getVersion())) {
        location = entry.getLocation();
        break;
      }
    }
    return location;
  }

  private final Manifest getManifest(final File file) {
    try {
      return getManifest(file.toURL());
    } catch (MalformedURLException e) {
      return null;
    }
  }

  private final Manifest getManifest(final URL location) {
    try {
      final JarFile bundle = new JarFile(FileUtils.toFile(location));
      return bundle.getManifest();
    } catch (IOException e) {
      // ignore and return null, this is a bad URL
      return null;
      // warn(Message.WARN_EXCEPTION_OCCURED, new Object[] { location, e.getMessage() });
    }
  }

  private final URL resolveLocation(final Module module) {
    final String groupId = module.getGroupId();
    final String name = module.getName();
    final String version = module.getVersion();
    return resolveLocation(name, version, groupId);
  }

  private final URL resolveLocation(final String name, final String version, final String groupId) {
    final String base = groupId.replace('.', File.separatorChar);
    final String path = MessageFormat.format("{2}{3}{0}{3}{1}{3}" + BUNDLE_PATH, new String[] { name, version, base,
        File.separator });
    return resolveUrls(path);
  }

  private URL resolveUrls(final String path) {
    for (int i = 0; i < repositories.length; i++) {
      URL location = null;
      InputStream is = null;
      try {
        location = new URL(repositories[i].toString() + (repositories[i].toString().endsWith("/") ? "" : "/") + path);
        is = location.openStream();
        is.read();
        is.close();
        return location;
      } catch (IOException e) {
        // ignore bad or unreachable URL
      } finally {
        try {
          if (is != null) is.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
    return null;
  }

  private class Entry {
    private URL      location;
    private Manifest manifest;

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
      } else if (!object.getClass().isArray()) {
        result = hash(result, object);
      } else {
        int len = Array.getLength(object);
        for (int i = 0; i < len; i++) {
          Object o = Array.get(object, i);
          result = hash(result, o);
        }
      }
      return result;
    }
  }

  private static class Message {

    static final Message WARN_BUNDLE_UNRESOLVED               = new Message("warn.bundle.unresolved");
    static final Message WARN_REPOSITORY_UNRESOLVED           = new Message("warn.repository.unresolved");
    static final Message WARN_FILE_IGNORED_INVALID_NAME       = new Message("warn.file.ignored.invalid-name");
    static final Message WARN_FILE_IGNORED_MISSING_MANIFEST   = new Message("warn.file.ignored.missing-manifest");
    static final Message WARN_REPOSITORY_PROTOCOL_UNSUPPORTED = new Message("warn.repository.protocol.unsupported");
    static final Message WARN_EXCEPTION_OCCURED               = new Message("warn.exception.occured");
    static final Message ERROR_BUNDLE_UNREADABLE              = new Message("error.bundle.unreadable");
    static final Message ERROR_BUNDLE_UNRESOLVED              = new Message("error.bundle.unresolved");
    static final Message ERROR_BUNDLE_DEPENDENCY_UNRESOLVED   = new Message("error.bundle-dependency.unresolved");
    static final Message ERROR_BUNDLE_MALFORMED_URL           = new Message("error.bundle.malformed-url");

    private final String resourceBundleKey;

    private Message(final String resourceBundleKey) {
      this.resourceBundleKey = resourceBundleKey;
    }

    String key() {
      return resourceBundleKey;
    }
  }

  private static final TCLogger       logger = CustomerLogging.getConsoleLogger();
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

  private final String warn(final Message message, final Object[] arguments) {
    final String msg = formatMessage(message, arguments);
    logger.warn(msg);
    return msg;
  }

  private final String error(final Message message, final Object[] arguments) {
    final String msg = formatMessage(message, arguments);
    logger.error(message);
    return msg;
  }

  private final static String formatMessage(final Message message, final Object[] arguments) {
    return MessageFormat.format(resourceBundle.getString(message.key()), arguments);
  }
}
