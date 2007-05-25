/*
* 01/07/2003 - 15:19:32
*
* SAutomaton.java -
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

import com.tc.jrexx.automaton.*;
import java.util.*;
import java.io.*;

/**
 * This class represents a (non-)deterministic final automaton (NFA/DFA).
 * Use this class to create an automaton manually by adding states to the automaton and transitions to other states
 * or to browse through the automaton's states and implement your own matching strategies.
 * <br>to create an automaton manually try
 * <code>
 * <br>import com.karneim.util.collection.set.*;
 * <br>public class Test {
 * <br>  public static void main(String[] args) {
 * <br>    SAutomaton automaton = new SAutomaton();
 * <br>    {@link IStatePro} s1 = automaton.addState(false);
 * <br>    IStatePro s2 = automaton.addState(true);
 * <br>    s1.addTransition(new {@link CharSet}("0123456789"),s2);
 * <br>    s2.addTransition(new CharSet("0123456789"),s2);
 * <br>    automaton.setStartState(s1);
 * <br>  }
 * <br>}
 * <br>
 * </code>
 * <br>to browse through the automaton's states try
 * <code>
 * <br>  final {@link IStatePro} startState = automaton.getStartState();
 * <br>  final {@link StateProSet} states = new StateProSet(startState);
 * <br>  final {@link StateProSet.Iterator} it = states.iterator();
 * <br>  for (IStatePro state=it.next(); state!=null; state=it.next()) {
 * <br>    IStatePro.ITransition[] transitions = state.getTransitions();
 * <br>    for (int i=0; i transitions.length; ++i) {
 * <br>      states.add(transitions[i].getToState());
 * <br>      System.out.println(
 * <br>        "from "    + transitions[i].getFromState()
 * <br>      + " through " + transitions[i].getCharSet()
 * <br>      + " to "      + transitions[i].getToState()
 * <br>      );
 * <br>    }
 * <br>  }
 * <br>
 * </code>
 * <br>to implement own matching strategies try
 * <code>
 * <br>  /**
 * <br>   * returns true if input is an existing path through automaton's states
 * <br>   * otherwise false.
 * <br>   *
 * <br>  public static boolean incompleteMatch(SAutomaton automaton,String input) {
 * <br>    {@link IState} current = automaton.getStartState().visit();
 * <br>    for (int i=0; i input.length(); ++i) {
 * <br>      current = current.next(input.charAt(i));
 * <br>      if (current == null) return false;
 * <br>    }
 * <br>    return true;
 * <br>  }
 * </code>
 *
 * @author Ralf Meyer
 * @version 1.0
 */
public class SAutomaton {

  final class SAutomatonChangeListener implements Automaton.IChangedListener {

    public void stateAdded(Automaton.State state) {
      StatePro wrapper = (StatePro)SAutomaton.this.state2wrapper.get(state);
      if (wrapper==null) wrapper = new StatePro((AutomatonSet_String.SState)state);
    
      final Iterator it = SAutomaton.this.listeners.iterator();
      for (int i=SAutomaton.this.listeners.size(); i>0; --i) {
        ((SAutomaton.IChangeListener)it.next()).stateAdded(wrapper);
      }
    }

    public void stateRemoved(Automaton.State state) {
      StatePro wrapper = (StatePro)SAutomaton.this.state2wrapper.get(state);
      if (wrapper==null) wrapper = new StatePro((AutomatonSet_String.SState)state);
    
      final Iterator it = SAutomaton.this.listeners.iterator();
      for (int i=SAutomaton.this.listeners.size(); i>0; --i) {
        ((SAutomaton.IChangeListener)it.next()).stateRemoved(wrapper);
      }
    }

