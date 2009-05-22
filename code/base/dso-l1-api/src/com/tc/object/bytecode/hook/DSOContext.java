/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook;

import com.tc.object.bytecode.Manager;

import java.net.URL;

/**
 * The idea behind DSOContext is to encapsulate a DSO "world" in a client VM. But this idea has not been fully realized.
 */
public interface DSOContext extends ClassProcessor {

  public static final String CLASS = "com/tc/object/bytecode/hook/DSOContext";
  public static final String TYPE  = "L" + CLASS + ";";

  /**
   * @return The Manager instance
   */
  public Manager getManager();

  /**
   * Get type of locks used by sessions
   * 
   * @param appName Web app anem
   * @return Lock type
   */
  public int getSessionLockType(String appName);

  /**
   * Get url to class file
   * 
   * @param className Class name
   * @param loader the calling classloader
   * @param hideSystemResources true if resources destined only for the system class loader should be hidden
   * @return URL to class itself
   */
  public URL getClassResource(String className, ClassLoader loader, boolean hideSystemResources);

  /**
   * Returns true is web-application is configured for session-locking
   */
  public boolean isApplicationSessionLocked(String appName);

}
