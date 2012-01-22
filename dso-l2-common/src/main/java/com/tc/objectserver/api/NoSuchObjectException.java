/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;

import java.io.Serializable;

public class NoSuchObjectException extends Exception implements Serializable {

  public NoSuchObjectException(ObjectID id) {
    super(id + " does not exist");
  }

}