    public void startStateChanged(Automaton.State oldStartState,Automaton.State newStartState) {
      StatePro oldWrapper = null;
      if (oldStartState!=null) {
        oldWrapper = (StatePro)SAutomaton.this.state2wrapper.get(oldStartState);
        if (oldWrapper==null) oldWrapper = new StatePro((AutomatonSet_String.SState)oldStartState);
      }
    
      StatePro newWrapper = null;
      if (newStartState!=null) {
        newWrapper = (StatePro)SAutomaton.this.state2wrapper.get(newStartState);
        if (newWrapper==null) newWrapper = new StatePro((AutomatonSet_String.SState)newStartState);
      }
    
      final Iterator it = SAutomaton.this.listeners.iterator();
      for (int i=SAutomaton.this.listeners.size(); i>0; --i) {
        ((SAutomaton.IChangeListener)it.next()).startStateChanged(oldWrapper,newWrapper);
      }
    }
  }

  /**
   * The listener interface for receiving change events of a SAutomaton.
   * The class that is interested in processing an automaton's change event implements this interface.
   * A listener instance of that class is registered with the automaton using the automaton's addChangeListener method.
   * @author Ralf Meyer
   * @version 1.0
   */
  public interface IChangeListener {
    /**
     The Automaton invokes this method on all registered listener if a new state has been added to the automaton.
     */
    public void stateAdded(IStatePro state);
    /**
     The Automaton invokes this method on all registered listener if an existing state has been removed from the automaton.
     */
    public void stateRemoved(IStatePro state);
    /**
     The Automaton invokes this method on all registered listener if the automaton's current startState has been changed.
     Both oldStartState and newStartState can be null.
     */
    public void startStateChanged(IStatePro oldStartState, IStatePro newStartState);
  }

  protected class State implements IState {
    protected final AutomatonSet_String.ISState state;
    protected State(AutomatonSet_String.ISState state) {
      this.state = state;
    }

    public boolean isFinal() {
      return this.state.isFinal();
    }

    public IState next(char ch) {
      final Automaton.IState nextState = this.state.next(ch);
      return nextState==null ? null : new State((AutomatonSet_String.ISState)nextState);
    }
/*
    public IState nnext(String s) {
      return this.nnext(s,0,s.length());
    }

    public IState nnext(String s,int offset) {
      return this.nnext(s,0,s.length()-offset);
    }

    public IState nnext(String s,int offset,int length) {
      AutomatonSet_String.IState state = this.state;
      for (;length>0; --length, ++offset) {
        AutomatonSet_String.IState newState = state.next(s.charAt(offset));
        if (newState==null) break;
        state = newState;
      }
      return new State((AutomatonSet_String.ISState)state);
    }
*/

    public StateProSet getAllReachableStates() {
      final StateProSet result = new StateProSet();
      final Automaton.LinkedSet_State states = this.state.getAllReachableStates();
      for (Automaton.Wrapper_State w = states.elements; w!=null; w=w.next) {

        StatePro wrapper = (StatePro)SAutomaton.this.state2wrapper.get(w.state);
        if (wrapper==null) wrapper = new StatePro((AutomatonSet_String.SState)w.state);
        result.add(wrapper);
      }
      return result;
    }

    public String toString() {
      return this.state.toString();
    }
  }

  protected class Transition implements IStatePro.ITransition {
    protected final Automaton.State.Transition transition;
    protected Transition(Automaton.State.Transition transition) {
      this.transition = transition;
      SAutomaton.this.transition2wrapper.put(transition,this);
    }
/*
    protected void finalize() {
      SAutomaton.this.transition2wrapper.remove(this.transition);
    }
*/
    public IStatePro getFromState() {
      StatePro wrapper = (StatePro)SAutomaton.this.state2wrapper.get(this.transition.getFromState());
      if (wrapper==null)
        wrapper = new StatePro((AutomatonSet_String.SState)this.transition.getFromState());
      return wrapper;
    }

    public Set getLabels() {
      final HashSet labels = (HashSet)this.transition.properties;
//      if (labels!=null) return java.util.Collections.unmodifiableSet(labels);
      if (labels!=null) return labels;
      return java.util.Collections.EMPTY_SET;
    }

