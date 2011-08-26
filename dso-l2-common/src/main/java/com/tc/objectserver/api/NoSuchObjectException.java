/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;

import java.io.Serializable;

public class NoSuchObjectException extends Exception implements Serializable {

  public NoSuchObjectException(ObjectID id) {
    super(id + " does not exist");
  }

}
