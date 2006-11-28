/*
* 01/07/2003 - 15:19:32
*
* ISet_char.java -
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
package com.tc.jrexx.set;

import java.io.Serializable;

public interface ISet_char extends Serializable,Cloneable {

  public interface Iterator {
    public boolean hasNext();
    public char next();
  }

  public boolean contains(char ch);
  public boolean isEmpty();
  public int size();
  public ISet_char.Iterator iterator();


//  public ISet_char getComplement();

  /**
   * return this.addAll(set).
   * return C = A | B = this | set
   */
//  public ISet_char getUnionSet(ISet_char set);

  /**
   * return this.retainAll(set).
   * return C = A & B = this & set
   */
//  public ISet_char getIntersectionSet(ISet_char set);

  /**
   * return this.removeAll(set).
   * return C = A \ B = this \ set
   */
//  public ISet_char getDifferenceSet(ISet_char set);



  public void clear();

  public boolean add(char ch);
  public boolean remove(char ch);

  public void complement();

  public void addAll(String chars);

  /**
   * adds all chars from set to this ISet_char without adding doublicates.
   * returns the number of chars added to this ISet_char.
   */
  public void addAll(ISet_char set);

  /**
   * Removes from this set all of its elements that are contained in the specified set (optional operation).
   * returns the number of chars that were removed.
   */
  public void removeAll(ISet_char set);

  public void retainAll(ISet_char set);

  public Object clone();

}