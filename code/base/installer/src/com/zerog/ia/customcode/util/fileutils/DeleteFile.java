/* -*-tab-width: 4; -*-
 * 
 * DeleteFile.java 10/06/1999
 * 
 * Copyright 1999 Zero G Software, Inc., All rights reserved.
 * 514 Bryant St., California, 94107, U.S.A.
 * 
 * CONFIDENTIAL AND PROPRIETARY INFORMATION - DO NOT DISCLOSE.
 *
 * The following program code (the "Software") consists of
 * unpublished, confidential and proprietary information of
 * Zero G Software, Inc.  The use of the Software is governed
 * by a license agreement and protected by trade secret and
 * copyright laws.  Disclosure of the Software to third
 * parties, in any form, in whole or in part, is expressly
 * prohibited except as authorized by the license agreement.
 * 
 * The DevNet license agreement gives you specific rights
 * with regard to using this code in your own projects and
 * in derivative works.  See the DevNet license agreement
 * for more information.
 * 
 */

package com.zerog.ia.customcode.util.fileutils;

import java.io.*;

import com.zerog.ia.api.pub.*;

/**
 *	CopyDirectory takes a specified file and copies it
 *	to a new location.
 *	
 *	@see com.acme.dialogs.CustomCodeAction
 *	
 *	@version 3.0.0
 */
public class DeleteFile extends CustomCodeAction
{
	private static final String INSTALL_MESSAGE = "Removing files";
	private static final String UNINSTALL_MESSAGE = "";
	private static final String ERR_MSG = "DeleteFile: no file specified.";
	private static final String FILE_VAR_NAME = "$DeleteFile_File$";
	private boolean isLoaded = false;
	/**
	 *	This is the method that is called at install-time. The InstallerProxy
	 *	instance provides methods to access information in the installer,
	 *	set status, and control flow.<p>
	 *
	 *	For the purposes of the this action (DeleteFile), this method
	 *	<ol>
	 *	<li>gets its parameters from InstallAnywhere Variables,</li>
	 *	<li>checks the parameters' validity,</li>
	 *	<li>and deletes the file.</li></ol>
	 *	
	 *	@see com.zerog.ia.api.pub.CustomCodeAction#install
	 */
	public void install( InstallerProxy ip ) throws InstallException
	{

		if(isLoaded == true)
			return;
		isLoaded = true;

		/*	Get input from InstallAnywhere Variables.  The literal contents of
			the Variables are retieved into the Strings. */
		String fileToDelete = ip.substitute( FILE_VAR_NAME );

		/*	substitute() will return an empty string for any InstallAnywhere 
			Variable that hasn't been assigned yet.  
		
		/*	If there is both a source and a destination, copy the files. */
		if ( fileToDelete.equals("") )
		{
			error( fileToDelete );
		}
		else
		{
			try {
				deleteFile( fileToDelete );
			} catch ( IOException ioe ) {
				throw new NonfatalInstallException( ioe.getMessage() );
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
	public void uninstall( UninstallerProxy up ) throws InstallException
	{
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
	
	/**
	 *	Delete the file represented by fileToDelete.
	 */
	public static void deleteFile( String fileToDelete ) throws IOException
	{
		deleteFile( new File( fileToDelete ) );
	}
	
	/**
	 *	Delete the file represented by fileToDelete.
	 */
	public static void deleteFile( File fileToDelete ) throws IOException
	{
		if ( fileToDelete.isFile() )
		{
			if ( ! fileToDelete.delete() )
			{
				throw new IOException( "Couldn't delete file: "
					+ fileToDelete.getAbsolutePath() );
			}
		}
		else
		{
			throw new IOException(
				"Couldn't delete file because file is a directory: " +
					fileToDelete.getAbsolutePath() );
		}
	}
	
	/**
	 *	Print something to indicate that the parameters were not acceptable.
	 */
	private void error( String path )
	{
		System.err.println( ERR_MSG );
		System.err.println( "Path: " + path );
	}
}