    public ISet_char getCharSet() {
      return this.transition.getCharSet();
    }

    public IStatePro getToState() {
      StatePro wrapper = (StatePro)SAutomaton.this.state2wrapper.get(this.transition.getToState());
      if (wrapper==null)
        wrapper = new StatePro((AutomatonSet_String.SState)this.transition.getToState());
      return wrapper;
    }

    public String toString() {
      final StringBuffer buffer = new StringBuffer();
      final AutomatonSet_String.SState fromState = (AutomatonSet_String.SState)this.transition.getFromState();
      final AutomatonSet_String.SState toState = (AutomatonSet_String.SState)this.transition.getToState();

      if (fromState.isFinal()) buffer.append('[').append(fromState.stateNr).append(']');
      else buffer.append('(').append(fromState.stateNr).append(')');

      if (this.transition.getCharSet()==null) {
        if (this.transition.properties==null) buffer.append(" --> ");
        else buffer.append(" - " ).append(this.transition.properties).append(": -> ");
      } else {
        if (this.transition.properties==null) buffer.append(" - ").append(this.transition.getCharSet()).append(" -> ");
        else buffer.append(" - ").append(this.transition.properties).append(':').append(this.transition.getCharSet()).append(" ->");
      }

      if (toState.isFinal()) buffer.append('[').append(toState.stateNr).append(']');
      else buffer.append('(').append(toState.stateNr).append(')');

      return buffer.toString();
    }

  }

  private final class StateProVisitedListener implements AutomatonSet_String.IStateVisitedListener {
    private final StatePro statePro;

    public StateProVisitedListener(StatePro statePro) {
      this.statePro = statePro;
    }

    public void stateVisited(Automaton.State state) {
      StatePro wrapper = (StatePro)SAutomaton.this.state2wrapper.get(state);
      if (wrapper==null) wrapper = new StatePro((AutomatonSet_String.SState)state);
      
      final Iterator it = statePro.visitListeners.iterator();
      for (int i=statePro.visitListeners.size(); i>0; --i) {
        ((IStatePro.IVisitListener)it.next()).stateVisited(wrapper);
      }
    }
    
    public void stateVisited(Automaton.State state,char ch) {
      StatePro wrapper = (StatePro)SAutomaton.this.state2wrapper.get(state);
      if (wrapper==null) wrapper = new StatePro((AutomatonSet_String.SState)state);
      
      final Iterator it = statePro.visitListeners.iterator();
      for (int i=statePro.visitListeners.size(); i>0; --i) {
        ((IStatePro.IVisitListener)it.next()).stateVisited(wrapper,ch);
      }
    }
    
    public void stateUnVisited(Automaton.State state) {
      StatePro wrapper = (StatePro)SAutomaton.this.state2wrapper.get(state);
      if (wrapper==null) wrapper = new StatePro((AutomatonSet_String.SState)state);
      
      final Iterator it = statePro.visitListeners.iterator();
      for (int i=statePro.visitListeners.size(); i>0; --i) {
        ((IStatePro.IVisitListener)it.next()).stateUnVisited(wrapper);
      }
    }
  }

  private final class StateProChangedListener implements AutomatonSet_String.ISStateChangedListener {
    
    private final StatePro statePro;

    public StateProChangedListener(StatePro statePro) {
      this.statePro = statePro;
    }

    public void transitionAdded(Automaton.State.Transition transition) {
      Transition wrapper = (Transition)SAutomaton.this.transition2wrapper.get(transition);
      if (wrapper==null) wrapper = new Transition(transition);
      
      final Iterator it = statePro.changeListeners.iterator();
      for (int i=statePro.changeListeners.size(); i>0; --i) {
        ((IStatePro.IChangeListener)it.next()).transitionAdded(wrapper);
      }
    }
    
