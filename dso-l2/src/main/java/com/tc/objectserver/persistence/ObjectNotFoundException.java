package com.tc.objectserver.persistence;

import java.io.IOException;

/**
 * @author tim
 */
public class ObjectNotFoundException extends IOException {
  public ObjectNotFoundException(String msg) {
    super(msg);
  }
}
