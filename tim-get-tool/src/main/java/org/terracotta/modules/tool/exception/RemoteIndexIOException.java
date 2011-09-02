/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool.exception;

import java.io.File;
import java.net.URL;

/**
 * This exception indicates that the remote repository index file could not be accessed, most likely because it is not
 * available.
 */
public class RemoteIndexIOException extends RuntimeException {

  private final File localDataFile;
  private final URL  remoteDataUrl;

  public RemoteIndexIOException() {
    super();
    this.localDataFile = null;
    this.remoteDataUrl = null;
  }

  public RemoteIndexIOException(String message, Throwable cause, File localDataFile, URL remoteDataUrl) {
    super(message, cause);
    this.localDataFile = localDataFile;
    this.remoteDataUrl = remoteDataUrl;
  }

  public RemoteIndexIOException(String message, Throwable cause) {
    super(message, cause);
    this.localDataFile = null;
    this.remoteDataUrl = null;
  }

  public RemoteIndexIOException(String message) {
    super(message);
    this.localDataFile = null;
    this.remoteDataUrl = null;
  }

  public RemoteIndexIOException(Throwable cause) {
    super(cause);
    this.localDataFile = null;
    this.remoteDataUrl = null;
  }

  public File getLocalDataFile() {
    return localDataFile;
  }

  public URL getRemoteDataUrl() {
    return remoteDataUrl;
  }

}
