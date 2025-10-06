/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.classloader;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.server.Directories;
import com.tc.util.ManagedServiceLoader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Top level service locator class used to identify and isolate service
 * dependencies in its own classloader. Caching will allow for one classloader
 * per root jar.
 */
public class ServiceLocator extends ManagedServiceLoader {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceLocator.class);
  private static final boolean STRICT = !TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.PLUGIN_CLASSLOADER_COMPATIBILITY);
  private static final Path PLUGIN_PATH = getPluginPath();
  private final ClassLoader defaultClassLoader;
  private final boolean verify;
  private final Map<String, ClassLoader> locationCache = new ConcurrentHashMap<>();

  public ServiceLocator(ClassLoader parent) {
    this(parent, true);
  }

  public ServiceLocator(ClassLoader parent, boolean verify) {
    defaultClassLoader = parent;
    this.verify = verify;
  }

  private static Path getPluginPath() {
    try {
      return Directories.getServerPluginsLibDir();
    } catch (FileNotFoundException ff) {
      return null;
    }
  }

  private boolean verifyPluginIsInProperDir(String location) {
    if (verify) {
      try {
        return PLUGIN_PATH == null ? false : Path.of(new URI(location).getPath()).startsWith(PLUGIN_PATH);
      } catch (URISyntaxException notfound) {
        return false;
      }
    } else {
      return true;
    }
  }

  @Override
  protected Class<?> loadClass(String className, String location, ClassLoader loader) {
    try {
      if (verifyPluginIsInProperDir(location)) {
        ClassLoader component = locationCache.computeIfAbsent(location, loc -> createComponentClassLoader(loc, loader));
        if (component != null) {
          return Class.forName(className, true, component);
        }
      } else {
        return null;
      }
    } catch (ClassNotFoundException nf) {
      LOG.warn("unable to load " + className + " from " + location, nf);
    }
    return super.loadClass(className, location, loader);
  }

  private ClassLoader createComponentClassLoader(String location, ClassLoader parent) {
    try {
      return new ComponentURLClassLoader(new URL[]{new URL(location)}, parent, new AnnotationOrDirectoryStrategyChecker());
    } catch (MalformedURLException aml) {
      LOG.warn("unable to load " + location, aml);
    }
    return null;
  }

  private static boolean fileFilter(Path target) {
    String name = target.toString().toLowerCase();
    return name.endsWith(".jar") || name.endsWith(".zip");
  }

  private static URL toURL(Path uri) {
    try {
      return uri.toUri().toURL();
    } catch (MalformedURLException mal) {
      return null;
    }
  }

  private static URL[] createURLS(Path plugins) {
    if (Files.isDirectory(plugins)) {
      try {
        return Files.list(plugins)
                .filter(ServiceLocator::fileFilter)
                .map(ServiceLocator::toURL)
                .filter(u -> u != null)
                .toArray(i -> new URL[i]);
      } catch (IOException io) {
        throw new UncheckedIOException(io);
      }
    }
    throw new RuntimeException("plugins directory is not valid");
  }

  private static URL[] findPluginURLS() throws FileNotFoundException {
    return createURLS(Directories.getServerPluginsLibDir());
  }

  public static ServiceLocator createPlatformServiceLoader(ClassLoader serverClassLoader) {
    return new ServiceLocator(createPlatformClassLoader(serverClassLoader, STRICT));
  }

  public ClassLoader createUniversalClassLoader() {
    try {
      return new StrictURLClassLoader(findPluginURLS(), defaultClassLoader.getParent(), new UniversalCommonComponentChecker());
    } catch (FileNotFoundException file) {
      return ClassLoader.getSystemClassLoader();
    }
  }

  private static ClassLoader createPlatformClassLoader(ClassLoader serverClassLoader, boolean strict) {
    try {
//      LOG.info("Entity/Service apis will be loaded from " + Directories.getServerPluginsApiDir().getAbsolutePath());
//      LOG.info("Entity/Service implementations will be loaded from " + Directories.getServerPluginsLibDir().getAbsolutePath());
      CommonComponentChecker checker = (strict) ? new AnnotationOrDirectoryStrategyChecker() : new UniversalCommonComponentChecker();
      URLClassLoader purls = new StrictURLClassLoader(findPluginURLS(), createApiClassLoader(serverClassLoader), checker);
      return purls;
    } catch (FileNotFoundException file) {
      return ClassLoader.getSystemClassLoader();
    }
  }

  private static ApiClassLoader createApiClassLoader(ClassLoader parent) {
    try {
      return new ApiClassLoader(createURLS(Directories.getServerPluginsApiDir()), parent);
    } catch (FileNotFoundException notfound) {
      return new ApiClassLoader(new URL[0], parent);
    }
  }

  Collection<Class<?>> testingCheckUrls(String interfaceName) {
    return discoverImplementations(interfaceName, defaultClassLoader);
  }

  public <T> List<Class<? extends T>> getImplementations(Class<T> interfaceClass) {
    return super.getImplementationsTypes(interfaceClass, defaultClassLoader);
  }

  public ClassLoader getServiceLoader() {
    return defaultClassLoader;
  }
}
