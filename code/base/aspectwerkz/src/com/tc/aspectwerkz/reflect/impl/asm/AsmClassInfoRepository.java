/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.aspectwerkz.reflect.impl.asm;

import com.google.common.collect.MapMaker;
import com.tc.aspectwerkz.exception.DefinitionException;
import com.tc.aspectwerkz.reflect.ClassInfo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A repository for the class info hierarchy. Is class loader aware.
 * 
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonér </a>
 */
public class AsmClassInfoRepository {
  /**
   * Map with all the class info repositories mapped to their class loader.
   */
  private static final Map<ClassLoader, Reference<AsmClassInfoRepository>> s_repositories          = new MapMaker()
                                                                                                       .weakKeys()
                                                                                                       .makeMap();

  private static final AsmClassInfoRepository                              NULL_LOADER_REPOSITORYU = new AsmClassInfoRepository(
                                                                                                                                null);

  /**
   * Map with all the class info mapped to their class names.
   */
  private final Map<String, Reference<ClassInfo>>                          m_repository            = new ConcurrentHashMap();

  /**
   * Class loader for the class repository.
   */
  private transient final WeakReference                                    m_loaderRef;

  /**
   * The annotation properties file.
   */
  private final Properties                                                 m_annotationProperties;

  /**
   * Creates a new repository.
   * 
   * @param loader
   */
  private AsmClassInfoRepository(final ClassLoader loader) {
    m_loaderRef = new WeakReference(loader);
    m_annotationProperties = new Properties();
    if (loader != null) {
      try {
        InputStream stream = loader.getResourceAsStream("annotation.properties");
        if (stream != null) {
          try {
            m_annotationProperties.load(stream);
          } finally {
            try {
              stream.close();
            } catch (Exception e) {
              //
            }
          }
        }
      } catch (IOException e) {
        throw new DefinitionException("could not find resource [annotation.properties] on classpath");
      }
    }
  }

  /**
   * Returns the class info repository for the specific class loader
   * 
   * @param loader
   * @return
   */
  public static AsmClassInfoRepository getRepository(final ClassLoader loader) {
    if (loader == null) return NULL_LOADER_REPOSITORYU;

    AsmClassInfoRepository repository = lookup(loader);

    // normal return case for existing repositories
    if (repository != null) { return repository; }


    // Construct the repo outside of the lock (see CDV-116)
    AsmClassInfoRepository repo = new AsmClassInfoRepository(loader);

    synchronized (s_repositories) {
      // check again
      repository = lookup(loader);

      // another thread won, don't replace the mapping
      if (repository != null) { return repository; }

      s_repositories.put(loader, new SoftReference(repo));
    }

    return repo;
  }

  private static AsmClassInfoRepository lookup(ClassLoader loader) {
    Reference<AsmClassInfoRepository> ref = s_repositories.get(loader);
    return ref == null ? null : ref.get();
  }

  /**
   * Remove a class from the repository.
   * 
   * @param className the name of the class
   */
  public static void removeClassInfoFromAllClassLoaders(final String className) {
    // TODO - fix algorithm
    throw new UnsupportedOperationException("fix algorithm");
  }

  /**
   * Returns the class info.
   * 
   * @param className
   * @return
   */
  public ClassInfo getClassInfo(final String className) {
    Reference classInfoRef = m_repository.get(className);
    ClassInfo info = classInfoRef == null ? null : (ClassInfo) classInfoRef.get();
    if (info == null) { return checkParentClassRepository(className, (ClassLoader) m_loaderRef.get()); }
    return info;
  }

  /**
   * Adds a new class info.
   * 
   * @param classInfo
   */
  public void addClassInfo(final ClassInfo classInfo) {
    // is the class loaded by a class loader higher up in the hierarchy?
    if (checkParentClassRepository(classInfo.getName(), (ClassLoader) m_loaderRef.get()) == null) {
      m_repository.put(classInfo.getName(), new SoftReference(classInfo));
    } else {
      // TODO: remove class in child class repository and add it for the
      // current (parent) CL
    }
  }

  /**
   * Checks if the class info for a specific class exists.
   * 
   * @param name
   * @return
   */
  public boolean hasClassInfo(final String name) {
    Reference classInfoRef = m_repository.get(name);
    return (classInfoRef == null) ? false : (classInfoRef.get() != null);
  }

  /**
   * Removes the class from the repository (since it has been modified and needs to be rebuild).
   * 
   * @param className
   */
  public void removeClassInfo(final String className) {
    m_repository.remove(className);
  }

  /**
   * Returns the annotation properties for the specific class loader.
   * 
   * @return the annotation properties
   */
  public Properties getAnnotationProperties() {
    return m_annotationProperties;
  }

  /**
   * Searches for a class info up in the class loader hierarchy.
   * 
   * @param className
   * @param loader
   * @return the class info
   * @TODO might clash for specific class loader lookup algorithms, user need to override this class and implement this
   *       method
   */
  public ClassInfo checkParentClassRepository(final String className, final ClassLoader loader) {
    if (loader == null) { return null; }
    ClassLoader parent = loader.getParent();
    if (parent == null) {
      return null;
    } else {
      AsmClassInfoRepository parentRep = AsmClassInfoRepository.getRepository(parent);

      Reference classInfoRef = parentRep.m_repository.get(className);
      ClassInfo info = classInfoRef == null ? null : (ClassInfo) classInfoRef.get();
      if (info != null) {
        return info;
      } else {
        return checkParentClassRepository(className, parent);
      }
    }
  }

  /*
   * public ClassInfo checkParentClassRepository(final String className, final ClassLoader loader) { if (loader == null)
   * {
   * return null; } ClassLoader parent = loader.getParent(); if (parent == null) { return null; } else { ClassInfo info
   * =
   * AsmClassInfoRepository.getRepository(parent).getClassInfo(className); if (info != null) { return info; } else {
   * return checkParentClassRepository(className, parent); } } }
   */

}