    public void transitionRemoved(Automaton.State.Transition transition) {
      Transition wrapper = (Transition)SAutomaton.this.transition2wrapper.get(transition);
      if (wrapper==null) wrapper = new Transition(transition);
      final Iterator it = statePro.changeListeners.iterator();
      for (int i=statePro.changeListeners.size(); i>0; --i) {
        ((IStatePro.IChangeListener)it.next()).transitionRemoved(wrapper);
      }
    }
    
    public void isFinalChanged(AutomatonSet_String.SState state, boolean isFinal) {
      StatePro wrapper = (StatePro)SAutomaton.this.state2wrapper.get(state);
      if (wrapper==null) wrapper = new StatePro((AutomatonSet_String.SState)state);
      
      final Iterator it = statePro.changeListeners.iterator();
      for (int i=statePro.changeListeners.size(); i>0; --i) {
        ((IStatePro.IChangeListener)it.next()).isFinalChanged(wrapper,isFinal);
      }
    }
  }

  
  protected class StatePro implements IStatePro {

    protected final Automaton.IStateVisitedListener stateVisitedListener = new StateProVisitedListener(this);

    protected final Automaton.IStateChangedListener stateChangedListener = new StateProChangedListener(this);

    protected LinkedList visitListeners = null;
    protected LinkedList changeListeners = null;

    public void addVisitListener(IStatePro.IVisitListener listener) {
      if (this.visitListeners==null) {
        this.visitListeners = new LinkedList();
        this.state.addVisitedListener(this.stateVisitedListener);
      }
      this.visitListeners.add(listener);
    }

    public boolean removeVisitListener(IStatePro.IVisitListener listener) {
      if (this.visitListeners!=null) {
        final Iterator it = this.visitListeners.iterator();
        for (int i=this.visitListeners.size(); i>0; --i) {
          if (listener==it.next()) {
            if (this.visitListeners.size()>1) it.remove();
            else {
              this.state.removeVisitedListener(this.stateVisitedListener);
              this.visitListeners = null;
            }
            return true;
          }
        }
      }
      return false;
    }

    public void addChangeListener(IStatePro.IChangeListener listener) {
      if (this.changeListeners==null) {
        this.changeListeners = new LinkedList();
        this.state.addChangedListener(this.stateChangedListener);
      }
      this.changeListeners.add(listener);
    }

    public boolean removeChangeListener(IStatePro.IChangeListener listener) {
      if (this.changeListeners!=null) {
        final Iterator it = this.changeListeners.iterator();
        for (int i=this.changeListeners.size(); i>0; --i) {
          if (listener==it.next()) {
            if (this.changeListeners.size()>1) it.remove();
            else {
              this.state.removeChangedListener(this.stateChangedListener);
              this.changeListeners = null;
            }
            return true;
          }
        }
      }
      return false;
    }

    protected final AutomatonSet_String.SState state;
    protected StatePro(AutomatonSet_String.SState state) {
if (state==null) throw new Error("state==null");
      this.state = state;
      SAutomaton.this.state2wrapper.put(state,this);
    }

    protected void finalize() {
      SAutomaton.this.state2wrapper.remove(this.state);
    }

    protected SAutomaton parent() {
      return SAutomaton.this;
    }

    public boolean isFinal() {
      return this.state.isFinal();
    }

    public void setFinal(boolean isFinal) {
      this.state.setFinal(isFinal);
    }

    public IState visit() {
      return new State((AutomatonSet_String.ISState)this.state.visit());
    }

    public IStatePro.ITransition addTransition(ISet_char charSet,IStatePro toState) {
      final AutomatonSet_String.SState.Transition trans = this.state.addTransition(null,charSet,((StatePro)toState).state);
      Transition wrapper = (Transition)SAutomaton.this.transition2wrapper.get(trans);
      if (wrapper==null) wrapper = new Transition(trans);
      return wrapper;
    }

