/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

/**
 * This interface defines the contract between StandardDsoClientConfigHelper and a module so that the configHelper can get
 * information such as custom applicator of a class from a module.
 */
public interface ModuleSpec extends OsgiServiceSpec {

  /**
   * Get specification of all change applicators to apply
   * @return The spec
   */
  public ChangeApplicatorSpec getChangeApplicatorSpec();

  /**
   * Ask module whether this class uses a non-default constructor.
   * @param clazz The class in question
   * @return True if uses non-default constructor
   */
  public boolean isUseNonDefaultConstructor(Class clazz);

  /**
   * Get alternate peer class to use, generally if clazz is non-portable.
   * @param clazz The class to check
   * @return An alternate peer class or the original clazz
   */
  public Class getPeerClass(Class clazz);

  /**
   * Check with module whether the specified class is portable.
   * @param clazz The class
   * @return True if portable, false if unknown (should check with other modules)
   */
  public boolean isPortableClass(Class clazz);
}
