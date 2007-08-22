/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.apache.commons.io.FileUtils;
import org.osgi.framework.BundleException;

import com.tc.bundles.exception.InvalidBundleManifestException;
import com.tc.bundles.exception.MissingBundleException;
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
import java.util.jar.JarFile;
import java.util.jar.Manifest;

// TODO: Add some logging
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

      // unable to resolve, bad location - log and skip
      if (location == null) {
        System.err.println("Bad location: " + location);
        continue;
      }

      // the location points to a valid bundle, resolve it's dependencies
      resolveDependencies(location);
    }

    URL[] urls = new URL[registry.size()];
    int j = 0;
    for (Iterator i = registry.iterator(); i.hasNext();) {
      final Entry entry = (Entry) i.next();
      urls[j++] = entry.getLocation();
      //System.err.println("- " + FileUtils.toFile(entry.getLocation()).getName());
    }
    return urls;
  }

  private final void resolveDependencies(final URL location) throws BundleException {
    final Manifest manifest = getManifest(location);
    if (manifest == null) {
      System.err.println("Bad or unreadable: " + location);
      throw new InvalidBundleManifestException("Bad or unreadable: " + location);
    }

    final String[] requirements = BundleSpec.getRequirements(manifest);
    for (int i = 0; i < requirements.length; i++) {
      final BundleSpec spec = new BundleSpec(requirements[i]);
      URL required = findInRegistry(spec);
      if (required == null) {
        required = addToRegistry(spec);
        if (required == null) {
          System.err.println("Required bundle missing: " + required);
          throw new MissingBundleException("Required bundle missing: " + required);
        }
      }
      resolveDependencies(required);
    }

    addToRegistry(location, manifest);
  }

  private final URL addToRegistry(final URL location, final Manifest manifest) {
    final Entry entry = new Entry(location, manifest);
    if (!registry.contains(entry)) {
      registry.add(entry);
      //System.err.println("+ " + FileUtils.toFile(entry.getLocation()).getName());
    }
    return entry.getLocation();
  }

  private final URL addToRegistry(BundleSpec spec) {
    for (int i = 0; i < repositories.length; i++) {
      final URL location = repositories[i];
      // TODO: support other protocol besides file://
      if (location.getProtocol().equalsIgnoreCase("file")) {
        final File repository = new File(location.getFile(), spec.getGroupId().replace('.', File.separatorChar));
        if (!repository.exists() || !repository.isDirectory()) {
          System.err.println("Bad location: " + location);
          continue;
        }

        final Collection jarfiles = FileUtils.listFiles(repository, new String[] { "jar" }, true);
        for (Iterator j = jarfiles.iterator(); j.hasNext();) {
          final File bundleFile = (File) j.next();
          if (!bundleFile.isFile() || !bundleFile.getName().matches(BUNDLE_FILENAME_PATTERN)) {
            System.err.println("Is not considered a bundle file: " + bundleFile.getAbsolutePath());
            continue;
          }

          final Manifest manifest = getManifest(bundleFile);
          if (manifest == null) {
            System.err.println("Bad or unreadable: " + bundleFile.getAbsolutePath());
            continue;
          }

          final String symname = manifest.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
          final String version = manifest.getMainAttributes().getValue(BUNDLE_VERSION);
          if (spec.isCompatible(symname, version)) {
            try {
              return addToRegistry(bundleFile.toURL(), manifest);
            } catch (MalformedURLException e) {
              System.err.println("Unable to convert to URL: " + bundleFile.getAbsolutePath()); // should be fatal?
              return null;
            }
          }
        }
      } else {
        System.err.println("Unsupported repository location protocol: " + location.getProtocol());
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
      System.err.println("IO exception reading: " + location + ", error: " + e.getMessage());
      return null;
    }
  }

  private final URL resolveLocation(final BundleSpec spec) {
    final String groupId = spec.getGroupId();
    final String name = spec.getName();
    final String version = spec.getVersion();
    return resolveLocation(name, version, groupId);
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
    return resolveUrls(repositories, path);
  }

  private URL resolveUrls(final URL[] urls, final String path) {
    if (urls != null && path != null) {
      for (int i = 0; i < urls.length; i++) {
        try {
          final URL testURL = new URL(urls[i].toString() + (urls[i].toString().endsWith("/") ? "" : "/") + path);
          final InputStream is = testURL.openStream();
          is.read();
          is.close();
          return testURL;
        } catch (MalformedURLException e) {
          // Ignore this, the URL is bad
        } catch (IOException e) {
          // Ignore this, the URL is bad
        }
      }
    }
    return null;
  }

  class Entry {
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

}