    public boolean removeTransition(IStatePro.ITransition transition) {
      return this.state.removeTransition(((Transition)transition).transition);
    }

    public void removeAllTransitions() {
      this.state.removeAllTransitions();
    }

    public StateProSet getAllReachableStates() {
      final StateProSet result = new StateProSet();
      final Automaton.LinkedSet_State states = this.state.getAllReachableStates();
      for (Automaton.Wrapper_State w = states.elements; w!=null; w=w.next) {

        StatePro wrapper = (StatePro)SAutomaton.this.state2wrapper.get(w.state);
        if (wrapper==null) wrapper = new StatePro((AutomatonSet_String.SState)w.state);
        result.add(wrapper);
      }
      return result;
    }


    public IStatePro.ITransition[] getTransitions() {
      final LinkedList list = new LinkedList();
      for (AutomatonSet_String.State.Transition trans=this.state.transitions; trans!=null; trans=trans.next) {
        Transition wrapper = (Transition)SAutomaton.this.transition2wrapper.get(trans);
        if (wrapper==null) wrapper = new Transition(trans);
//        list.add(wrapper);
          list.addFirst(wrapper);
      }
      return (IStatePro.ITransition[])list.toArray(new IStatePro.ITransition[list.size()]);
    }

    public IStatePro.ITransition[] getETransitions() {
      final LinkedList list = new LinkedList();
      for (AutomatonSet_String.State.Transition trans=this.state.eTransitions; trans!=null; trans=trans.next) {
        Transition wrapper = (Transition)SAutomaton.this.transition2wrapper.get(trans);
        if (wrapper==null) wrapper = new Transition(trans);
//        list.add(wrapper);
        list.addFirst(wrapper);
      }
      return (IStatePro.ITransition[])list.toArray(new IStatePro.ITransition[list.size()]);
    }

    /**
     * returns all transitions (normal and epsilon transitions) of this state.
     * the result array contains first all epsilon transitions and then all normal transitions
     */
    public IStatePro.ITransition[] getAllTransitions() {
      final LinkedList list = new LinkedList();
      for (AutomatonSet_String.State.Transition trans=this.state.transitions; trans!=null; trans=trans.next) {
        Transition wrapper = (Transition)SAutomaton.this.transition2wrapper.get(trans);
        if (wrapper==null) wrapper = new Transition(trans);
//        list.add(wrapper);
        list.addFirst(wrapper);
      }
      for (AutomatonSet_String.State.Transition trans=this.state.eTransitions; trans!=null; trans=trans.next) {
        Transition wrapper = (Transition)SAutomaton.this.transition2wrapper.get(trans);
        if (wrapper==null) wrapper = new Transition(trans);
//        list.add(wrapper);
        list.addFirst(wrapper);
      }
      return (IStatePro.ITransition[])list.toArray(new IStatePro.ITransition[list.size()]);
    }

    public int getStateNumber() {
      return this.state.stateNr;
    }

    public String toString() {
      if (this.isFinal()) return "["+this.state.stateNr+"]";
      return "("+this.state.stateNr+")";
    }

  }

  // should be IdentityMap
  protected transient HashMap state2wrapper = null;
  protected transient HashMap transition2wrapper = null;

  protected transient Automaton.IChangedListener automatonChangedListener = null;

  protected Automaton.IChangedListener getAutomatonChangedListener() {
    if (this.automatonChangedListener!=null) return this.automatonChangedListener;

    this.automatonChangedListener = new SAutomatonChangeListener();

    return this.automatonChangedListener;
  }

  protected transient LinkedList listeners = null;

