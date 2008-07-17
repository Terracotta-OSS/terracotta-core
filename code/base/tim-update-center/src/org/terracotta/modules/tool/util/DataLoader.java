/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool.util;


import org.terracotta.modules.tool.util.DownloadUtil.DownloadOption;

import com.google.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

/**
 * Utility class used for loading a data file from a remote and/or local location.
 *
 * The DataLoader will cache the local data file according to its cache policy.
 *
 * This class is not thread safe, nor does it perform any file locking.
 */
public class DataLoader {

  /**
   * Enum defining the various cache refresh policies available for the
   * {@link DataLoader}.
   *
   * The ON_EXPIRATION policy uses the expirationInSeconds property as the basis
   * of expiration.  The default value is 24 hours.  To change this value, call
   * the {@link #setExpirationInSeconds(long)} method on the ON_EXPIRATION
   * enum value to an appropriate number of seconds.
   */
  public enum CacheRefreshPolicy {
    NEVER, ALWAYS, ON_REMOTE_DATA_MODIFIED, ON_EXPIRATION;
    long expirationInSeconds = 60 * 60 * 24; // 24 hours

    public long getExpirationInSeconds() {
      return expirationInSeconds;
    }

    public CacheRefreshPolicy setExpirationInSeconds(long expirationInSeconds) {
      this.expirationInSeconds = expirationInSeconds;
      return this;
    }

  }

  private URL  remoteDataUrl;
  private File localDataFile;
  private CacheRefreshPolicy cacheRefreshPolicy = CacheRefreshPolicy.ON_EXPIRATION;

  private Proxy proxy = Proxy.NO_PROXY;

  public DataLoader(File localDataFile) {
    this.localDataFile = localDataFile;
  }

  public DataLoader(URL remoteDataUrl, File localDataFile) {
    this(localDataFile);
    this.remoteDataUrl = remoteDataUrl;
  }

  /**
   * Sets the Proxy to use when retrieving the remote data file.
   */
  @Inject public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }

  public CacheRefreshPolicy getCacheRefreshPolicy() {
    return cacheRefreshPolicy;
  }

  @Inject
  public void setCacheRefreshPolicy(CacheRefreshPolicy cacheRefreshPolicy) {
    this.cacheRefreshPolicy = cacheRefreshPolicy;
  }

  /**
   * Returns a <code>File<code> object referring to the local data file, loading
   * from the remote data URL if {@link #isLocalDataFresh()} returns false.
   *
   * If this method returns normally, the returned File object is guaranteed
   * to refer to a file that {@link File#exists()}.
   */
  public File getDataFile() throws IOException {
    try {
      if (!isLocalDataFresh()) {
        loadDataFile();
      }
    } catch (IOException e) {
      if (this.localDataFile.exists()) {
        // TODO: use logging instead of or in addition to System.err
        System.err.println("WARNING: Failed to download remote data file.  Using cached copy.");
      }
      else {
        throw e;
      }
    }
    return localDataFile;
  }

  /**
   * Is the local data file up to date with respect to the caching policy?
   * @throws IOException 
   */
  public boolean isLocalDataFresh() throws IOException {
    if (!localDataFile.exists())
      return false;

    if (remoteDataUrl == null)
      return true;

    switch (this.cacheRefreshPolicy) {
      case NEVER:
        return true;
      case ALWAYS:
        return false;
      case ON_REMOTE_DATA_MODIFIED:
        return isRemoteDataModified();
      case ON_EXPIRATION:
        return isLocalDataFileExpired();
    }
    return false;
  }

  public boolean isRemoteDataModified() throws IOException {
    URLConnection connection = remoteDataUrl.openConnection(proxy);
    return connection.getLastModified() > localDataFile.lastModified();
  }

  public boolean isLocalDataFileExpired() {
    return localDataFileAge() > cacheRefreshPolicy.getExpirationInSeconds();
  }

  /**
   * Returns the age of the local data file in seconds.  If the file does not
   * exist, returns Long.MAX_VALUE.
   */
  public long localDataFileAge() {
    if (!localDataFile.exists())
      return Long.MAX_VALUE;
    double age = System.currentTimeMillis() - localDataFile.lastModified();
    return Math.round(age / 1000.0);
  }

  public void loadDataFile() throws IOException {
    DownloadUtil downloader = new DownloadUtil(this.proxy);
    downloader.download(this.remoteDataUrl, localDataFile,
                        DownloadOption.CREATE_INTERVENING_DIRECTORIES,
                        DownloadOption.OVERWRITE_EXISTING);
  }
}
