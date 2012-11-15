/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import java.util.concurrent.Callable;

/**
 *
 * @author mscott
 */
public interface CanCancel {
    boolean cancel();
}
