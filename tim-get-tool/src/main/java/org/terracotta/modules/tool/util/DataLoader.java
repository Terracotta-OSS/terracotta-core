/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.util;

import org.terracotta.modules.tool.config.Config;
import org.terracotta.modules.tool.config.ConfigAnnotation;
import org.terracotta.modules.tool.util.DownloadUtil.DownloadOption;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

/**
 * Utility class used for loading a data file from a remote and/or local location. The DataLoader will cache the local
 * data file according to its cache policy. This class is not thread safe, nor does it perform any file locking.
 */
public class DataLoader {

  /**
   * Enum defining the various cache refresh policies available for the {@link DataLoader}. The ON_EXPIRATION policy
   * uses the expirationInSeconds property as the basis of expiration. The default value is 24 hours. To change this
   * value, set the dataCacheExpirationInSeconds property value either via the System properties or the
   * tim-get.properties file.
   */
  public enum CacheRefreshPolicy {

    NEVER, ALWAYS, ON_REMOTE_DATA_MODIFIED, ON_EXPIRATION;

    long expirationInSeconds = 60 * 60 * 24;

    public long getExpirationInSeconds() {
      return expirationInSeconds;
    }

    public CacheRefreshPolicy setExpirationInSeconds(long expirationInSeconds) {
      this.expirationInSeconds = expirationInSeconds;
      return this;
    }

  }

  private final URL          remoteDataUrl;
  private final File         localDataFile;
  private final boolean      isGzipped;
  private CacheRefreshPolicy cacheRefreshPolicy = CacheRefreshPolicy.ON_EXPIRATION;

  @Inject
  @Named(ConfigAnnotation.DOWNLOADUTIL_INSTANCE)
  private final DownloadUtil downloader;

  public DataLoader(URL remoteDataUrl, File localDataFile) {
    this.localDataFile = localDataFile;
    this.downloader = new DownloadUtil();
    this.remoteDataUrl = remoteDataUrl;
    this.isGzipped = remoteDataUrl.toString().endsWith(".gz");
  }

  @Inject
  public DataLoader(@Named(ConfigAnnotation.CONFIG_INSTANCE) Config config) {
    this(config.getDataFileUrl(), config.getIndexFile());
    setCacheRefreshPolicy(CacheRefreshPolicy.ON_EXPIRATION.setExpirationInSeconds(config
        .getDataCacheExpirationInSeconds()));
  }

  /**
   * Sets the Proxy to use when retrieving the remote data file.
   */
  public void setProxy(Proxy proxy) {
    downloader.setProxy(proxy);
  }

  public CacheRefreshPolicy getCacheRefreshPolicy() {
    return cacheRefreshPolicy;
  }

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
  private File getDataFile() throws IOException {
    try {
      if (!isLocalDataFresh()) {
        loadDataFile();
      }
    } catch (IOException e) {
      if (this.localDataFile.exists()) {
        // TODO: use logging instead of or in addition to System.err
        System.err.println("WARNING: Failed to download remote data file.  Using cached copy at '"
                           + localDataFile.getAbsolutePath() + "'.");
        System.err.println("Error message: " + e.getMessage());
        System.err.flush();
      } else {
        throw e;
      }
    }
    return localDataFile;
  }

  /**
   * Is the local data file up to date with respect to the caching policy?
   * 
   * @throws IOException
   */
  private boolean isLocalDataFresh() throws IOException {
    if (!localDataFile.exists()) return false;

    if (remoteDataUrl == null) return true;

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

  private boolean isRemoteDataModified() throws IOException {
    URLConnection connection = remoteDataUrl.openConnection(downloader.getProxy());
    return connection.getLastModified() > localDataFile.lastModified();
  }

  private boolean isLocalDataFileExpired() {
    return false && localDataFileAge() > cacheRefreshPolicy.getExpirationInSeconds();
  }

  /**
   * Returns the age of the local data file in seconds. If the file does not exist, returns Long.MAX_VALUE.
   */
  private long localDataFileAge() {
    if (!localDataFile.exists()) return Long.MAX_VALUE;
    double age = System.currentTimeMillis() - localDataFile.lastModified();
    return Math.round(age / 1000.0);
  }

  private void loadDataFile() throws IOException {
    if (this.remoteDataUrl == null) return;
    downloader.download(this.remoteDataUrl, localDataFile, DownloadOption.CREATE_INTERVENING_DIRECTORIES,
                        DownloadOption.OVERWRITE_EXISTING);
  }

  public InputStream openDataStream() throws FileNotFoundException, IOException {
    if (isGzipped) {
      return new GZIPInputStream(new FileInputStream(getDataFile()));
    } else {
      return new FileInputStream(getDataFile());
    }
  }

  public URL getRemoteDataUrl() {
    return remoteDataUrl;
  }

  public File getLocalDataFile() {
    return localDataFile;
  }

}
