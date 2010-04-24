/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

/**
 * This interface defines the contract between StandardDsoClientConfigHelper and a module so that the configHelper can get
 * information from a module.
 *
 * The concept of "rank" comes from the OSGi "service rank", which lets you define how
 * likely a service is to be returned as the default service from a bundle.
 * @see org.osgi.framework.BundleContext#getServiceReference
 * @see org.osgi.framework.Constants#SERVICE_RANKING
 */
public interface OsgiServiceSpec {
  /** OSGi service ranking defining which service is returned - LOW=0 */
  public final static Integer LOW_RANK    = new Integer(0);
  /** OSGi service ranking defining which service is returned - NORMAL=1 */
  public final static Integer NORMAL_RANK = new Integer(1); // default ranking
  /** OSGi service ranking defining which service is returned - HIGH=2 */
  public final static Integer HIGH_RANK   = new Integer(2);

}