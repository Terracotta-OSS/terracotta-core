/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.logging;

/**
 * Controls the set of detail information that is logged
 */
public interface RuntimeOutputOptions {

  boolean includeToString();

  boolean includeAutolockInstanceDetails();

  boolean includeFullStack();

  boolean includeCaller();

}
