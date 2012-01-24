/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook;

import com.tc.object.bytecode.Manager;
import com.tc.object.config.ModuleConfiguration;

import java.lang.instrument.ClassFileTransformer;

/**
 * The idea behind DSOContext is to encapsulate a DSO "world" in a client VM. But this idea has not been fully realized.
 */
public interface DSOContext extends ClassProcessor, ClassFileTransformer {

  public static final String CLASS = "com/tc/object/bytecode/hook/DSOContext";
  public static final String TYPE  = "L" + CLASS + ";";

  /**
   * @return The Manager instance
   */
  public Manager getManager();

  public void shutdown();

  public ModuleConfiguration getModuleConfigurtion();

}
