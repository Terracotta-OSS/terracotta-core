/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.zerog.ia.customcode.util.fileutils;

import java.io.*;

import com.zerog.ia.api.pub.*;

/**
 *	Rename renames a file or directory.
 *	
 *	@see com.acme.dialogs.CustomCodeAction
 *	
 *	@version 3.0.0
 */
public class Rename extends CustomCodeAction
{
	private static final String INSTALL_MESSAGE = "Renaming files";
	private static final String UNINSTALL_MESSAGE = "";
	private static final String ERR_MSG =
		"Rename: no target or new name specified.";
	private static final String TARGET_VAR_NAME = "$Rename_Target$";
	private static final String NEWNAME_VAR_NAME = "$Rename_NewName$";
	private static final String SUCCESS = "SUCCESS";
	private static final String ERROR = "ERROR";
	private boolean isLoaded = false;
	/**
	 *	This is the method that is called at install-time. The InstallerProxy
	 *	instance provides methods to access information in the installer,
	 *	set status, and control flow.<p>
	 *
	 *	For the purposes of the this action (Rename), this method
	 *	<ol>
	 *	<li>gets its parameters from InstallAnywhere Variables,</li>
	 *	<li>checks the parameters' validity,</li>
	 *	<li>and renames the file.</li></ol>
	 *	
	 *	@see com.zerog.ia.api.pub.CustomCodeAction#install
	 */
	public void install( InstallerProxy ip ) throws InstallException
	{	
		/**
		 * InstallAnywhere variable:RENAME_SUCCESS
		 *
		 * possible return values are: 
     * SUCCESS
		 * ERROR
		 *
		 */	 
		ip.setVariable("RENAME_SUCCESS", Rename.SUCCESS);
	
	
		System.out.println("Rename: RENAME_SUCCESS=" + Rename.SUCCESS);

		/*	Get input from InstallAnywhere Variables.  The literal contents of
			the Variables are retieved into the Strings. */
		String target = ip.substitute( TARGET_VAR_NAME );
		String newName = ip.substitute( NEWNAME_VAR_NAME );
		
		
		/*	substitute() will return an empty string for any InstallAnywhere 
			Variable that hasn't been assigned yet.  
		
		/*	If there is both a source and a destination, copy the files. */
		
		if (   target.equals("")
			|| newName.equals("") )
		{
			error( target, newName );
		}
		else
		{
			System.out.println("target = " + target + ", newName = " + newName);
			try {
				rename( target, newName );
			} catch ( IOException ioe ) {
				System.out.println("Rename: Exception = "+ ioe.getMessage());
				ip.setVariable("RENAME_SUCCESS", Rename.ERROR);
				System.out.println("Rename: RENAME_SUCCESS=" + Rename.ERROR);
				//throw new NonfatalInstallException( ioe.getMessage() );
			}
		}
	}
	
	/**
	 *	This is the method that is called at uninstall-time.  For
	 *	an example of how to effect the uninstallation of something
	 *	like this, please see com.acme.fileutils.CopyFile.
	 *	
	 *	@see com.acme.fileutils.CopyFile
	 *	@see com.zerog.ia.api.pub.CustomCodeAction#uninstall
	 */
	public void uninstall( UninstallerProxy up ) throws InstallException {
    //
	}
	
	/**
	 *	This method will be called to display a status message during the
	 *	installation.
	 *	
	 *	@see com.zerog.ia.api.pub.CustomCodeAction#getInstallStatusMessage
	 */
	public String getInstallStatusMessage()
	{
		return INSTALL_MESSAGE;
	}
	
	/**
	 *	This method will be called to display a status message during the
	 *	uninstall.
	 *	
	 *	@see com.zerog.ia.api.pub.CustomCodeAction#getUninstallStatusMessage
	 */
	public String getUninstallStatusMessage()
	{
		return UNINSTALL_MESSAGE;
	}
	
	public static void rename( String target, String newName )
		throws IOException
	{
		rename( new File( target ), newName );
	}
	
	/**
	 *	Rename the file represented by source to the file
	 *	represented by destination.
	 */
	public static void rename( File target, String newName )
		throws IOException
	{
		if ( ! target.renameTo( new File( target.getParent(), newName ) ) )
			throw new IOException( "Couldn't rename file." );
	}
	
	/**
	 *	Print something to indicate that the parameters were not acceptable.
	 */
	private void error( String target, String newName )
	{
		System.err.println( ERR_MSG );
		System.err.println( "Target: " + target );
		System.err.println( "New Name: " + newName );
	}
}
