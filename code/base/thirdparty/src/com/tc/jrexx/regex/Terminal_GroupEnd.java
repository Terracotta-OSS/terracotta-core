/*
* 01/07/2003 - 15:19:32
*
* Terminal_GroupEnd.java -
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

final class Terminal_GroupEnd {
    static final Terminal_GroupEnd INSTANCE = new Terminal_GroupEnd(null);
    final String name;
    protected Terminal_GroupEnd(String name) {
      this.name = name;
    }

    public String toString() {
      return (this.name==null) ? ")" : "</"+this.name+">";
    }


}