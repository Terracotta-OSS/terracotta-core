/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool.exception;

/**
 * This exception indicates that the remote repository index file could not be accessed, most
 * likely because it is not available.
 */
public class RemoteIndexIOException extends RuntimeException {

  public RemoteIndexIOException() {
    super();
  }

  public RemoteIndexIOException(String message, Throwable cause) {
    super(message, cause);
  }

  public RemoteIndexIOException(String message) {
    super(message);
  }

  public RemoteIndexIOException(Throwable cause) {
    super(cause);
  }


}
