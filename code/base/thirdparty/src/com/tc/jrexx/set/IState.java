/*
* 01/07/2003 - 15:19:32
*
* IState.java -
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

/**
 * <CODE>IState</CODE> is the epsilon enclosure of one or more <CODE>{@link IStatePro}</CODE>.
 * The epsilon enclosure of an <CODE>IStatePro</CODE> are the <CODE>IStatePro</CODE> itself
 * and all states that are reachable through  epsilon transitions (see {@link IStatePro.addTransition})
 * beginning with that <CODE>IStatePro</CODE>.
 * <BR>You can get an epsilon enclosure of an <CODE>IStatePro startState</CODE> manually by this code:
 * <CODE>
 * <BR>  final IStatePro startState;
 * <BR>  final {@link StateProSet} epsilonClosure = new StateProSet(startState);
 * <br>  final {@link StateProSet.Iterator} it = states.iterator();
 * <br>  for (IStatePro state=it.next(); state!=null; state=it.next()) {
 * <br>    IStatePro.ITransition[] transitions = state.getETransitions();
 * <br>    for (int i=0; i transitions.length; ++i) {
 * <br>      epsilonClosure.add(transitions[i].getToState());
 * <br>    }
 * <BR>  }
 * </CODE>
 *
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public interface IState {


  public boolean isFinal();

  /**
   * returns the <CODE>IState</CODE> of all <CODE>IStatePro</CODE> that are reachable from
   * this <CODE>IState</CODE> with a character <CODE>ch</CODE>.
   * @param ch
   * @return
   */
  public IState next(char ch);

  /**
   * Returns all states that are reachable from this state through it's transitions and so on.
   * <br>important: this state is only element of the returned set, if it is an element of a loop
   * @return all reachable states as a set
   */
  public StateProSet getAllReachableStates();
//    public StateProSet getNextStates();

}