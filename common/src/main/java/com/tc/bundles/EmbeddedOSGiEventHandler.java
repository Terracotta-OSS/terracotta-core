/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

import org.osgi.framework.BundleException;

public interface EmbeddedOSGiEventHandler {
  void callback(Object payload) throws BundleException;
}