  /**
   * Adds the specified listener to receive change events from this automaton.
   * The listener will be registered as listener in any case, even if it has been registered yet.
   * If a listener instance is added two times, it will receive events twice.
   * important: don't forget to remove the listener, if you don't need it any longer but still have the automaton in use.
   * Otherwise your listener won't be carbage collected (because it is registered with this automaton)
   * and still will receive events.
   */
  public void addChangeListener(SAutomaton.IChangeListener listener) {
    if (this.listeners==null) {
      this.listeners = new LinkedList();
      ((AutomatonSet_String)this.automaton).addChangedListener(this.getAutomatonChangedListener());
    }
    this.listeners.add(listener);
  }

  /**
   * Removes the specified listener so that it no longer receives change events from this automaton.
   * If the listener instance is registered more than ones, only one instance will be removed.
   * @param listener
   * @throw IllegalArgumentException - if null is passed as listener
   * @return true if the listener was registered else false
   */
  public boolean removeChangeListener(SAutomaton.IChangeListener listener) {
    if (this.listeners!=null) {
      final Iterator it = this.listeners.iterator();
      for (int i=this.listeners.size(); i>0; --i) {
        if (listener==it.next()) {
          if (this.listeners.size()>1) it.remove();
          else {
            this.automaton.removeChangedListener(this.automatonChangedListener);
            this.automatonChangedListener = null;
            this.listeners = null;
          }
          return true;
        }
      }
    }
    return false;
  }




  protected transient AutomatonSet_String automaton;

  /**
   * Creates a new empty automaton
   */
  public SAutomaton() {
    this(new AutomatonSet_String());
  }

  public SAutomaton(FSAData data) {
    this(new AutomatonSet_String());
    this.init(data);
  }

  protected static FSAData toFSAData(Object obj) {
    if (obj.getClass()!=FSAData.class) {
      SAutomatonData data = (SAutomatonData)obj;

      FSAData.State[] newStates = new FSAData.State[data.states==null ? 0 : data.states.length];
      for (int i=0; i<newStates.length; ++i) {
        SAutomatonData.State state = data.states[i];
        if (state!=null) {
          FSAData.State.Transition[] newTransitions =
            new FSAData.State.Transition[state.transitions==null ? 0 : state.transitions.length];
          for (int t=0; t<newTransitions.length; ++t) {
            SAutomatonData.State.Transition trans = state.transitions[t];
            newTransitions[t] = new FSAData.State.Transition(trans.properties,trans.charSet,trans.toStateNumber);
          }
          newStates[i] = new FSAData.State(state.number,state.isFinal,newTransitions,state.transitionsAreDeterministic);
        }
      }
      return new FSAData(newStates,data.startStateNumber,data.isDeterministic);

    } else {
      FSAData data = (FSAData)obj;
      switch(data.objectVersion) {
        case FSAData.classVersion : return data;
        default : return data;
      }
    }
  }

  public SAutomaton(InputStream automatonDataStream) throws IOException, ClassNotFoundException {
    this(new AutomatonSet_String());
//    this.init((FSAData)automatonDataStream.readObject());
    this.init(toFSAData(new ObjectInputStream(automatonDataStream).readObject()));
  }

  protected SAutomaton(AutomatonSet_String automaton) {
    this.automaton = automaton;
    this.state2wrapper = new HashMap();
    this.transition2wrapper = new HashMap();
  }

  public boolean isDeterministic() {
    return this.automaton.isDeterministic();
  }

  /**
   * Returns the current start state of the automaton.
   * important: The result is null, if and only if the current start state is null
   * @return the current start state of the automaton
   */
  public IStatePro getStartState() {
    Automaton.State startState = ((AutomatonSet_String)this.automaton).getStartState();
    if (startState==null) return null;

    StatePro wrapper = (StatePro)this.state2wrapper.get(startState);
    if (wrapper==null)
      wrapper = new StatePro((AutomatonSet_String.SState)startState);
    return wrapper;
  }


