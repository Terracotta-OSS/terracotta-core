/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.rejoin;

import org.terracotta.toolkit.object.ToolkitObject;

import com.tc.platform.rejoin.RejoinLifecycleListener;

/**
 * Interface similar to {@link RejoinLifecycleListener}, but at toolkit layer.
 */
public interface RejoinAwareToolkitObject extends ToolkitObject, RejoinCallback {
  //
}
