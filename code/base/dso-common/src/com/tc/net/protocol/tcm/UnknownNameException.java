/*
 * Created on Apr 6, 2004
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