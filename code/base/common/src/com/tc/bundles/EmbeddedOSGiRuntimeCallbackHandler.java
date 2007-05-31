/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bundles;

import org.osgi.framework.BundleException;

public interface EmbeddedOSGiRuntimeCallbackHandler {
  void callback(Object payload)  throws BundleException;
}
