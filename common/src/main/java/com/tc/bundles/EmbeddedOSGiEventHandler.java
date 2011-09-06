/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bundles;

import org.osgi.framework.BundleException;

public interface EmbeddedOSGiEventHandler {
  void callback(Object payload) throws BundleException;
}
