/*
* 01/07/2003 - 15:19:32
*
* PScanner.java -
* Copyright (C) 2003 Buero fuer Softwarearchitektur GbR
* ralf.meyer@karneim.com
* http://jrexx.sf.net
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/
package com.tc.jrexx.regex;

import java.util.Vector;
import java.text.ParsePosition;

class PScanner {
	public static final int UNLIMITED_MAX_LENGTH = Integer.MAX_VALUE;

	private final Automaton_Pattern.TerminalFormat[] terminalFormats;
	private final int[] terminalsMaxLength;
	private final boolean terminalFormatsAreExclusive;

	public PScanner(Automaton_Pattern.TerminalFormat[] terminalFormats) {
		this(terminalFormats,false);
	}

	public PScanner(Automaton_Pattern.TerminalFormat[] terminalFormats, boolean terminalFormatsAreExclusive) {
		this.terminalFormats = terminalFormats;
		this.terminalFormatsAreExclusive = terminalFormatsAreExclusive;

		final int n = this.terminalFormats.length;
		if (!this.terminalFormatsAreExclusive) {
			// reverse terminalFormats list
			for (int i= (n-1)>>1; i>=0; --i) {
			    Automaton_Pattern.TerminalFormat temp = this.terminalFormats[i];
			    this.terminalFormats[i] = this.terminalFormats[n-i];
			    this.terminalFormats[n-i] = temp;
			}
		}
		this.terminalsMaxLength = new int[n];
		for (int i=0; i<n; i++) {
			this.terminalsMaxLength[i] = this.terminalFormats[i].maxLength();
		}
	}


	public Vector scan(String source) {
		return this.scan(source,0);
	}


	public Vector scan(String source, int startIndex) {
		if (source==null) {
			String message = "null source specified";
			throw new IllegalArgumentException(message);
		}

		final char[] input = source.toCharArray();

		int firstIndexOfTerminalFormats = -1;
		int lastIndexOfTerminalFormats = -1;

		for (int i=this.terminalFormats.length-1; i>=0; i--) {
			if (this.terminalFormats[i]!=null) {
				lastIndexOfTerminalFormats = i;
				break;
			}
		}

		if (lastIndexOfTerminalFormats==-1) {
			String message = "no terminal formats added";
			throw new NullPointerException(message);
		}

		for (int i=0; i<=lastIndexOfTerminalFormats; i++) {
			if (this.terminalFormats[i]!=null) {
				firstIndexOfTerminalFormats = i;
				break;
			}
		}

// System.out.println("Scanner start on: "+new String(input,startIndex,input.length-startIndex));
		final Vector tokenList = new Vector();
		final int inputLength = input.length;
		final ParsePosition pos = new ParsePosition(startIndex);
		int index = startIndex;
		while (index<inputLength) {
			int longestMatch = -1;
			Object lastToken = null, token;
			for (int i=lastIndexOfTerminalFormats; i>=firstIndexOfTerminalFormats; i--) {
				if (this.terminalsMaxLength[i]>=longestMatch) {
					pos.setIndex(index);

//System.out.print(this.terminalFormats[i].getClass().getName().substring(this.terminalFormats[i].getClass().getName().indexOf(".")+1));
//System.out.print(".scan("+new String(input,pos.getIndex(),input.length-pos.getIndex())+") -> ");
					token = this.terminalFormats[i].parseObject(input,pos);
//System.out.println(token+" -> "+new String(input,pos.getIndex(),input.length-pos.getIndex()));
					final int matchLength = pos.getIndex()-index;
					if (token!=null) {
						if (this.terminalFormatsAreExclusive) {
							longestMatch = matchLength;
							lastToken = token;
							break;
						} else {
							if (matchLength>=longestMatch) {
								longestMatch = matchLength;
								lastToken = token;
							}
						}
					}
				}
			}
//if (lastToken!=null) System.out.println("Token recognized: "+lastToken);
			if (lastToken!=null) tokenList.addElement(lastToken);
			else {
				String message = "can not scan input:"
							+"\n"+new String(input,startIndex,input.length-startIndex)
							+"\nerrorPosition: "+index
							+"\n"+new String(input,index,input.length-index);
				throw new ParseException(message);
			}
			index+= longestMatch;
		}

		return tokenList;
	}

	public String toString() {
		StringBuffer answer = new StringBuffer();
		answer.append("Scanner(");
		if (this.terminalFormatsAreExclusive) answer.append("exclusive");
		answer.append(")");
		for (int i=0; i<this.terminalFormats.length; i++)
			if (this.terminalFormats[i]!=null)
				answer.append('\n').append(this.terminalFormats[i]);
		return answer.toString();
	}

}