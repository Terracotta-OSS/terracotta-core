/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.reflect;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A repository for the class info hierarchy. Is class loader aware.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class ClassInfoRepository {
  /**
   * Map with all the class info repositories mapped to their class loader.
   */
  private static final HashMap s_repositories = new HashMap();

  /**
   * Map with all the class info mapped to their class names.
   */
  private final Map m_repository = new WeakHashMap();

  /**
   * Class loader for the class repository.
   */
  private transient final WeakReference m_loaderRef;

  /**
   * Creates a new repository.
   *
   * @param loader
   */
  private ClassInfoRepository(final ClassLoader loader) {
    m_loaderRef = new WeakReference(loader);
  }

  public static synchronized void clear() {
    s_repositories.clear();
  }

  /**
   * Returns the class info repository for the specific class loader
   *
   * @param loader
   * @return
   */
  public static synchronized ClassInfoRepository getRepository(final ClassLoader loader) {
    Integer hash = Integer.valueOf(loader == null ? 0 : loader.hashCode());  // boot cl
    WeakReference repositoryRef = (WeakReference) s_repositories.get(hash);
    ClassInfoRepository repository = repositoryRef == null ? null : (ClassInfoRepository) repositoryRef.get();
    if (repository != null) {
      return repository;
    } else {
      ClassInfoRepository repo = new ClassInfoRepository(loader);
      s_repositories.put(hash, new WeakReference(repo));
      return repo;
    }
  }

  /**
   * Remove a class from the repository.
   *
   * @param className the name of the class
   */
  public static void removeClassInfoFromAllClassLoaders(final String className) {
    //TODO - fix algorithm
    throw new UnsupportedOperationException("fix algorithm");
  }

  /**
   * Returns the class info.
   *
   * @param className
   * @return
   */
  public ClassInfo getClassInfo(final String className) {
    ClassInfo info = (ClassInfo) m_repository.get(className);
    if (info == null) {
      return checkParentClassRepository(className, (ClassLoader) m_loaderRef.get());
    }
    return (ClassInfo) m_repository.get(className);
  }

  /**
   * Adds a new class info.
   *
   * @param classInfo
   */
  public void addClassInfo(final ClassInfo classInfo) {
    // is the class loaded by a class loader higher up in the hierarchy?
    if (checkParentClassRepository(classInfo.getName(), (ClassLoader) m_loaderRef.get()) == null) {
      // need to create new String instance to avoid using the original reference
      m_repository.put(new String(classInfo.getName()), classInfo);
    } else {
      // TODO: remove class in child class repository and add it for the current (parent) CL
    }
  }

  /**
   * Checks if the class info for a specific class exists.
   *
   * @param name
   * @return
   */
  public boolean hasClassInfo(final String name) {
    return m_repository.containsKey(name);
  }

  /**
   * Searches for a class info up in the class loader hierarchy.
   *
   * @param className
   * @param loader
   * @return the class info
   * @TODO might clash for specific class loader lookup algorithms, user need to override this class and implement
   * this method
   */
  public ClassInfo checkParentClassRepository(final String className, final ClassLoader loader) {
    if (loader == null) {
      return null;
    }
    ClassInfo info;
    ClassLoader parent = loader.getParent();
    if (parent == null) {
      return null;
    } else {
      info = ClassInfoRepository.getRepository(parent).getClassInfo(className);
      if (info != null) {
        return info;
      } else {
        return checkParentClassRepository(className, parent);
      }
    }
  }
}