  /**
   * Sets the automaton's start state to the specified state.
   * If the automaton should have no start state pass null.
   * <br>important: the specified state must be a state of this automaton which means
   * <br>a) the state must have been created via the addState() method of this automaton
   * <br>b) the state must not have been removed from this automaton via the removeState method
   * @param state
   */
  public void setStartState(IStatePro state) {
    if ((state instanceof StatePro)==false) throw new IllegalArgumentException("state is no state of mine");

    final StatePro wrapper = (StatePro)state;
    if (wrapper.parent()!=this) throw new IllegalArgumentException("state is no state of mine");

    this.automaton.setStartState(wrapper.state);
  }

  /**
   * Adds a new non final state to this automaton.
   * @return a new state
   */
  public IStatePro addState() {
    return this.addState(false);
  }

  /**
   * Adds a new final or non final state to this automaton.
   * @return a new state
   */
  public IStatePro addState(boolean isFinal) {
    final AutomatonSet_String.SState newState = this.automaton.addState(isFinal);
    StatePro wrapper = (StatePro)SAutomaton.this.state2wrapper.get(newState);
    if (wrapper==null) wrapper = new StatePro((AutomatonSet_String.SState)newState);
    return wrapper;
  }

  /**
   * Removes the specified state from this automaton.
   * <br>First all transitions pointing to state are removed and then the state itself.
   * <br>If state is this automaton's start state then first of all this automaton's start state is set to null.
   * <br>important: the specified state must have been created via the addState method of this automaton
   * otherwise an IllegalArgumentException is thrown.
   * @param state
   * @return true if this automaton owns the specified state else false
   */
  public boolean removeState(IStatePro state) {
    if ((state instanceof StatePro)==false) throw new IllegalArgumentException("state is no state of mine");

    final StatePro wrapper = (StatePro)state;
    if (wrapper.parent()!=this) throw new IllegalArgumentException("state is no state of mine");
    return this.automaton.removeState(wrapper.state);
  }

  /**
   * Removes all states of this automaton.
   */
  public void clear() {
    this.automaton.clear();
  }

  /**
   * Minimizes this automaton as much as possible.
   * The resulting automaton has as less states as possible.
   * <br>important: the current implementation removes all properties from all transitions
   */
  public void minimize() {
    this.automaton.minimize();
  }

  /**
   * Returns all states of this automaton whatever they are reachable through the current start state or not.
   * @return
   */
  public StateProSet getStates() {
    final StateProSet result = new StateProSet();
    final Automaton.LinkedSet_State states = this.automaton.getStates();
    for (Automaton.Wrapper_State w = states.elements; w!=null; w=w.next) {
      StatePro wrapper = (StatePro)SAutomaton.this.state2wrapper.get(w.state);
      if (wrapper==null) wrapper = new StatePro((AutomatonSet_String.SState)w.state);
      result.add(wrapper);
    }
    return result;
  }

  public void complement() {
    this.automaton.complement();
  }

  public void addAll(SAutomaton automaton) {
    this.automaton.addAll(automaton.automaton);
  }

  public void retainAll(SAutomaton automaton) {
    this.automaton.retainAll(automaton.automaton);
  }

  public void removeAll(SAutomaton automaton) {
    this.automaton.removeAll(automaton.automaton);
  }

  public String toString() {
    return this.automaton.toString();
  }

  private String getCharSet(ISet_char charSet) {
    if (charSet==null) return null;
    final StringBuffer buffer = new StringBuffer(charSet.size());
    ISet_char.Iterator it_charSet = charSet.iterator();
    for (int i=charSet.size(); i>0; --i) buffer.append(it_charSet.next());

    ISet_char cs = new CharSet(buffer.toString());
    if (cs.equals(charSet)==false)
      throw new Error(""+charSet+"   "+cs);

    return buffer.toString();
  }

