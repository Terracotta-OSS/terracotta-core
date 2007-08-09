/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

/**
 * This interface defines the contract between StandardDsoClientConfigHelper and a module so that the configHelper can get
 * information such as custom applicator of a class from a module.
 *
 */
public interface ModuleSpec {
  public final static Integer LOW_RANK    = new Integer(0);
  public final static Integer NORMAL_RANK = new Integer(1); // default ranking
  public final static Integer HIGN_RANK   = new Integer(2);
  
  public ChangeApplicatorSpec getChangeApplicatorSpec();

  public boolean isUseNonDefaultConstructor(Class clazz);

  public Class getPeerClass(Class clazz);

  public boolean isPortableClass(Class clazz);
}
