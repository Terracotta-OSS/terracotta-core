/* -*-tab-width: 4; -*-
 * 
 * OtherUtils.java 10/06/1999
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

public class OtherUtils
{
	private static final int BUFFER_SIZE = 64 * 1024;
	private static byte [] BUFFER = null;
	
	/**
	 *	Create a buffer of the size indicated, and copy the InputStream to the
	 *	OutputStream fully.
	 *	
	 *	@returns the number of bytes copied
	 */
	public static long bufStreamCopy(InputStream is, OutputStream os, int bufferSizeInBytes )
			throws IOException
	{
		byte[] b = new byte[bufferSizeInBytes];
		
		return bufStreamCopy( is, os, b );
	}
	
	/**
	 *	Copy the InputStream to the OutputStream fully, using a shared buffer.
	 *	
	 *	@returns the number of bytes copied
	 */
	public static long bufStreamCopy(InputStream is, OutputStream os)
			throws IOException
	{
		/*	Lazily allocate this chunk. */
		if ( BUFFER == null )
		{
			BUFFER = new byte[BUFFER_SIZE];
		}
		
		synchronized ( BUFFER )
		{
			return bufStreamCopy( is, os, BUFFER );			
		}
	}
	
	/**
	 *	Copy the InputStream to the OutputStream fully, using the
	 *	indicated buffer.
	 *	
	 *	@returns the number of bytes copied
	 */
	public static long bufStreamCopy(InputStream is, OutputStream os, byte [] b )
	        throws IOException
	{
		long bytes = 0;
		
		int read = is.read( b, 0, b.length );
		while ( read != -1 )
		{
			os.write( b, 0, read );
			bytes += read;
			read = is.read( b, 0, b.length );
		}
		
		os.flush();
		
		return bytes;
	}
}