  public FSAData toData() {
    Automaton.LinkedSet_State xxx = this.automaton.getStates();
    AutomatonSet_String.SState[] states = new AutomatonSet_String.SState[xxx.size()];
    int x=0;
    for (Automaton.Wrapper_State w=xxx.elements; w!=null; w=w.next,++x) {
      states[x] = (AutomatonSet_String.SState)w.state;
    }

    FSAData.State[] data_states = new FSAData.State[states.length];
    for (int i=0; i<states.length; ++i) {
      LinkedList data_transitions = new LinkedList();
      for (Automaton.State.Transition trans=states[i].transitions; trans!=null; trans=trans.next) {
//        int toStateNr = 0; while (states[toStateNr]!=trans.toState) ++toStateNr;
        data_transitions.addFirst(
          new FSAData.State.Transition(
            trans.properties,
            this.getCharSet(trans.charSet)
//            ,toStateNr
            ,trans.toState.stateNr
          )
        );
      }
      for (Automaton.State.Transition trans=states[i].eTransitions; trans!=null; trans=trans.next) {
//        int toStateNr = 0; while (states[toStateNr]!=trans.toState) ++toStateNr;
        data_transitions.addFirst(
//          new SAutomatonData.State.Transition(trans.properties,null,toStateNr)
          new FSAData.State.Transition(trans.properties,null,trans.toState.stateNr)
        );
      }

      FSAData.State.Transition[] transitions =
        (FSAData.State.Transition[])data_transitions.toArray(
          new FSAData.State.Transition[data_transitions.size()]
        );

//      data_states[i] = new SAutomatonData.State(i,states[i].isFinal,transitions,states[i].isDeterministic());
      data_states[i] = new FSAData.State(states[i].stateNr,states[i].isFinal,transitions,states[i].isDeterministic());
    }

    final Automaton.State startState = this.automaton.getStartState();
    if (startState==null) return new FSAData(data_states,null,this.automaton.isDeterministic());

//    int startStateNr = 0; while (states[startStateNr]!=startState) ++startStateNr;

    FSAData result = new FSAData(data_states,new Integer(startState.stateNr),this.automaton.isDeterministic());

    return result;
  }

  protected void init(FSAData a) {
//    this.automatonChangedListener = null;
//    this.state2wrapper = new HashMap();
//    this.transition2wrapper = new HashMap();
//    this.automaton = this.newAutomaton();

    final HashMap map = new HashMap();

    if (a.states!=null) {
      for (int i=0; i<a.states.length; ++i) {
        Integer stateNr = new Integer(a.states[i].number);
        if (map.containsKey(stateNr))
          throw new IllegalArgumentException("bad automatonData: state with number "+stateNr+" does already exists");

        AutomatonSet_String.SState state =
          this.automaton.addState(a.states[i].isFinal,a.states[i].number);

        map.put(stateNr,state);
      }

      for (int i=0; i<a.states.length; ++i) {
        FSAData.State stateData = a.states[i];

        AutomatonSet_String.SState state =
          (AutomatonSet_String.SState)map.get(new Integer(stateData.number));

        if (stateData.transitions!=null) {
          for (int t=0; t<stateData.transitions.length; ++t) {
            FSAData.State.Transition transData = stateData.transitions[t];

            CharSet charSet = (transData.charSet==null) ? null : new CharSet(transData.charSet);

            AutomatonSet_String.SState toState =
              (AutomatonSet_String.SState)map.get(new Integer(transData.toStateNumber));

            state.addTransition(transData.properties,charSet,toState);
          }
        }

        state.setDeterministic(stateData.transitionsAreDeterministic);
      }
    }

    if (a.startStateNumber!=null) {
      AutomatonSet_String.SState startState =
        (AutomatonSet_String.SState)map.get(a.startStateNumber);

      if (startState==null)
        throw new IllegalArgumentException("bad automatonData: startState "+a.startStateNumber+" does not exists");

      this.automaton.setStartState(startState);
    }

    this.automaton.setDeterministic(
      a.isDeterministic
    );
  }

  public void toData(OutputStream automatonDataStream) throws IOException {
    new ObjectOutputStream(automatonDataStream).writeObject(this.toData());
  }



}