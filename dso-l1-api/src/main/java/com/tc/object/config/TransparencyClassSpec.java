/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

/**
 * Configure and describe the custom adaption of a class
 */
public interface TransparencyClassSpec {

  /**
   * Get the class name for this spec
   * 
   * @return Name
   */
  public String getClassName();

  /**
   * @return Change applicator specification
   */
  public ChangeApplicatorSpec getChangeApplicatorSpec();

  /**
   * @return True if should use non-default constrcutor
   */
  public boolean isUseNonDefaultConstructor();

  /**
   * Set to use non default constructor
   * 
   * @param useNonDefaultConstructor True to use non-default
   */
  public void setUseNonDefaultConstructor(boolean useNonDefaultConstructor);

  /**
   * @return Get name of change applicator class
   */
  public String getChangeApplicatorClassName();

}
