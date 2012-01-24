/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.utils;

/**
 * Thrown when two {@link XmlObject}s are not equal in
 * {@link com.tc.config.schema.utils.StandardXmlObjectComparator#checkEquals(XmlObject, XmlObject)}.
 */
public class NotEqualException extends Exception {

  public NotEqualException() {
    super();
  }

  public NotEqualException(String message, Throwable cause) {
    super(message, cause);
  }

  public NotEqualException(String message) {
    super(message);
  }

  public NotEqualException(Throwable cause) {
    super(cause);
  }

}
