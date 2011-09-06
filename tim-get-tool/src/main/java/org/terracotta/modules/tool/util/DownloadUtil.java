/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.util;

import org.apache.commons.io.FileUtils;

import com.tc.util.ProductInfo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;

/**
 * Utility class for downloading files from a remote URL to the local filesystem.
 */
public class DownloadUtil {
  /**
   * Options used to control behavior of the {@link DownloadUtil#download(URL, File, DownloadOption...)} method.
   */
  public enum DownloadOption {
    /** Should download overwrite existing files? */
    OVERWRITE_EXISTING,

    /** Should non-existant intervening directories be created? */
    CREATE_INTERVENING_DIRECTORIES,

    /**
     * Is local file download location a directory? If the destination is a directory, the downloaded file will be
     * stored in the specified directory and the file name will be inferred from the remote URL. Furthermore, if the
     * destination file already exists and is a directory, then this option is implicitly inferred.
     */
    DESTINATION_IS_DIRECTORY,

    /**
     * Only download if remote file is modified since timestamp of local file. Implies
     * {@link DownloadOption#OVERWRITE_EXISTING}.
     */
    IF_MODIFIED
  }

  /**
   * Helper class used internally to interpret download options.
   */
  private static class DownloadOptionsHelper {
    private final Collection<DownloadOption> options;

    public DownloadOptionsHelper(Collection<DownloadOption> options) {
      this.options = options;
    }

    public boolean overwriteExisting() {
      return isOptionSet(DownloadOption.OVERWRITE_EXISTING) || isOptionSet(DownloadOption.IF_MODIFIED);
    }

    public boolean createInterveningDirectories() {
      return isOptionSet(DownloadOption.CREATE_INTERVENING_DIRECTORIES);
    }

    public boolean isDestinationDirectory() {
      return isOptionSet(DownloadOption.DESTINATION_IS_DIRECTORY);
    }

    public boolean ifModified() {
      return isOptionSet(DownloadOption.IF_MODIFIED);
    }

    public boolean isOptionSet(DownloadOption option) {
      return this.options.contains(option);
    }
  }

  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
  private Proxy            proxy;
  private String           proxyAuth;
  private int              bufferSize          = DEFAULT_BUFFER_SIZE;

  public DownloadUtil() {
    this(Proxy.NO_PROXY);
  }

  public DownloadUtil(Proxy proxy) {
    this.proxy = proxy;
  }

  public Proxy getProxy() {
    return proxy;
  }

  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }

  public void setProxyAuth(String auth) {
    this.proxyAuth = auth;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  /**
   * Downloads the file from the given remoteFile URL and saves it to the file referred to by localFile.
   * 
   * @param remoteFile URL referring to the remote file to download
   * @param localFile File indicating where to save the downloaded remote file
   * @param options {@link DownloadOption}s to use for download
   */
  public void download(URL remoteFile, File localFile, DownloadOption... options) throws IOException {
    download(remoteFile, localFile, Arrays.asList(options));
  }

  /**
   * Downloads the file from the given remoteFile URL and saves it to the file referred to by localFile.
   * 
   * @param remoteFile URL referring to the remote file to download
   * @param localFile File indicating where to save the downloaded remote file
   * @param options {@link DownloadOption}s to use for download
   */
  public void download(URL remoteFile, File localFile, Collection<DownloadOption> options) throws IOException {
    DownloadOptionsHelper downloadOptions = new DownloadOptionsHelper(options);
    if (!downloadOptions.overwriteExisting() && localFile.exists()) { throw new IOException(
                                                                                            "Download file already exists, and overwrite option not set."); }

    InputStream inputStream = null;
    OutputStream outputStream = null;
    try {
      if (downloadOptions.createInterveningDirectories()) {
        if (downloadOptions.isDestinationDirectory()) {
          FileUtils.forceMkdir(localFile);
        } else {
          FileUtils.forceMkdir(localFile.getParentFile());
        }
      }

      File destinationFile = null;
      if (downloadOptions.isDestinationDirectory() || localFile.isDirectory()) {
        destinationFile = new File(localFile, DownloadUtil.fileName(remoteFile));
      } else {
        destinationFile = localFile;
      }

      URLConnection connection = remoteFile.openConnection(proxy);
      connection.setRequestProperty("User-Agent", "Terracotta tim-get/" + ProductInfo.getInstance().version());
      if (proxyAuth != null) {
        Authenticator.setDefault(new ProxyAuthenticator(proxyAuth));
      }
      if (destinationFile.exists() && downloadOptions.ifModified()) {
        if (connection.getLastModified() < destinationFile.lastModified()) {
          // TODO: log the fact that download was skipped
          return;
        }
      }

      inputStream = new BufferedInputStream(connection.getInputStream());
      outputStream = new FileOutputStream(destinationFile);

      byte[] buf = new byte[bufferSize];
      int bytesReadCount = 0;
      while ((bytesReadCount = inputStream.read(buf)) > 0) {
        outputStream.write(buf, 0, bytesReadCount);
      }
    } finally {
      if (inputStream != null) inputStream.close();
      if (outputStream != null) outputStream.close();
    }
  }

  private static String fileName(URL url) {
    String path = url.getPath();
    return path.substring(path.lastIndexOf('/') + 1);
  }

  public static void main(String[] args) throws IOException {
    new DownloadUtil().download(new URL(args[0]), new File(args[1]), DownloadOption.IF_MODIFIED);
  }
}
