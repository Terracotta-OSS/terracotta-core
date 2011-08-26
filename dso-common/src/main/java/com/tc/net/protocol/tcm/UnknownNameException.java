/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.exception.TCException;

/**
 * @author orion
 */
public class UnknownNameException extends TCException {
  public UnknownNameException(Class msgClass, byte name) {
    super("unknown name: " + name + " for message class " + msgClass.getName());
  }
}