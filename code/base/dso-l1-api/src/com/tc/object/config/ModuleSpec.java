/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

/**
 * This interface defines the contract between StandardDsoClientConfigHelper and a module so that the configHelper can get
 * information such as custom applicator of a class from a module.
 * 
 * The concept of "rank" comes from the OSGi "service rank", which lets you define how 
 * likely a service is to be returned as the default service from a bundle.  
 * @see org.osgi.framework.BundleContext#getServiceReference
 * @see org.osgi.framework.Constants#SERVICE_RANKING
 */
public interface ModuleSpec {
  
  /** OSGi service ranking defining which service is returned - LOW=0 */
  public final static Integer LOW_RANK    = new Integer(0);
  /** OSGi service ranking defining which service is returned - NORMAL=1 */
  public final static Integer NORMAL_RANK = new Integer(1); // default ranking
  /** OSGi service ranking defining which service is returned - HIGH=2 */
  public final static Integer HIGH_RANK   = new Integer(2);
  
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
