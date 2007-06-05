/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.zerog.ia.customcode.util.fileutils;

import java.io.*;

import com.zerog.ia.api.pub.*;

/**
 * DeleteDirectory deletes the specified directory. If the directory is not empty, its contents are deleted recursively
 * before it is removed.
 * 
 * @see com.acme.dialogs.CustomCodeAction
 * @version 3.0.0
 */
public class DeleteDirectory extends CustomCodeAction {
  private static final String INSTALL_MESSAGE   = "Deleting directories";
  private static final String UNINSTALL_MESSAGE = "";
  private static final String ERR_MSG           = "DeleteDirectory: no path specified.";
  private static final String SOURCE_VAR_NAME   = "$DeleteDirectory_Path$";
  private boolean             isLoaded          = false;
  private static final String SUCCESS           = "SUCCESS";
  private static final String ERROR             = "ERROR";

  /**
   * This is the method that is called at install-time. The InstallerProxy instance provides methods to access
   * information in the installer, set status, and control flow.
   * <p>
   * For the purposes of the this action (DeleteDirectory), this method
   * <ol>
   * <li>gets its parameters from InstallAnywhere Variables,</li>
   * <li>checks the parameters' validity,</li>
   * <li>and recursively deletes the directories.</li>
   * </ol>
   * 
   * @see com.zerog.ia.api.pub.CustomCodeAction#install
   */
  public void install(InstallerProxy ip) throws InstallException {
    if (isLoaded == true) return;
    isLoaded = true;

    /*
     * Get input from InstallAnywhere Variables. The literal contents of the Variables are retieved into the Strings.
     */
    String path = ip.substitute(SOURCE_VAR_NAME);

    /*
     * substitute() will return an empty string for any InstallAnywhere Variable that hasn't been assigned yet.
     */

    if (path.equals("")) {
      error(path);
    } else {
      try {
        deleteDirectory(path);
        ip.setVariable("DELETE_DIRECTORY_SUCCESS", DeleteDirectory.SUCCESS);
        System.out.println("DeleteDirectory: succeeded");
      } catch (IOException ioe) {
        System.out.println("DeleteDirectory: Exception = " + ioe.getMessage());
        ip.setVariable("DELETE_DIRECTORY_SUCCESS", DeleteDirectory.ERROR);
        System.out.println("DeleteDirectory: DELETE_DIRECTORY_SUCCESS=" + DeleteDirectory.ERROR);

        throw new NonfatalInstallException(ioe.getMessage());
      }
    }
  }

  /**
   * This is the method that is called at uninstall-time. For an example of how to effect the uninstallation of
   * something like this, please see com.acme.fileutils.CopyFile.
   * 
   * @see com.acme.fileutils.CopyFile
   * @see com.zerog.ia.api.pub.CustomCodeAction#uninstall
   */
  public void uninstall(UninstallerProxy up) throws InstallException {
    //
  }

  /**
   * This method will be called to display a status message during the installation.
   * 
   * @see com.zerog.ia.api.pub.CustomCodeAction#getInstallStatusMessage
   */
  public String getInstallStatusMessage() {
    return INSTALL_MESSAGE;
  }

  /**
   * This method will be called to display a status message during the uninstall.
   * 
   * @see com.zerog.ia.api.pub.CustomCodeAction#getUninstallStatusMessage
   */
  public String getUninstallStatusMessage() {
    return UNINSTALL_MESSAGE;
  }

  /**
   * Delete the specified directory represented by directoryToDelete. If the directory is not empty, its contents are
   * deleted recursively before it is removed.
   */
  public static void deleteDirectory(String directoryToDelete) throws IOException {
    deleteDirectory(new File(directoryToDelete));
  }

  /**
   * Delete the specified directory represented by directoryToDelete. If the directory is not empty, its contents are
   * deleted recursively before it is removed.
   */
  public static boolean deleteDirectory(File directoryToDelete) throws IOException {
    // make sure it's a directory
    if (directoryToDelete.isDirectory()) {

      String fileSep = System.getProperty("file.separator");

      String[] filesAndDirs = directoryToDelete.list();
      int numberFiles = filesAndDirs.length;

      for (int i = 0; i < numberFiles; i++) {

        File currentFile = new File(directoryToDelete.getPath() + fileSep + filesAndDirs[i]);

        // If it's a dir, empty it first
        if (currentFile.isDirectory()) {

          if (!deleteDirectory(currentFile)) return false;
          continue;
        }

        if (!currentFile.delete()) {
          System.err.println("DELETING");
          throw new IOException("Can't delete file or directory: " + currentFile.getAbsolutePath());
        }

      }

      return directoryToDelete.delete();
    } else {
      throw new IOException("Couldn't delete directory because directory is a file: "
                            + directoryToDelete.getAbsolutePath());
    }
  }

  /**
   * Print something to indicate that the parameters were not acceptable.
   */
  private void error(String path) {
    System.err.println(ERR_MSG);
    System.err.println("Path: " + path);
  }
}
