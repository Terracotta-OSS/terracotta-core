/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.exception;


/**
 * Thrown when someone tries to lookup an object with an ObjectID and the ObjectID does not exist.
 */
public class TCObjectNotFoundException extends TCRuntimeException {
  public final static String            CLASS_SLASH = "com/tc/exception/TCObjectNotFoundException";

  private static final ExceptionWrapper wrapper     = new ExceptionWrapperImpl();

  public TCObjectNotFoundException(String missingObjectID) {
    super(wrapper.wrap("Requested Object is missing : " + missingObjectID));
  }
}