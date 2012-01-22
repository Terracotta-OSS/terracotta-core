/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.reflect.impl.java;

import com.google.common.collect.MapMaker;
import com.tc.aspectwerkz.reflect.ClassInfo;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * A repository for the class info hierarchy. Is class loader aware.
 * <p/>
 * TODO refactor some with ASMClassInfoRepository but keep em separate for system runtime sake in AOPC (WLS)
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public class JavaClassInfoRepository {
  /**
   * Map with all the class info repositories mapped to their class loader.
   */
  private static final Map<ClassLoader, JavaClassInfoRepository> s_repositories         = new MapMaker().weakKeys().makeMap();

  private static final JavaClassInfoRepository                   NULL_LOADER_REPOSITORY = new JavaClassInfoRepository(
                                                                                                                      null);

  /**
   * Map with all the class info mapped to their class names.
   */
  private final Map<String, ClassInfo>                           m_repository           = new MapMaker().softValues().makeMap();

  /**
   * Class loader for the class repository.
   */
  private transient final WeakReference                          m_loaderRef;

  /**
   * Creates a new repository.
   *
   * @param loader
   */
  private JavaClassInfoRepository(final ClassLoader loader) {
    m_loaderRef = new WeakReference(loader);
  }

  public static void clear() {
    synchronized (JavaClassInfoRepository.class) {
      s_repositories.clear();
    }

    NULL_LOADER_REPOSITORY.m_loaderRef.clear();
    NULL_LOADER_REPOSITORY.m_repository.clear();
  }

  /**
   * Returns the class info repository for the specific class loader
   *
   * @param loader
   * @return
   */
  public static JavaClassInfoRepository getRepository(ClassLoader loader) {
    if (loader == null) { return NULL_LOADER_REPOSITORY; }

    JavaClassInfoRepository repository = s_repositories.get(loader);
    if (repository != null) { return repository; }

    synchronized (JavaClassInfoRepository.class) {
      // check again now that we're locked
      repository = s_repositories.get(loader);
      if (repository != null) { return repository; }

      JavaClassInfoRepository repo = new JavaClassInfoRepository(loader);
      s_repositories.put(loader, repo);
      return repo;
    }
  }


  static int repositoriesSize() {
    return s_repositories.size();
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
    ClassInfo info = m_repository.get(className);
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
      m_repository.put(classInfo.getName(), classInfo);
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
    return m_repository.get(name) != null;
  }

  /**
   * Searches for a class info up in the class loader hierarchy.
   *
   * @param className
   * @param loader
   * @return the class info
   * @TODO might clash for specific class loader lookup algorithms, user need to override this class and implement
   *       this method
   */
  public ClassInfo checkParentClassRepository(final String className, final ClassLoader loader) {
    if (loader == null) { return null; }
    ClassInfo info;
    ClassLoader parent = loader.getParent();
    if (parent == null) {
      return null;
    } else {
      info = JavaClassInfoRepository.getRepository(parent).getClassInfo(className);
      if (info != null) {
        return info;
      } else {
        return checkParentClassRepository(className, parent);
      }
    }
  }
}
