/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode.hook;

/**
 * Allows implementors to either pre- or post-process classes as they are loaded.
 */
public interface ClassProcessor extends ClassPreProcessor, ClassPostProcessor {
  // nothing extra to add here (yet)
}
