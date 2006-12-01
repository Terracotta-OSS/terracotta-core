/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tc.installer.util;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.zerog.ia.api.pub.*;

/**
 * This class creates a time stamp with the current time and date. It is created to be used as part of the upgrade
 * process. The upgrade process renames the parent folder of an existing Terracotta installation with the original name
 * and a date time stamp appended to the original name.
 * <p>
 * Format of renamed folder: "{OLD_NAME}_M-D-YYYY_HH:MM" <br>
 * e.g. <br>
 * C:\Program Files\Terracotta <br>
 * becomes <br>
 * C:\Program Files\Terracotta_06-23-2006_14:57
 * <p>
 * Please take a look at: <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/text/SimpleDateFormat.html">
 * SimpleDateFormat.java </a> to get the possible Date Format structure.
 */
public class DateStamp extends CustomCodeAction {
  private static final String INSTALL_MESSAGE   = "Creating Date Stamp";
  private static final String UNINSTALL_MESSAGE = "";

  /**
   * This is the method that is called at install-time. The InstallerProxy instance provides methods to access
   * information in the installer, set status, and control flow.
   * <p>
   * For the purposes of the this action (DateStamp), this method
   * <ol>
   * <li>gets its parameters from the InstallAnywhere Variables $DATE_FORMAT$</li>
   * <li>sets the InstallAnywhere variable $FORAMATTED_DATE$ with the current date whose <br>
   * date format is specified by $DATE_FORMAT$</li>
   * </ol>
   * 
   * @see com.zerog.ia.api.pub.CustomCodeAction#install
   */
  public void install(InstallerProxy ip) throws InstallException {

    try {
      String DateFormat = ip.substitute("$DATE_FORMAT$");
      String formattedDate = createFormattedDate(DateFormat);
      ip.setVariable("FORMATTED_DATE", formattedDate);
    } catch (Exception e) {
      throw new NonfatalInstallException(e.getMessage());
    }
  }

  /**
   * This is the method that is called at uninstall-time. The UninstallerProxy instance provides methods to access
   * information in the installer, set status, and control flow.
   * <p>
   * For the purposes of the this action (DateStamp), this method is not used.
   */
  public void uninstall(UninstallerProxy up) throws InstallException {
  }

  
  /**
   * Returns the current date/time in the date format specified by dateFormat
   * @param dateFormat The date format we want for the current date/time
   */
  private String createFormattedDate(String dateFormat) {
    // Make a new Date object. It will be initialized to the current time.
    Date now = new Date();
    String formattedDate;
    Format formatter;

    // Typical value for $DATE_FORMAT$ is "MM-dd-yyyy_HH:mm" e.g. 06-27-2006_14:52
    formatter = new SimpleDateFormat(dateFormat);
    formattedDate = formatter.format(now);
    return formattedDate;
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
}
