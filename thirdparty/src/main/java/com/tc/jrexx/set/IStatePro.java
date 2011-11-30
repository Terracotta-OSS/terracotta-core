/*
* 01/07/2003 - 15:19:32
*
* IStatePro.java -
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

import com.tc.jrexx.set.ISet_char;

/**
 * This interface represents a state of an automaton created via the automaton's addState method.
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: B??f??oftwarearchitektur   www.karneim.com</p>
 * @author Ralf Meyer
 * @version 1.0
 */
public interface IStatePro {
  public interface ITransition {
    public IStatePro getFromState();
    public ISet_char getCharSet();
    public IStatePro getToState();
  }


  /**
   * The listener interface for receiving visit events of an IStatePro.
   * The class that is interested in processing a state's visit event implements this interface.
   * A listener instance of that class is registered with the state using the state's addVisitListener method.
   * <br>A state will be visited every time it is the destination state of a transition that has been visited.
   * <br>A state that becomes visited, will then visit all its epsilon transitions
   * and these transitions will visit all destination states and so on.
   * <br>State visiting occurs using the methods
   * <br>IStatePro.visit
   * <br>IState.next
   *
   * <p>Copyright: Copyright (c) 2002</p>
   * <p>Company: B??f??oftwarearchitektur   www.karneim.com</p>
   * @author Ralf Meyer
   * @version 1.0
   */
  public interface IVisitListener {

    /**
     * The state invokes this method on all registered listener if it is visited through an epsilon transition.
     * @param state
     */
    public void stateVisited(IStatePro state);
    /**
     * The state invokes this method on all registered listener if it is visited through an transition with char ch.
     */
    public void stateVisited(IStatePro state,char ch);

    public void stateUnVisited(IStatePro state);
  }

  /**
   * The listener interface for receiving change events of an IStatePro.
   * The class that is interested in processing a state's change event implements this interface.
   * A listener instance of that class is registered with the state using the state's addChangeListener method.
   * <p>Copyright: Copyright (c) 2002</p>
   * <p>Company: B??f??oftwarearchitektur   www.karneim.com</p>
   * @author Ralf Meyer
   * @version 1.0
   */
  public interface IChangeListener {
    /**
     * The state invokes this method on all registered listener if a transition is added to the state
     */
    public void transitionAdded(IStatePro.ITransition transition);
    /**
     * The state invokes this method on all registered listener if a transition is removed from the state
     */
    public void transitionRemoved(IStatePro.ITransition transition);
    /**
     * The state invokes this method on all registered listener if it's final property is changed.
     */
    public void isFinalChanged(IStatePro state,boolean isFinal);
  }

  public void addVisitListener(IStatePro.IVisitListener listener);
  public boolean removeVisitListener(IStatePro.IVisitListener listener);

  public void addChangeListener(IStatePro.IChangeListener listener);
  public boolean removeChangeListener(IStatePro.IChangeListener listener);

  /**
   * @return true if the state is a final state else false
   */
  public boolean isFinal();

  /**
   * Makes this state final or non final.
   */
  public void setFinal(boolean isFinal);

  /**
   * Adds a new transition to this state.
   * The transition is defined by it's character set <CODE>charSet</CODE> and it's destionation
   * state <CODE>toState</CODE>, so that you can transit from this state to the destination state
   * only with a character contained in <CODE>charSet</CODE>. There is only one exception,
   * if <CODE>charSet</CODE> is null, an epsilon transition will be added, which means that there
   * are no chars needed to get to the destinationState <CODE>toState</CODE>; in other words a
   * state that has an epsilon transition can get through this epsilon transition to the destination
   * state <CODE>toState</CODE> without any char, so that we can say that <CODE>toState</CODE> melts
   * into the state.
   * @param charSet the characters for this transition
   * @param toState the destination state where to transit to
   * @return the new transition
   */
  public IStatePro.ITransition addTransition(ISet_char charSet,IStatePro toState);

  /**
   * Removes the specified transition from this state.
   * <br>important: the specified transition must be a transition
   * created via this state's addTransition method, otherwise an IllegalArgumentException is thrown
   * @param transition
   * @return true if transition was a transition of this state else false
   */
  public boolean removeTransition(IStatePro.ITransition transition);

  public void removeAllTransitions();

  public IStatePro.ITransition[] getTransitions();
  public IStatePro.ITransition[] getETransitions();
  public IStatePro.ITransition[] getAllTransitions();

  /**
   * Returns all states that are reachable from this state through it's transitions and so on.
   * <br>important: this state is only element of the returned set, if it is an element of a loop
   * @return all reachable states as a set
   */
  public StateProSet getAllReachableStates();

  /**
   * Visits this state with an epsilon transition and returns its epsilon closure.
   * @return the epsilon closure of this state
   */
  public IState visit();

  public int getStateNumber();
}