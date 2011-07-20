/*
* 01/07/2003 - 15:19:32
*
* Automaton.java -
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
package com.tc.jrexx.automaton;

import java.util.*;
import java.lang.ref.SoftReference;

import com.tc.jrexx.set.ISet_char;
import com.tc.util.FindbugsSuppressWarnings;

public abstract class Automaton implements Cloneable {

  protected final static int TRUE = 1;
  protected final static int FALSE = 0;
  protected final static int UNKNOWN = -1;

  private static int currentAutomatonNr = 0;

//  static final com.karneim.util.BitSet BITSET = new com.karneim.util.BitSet();

  public interface IChangedListener {
    public void stateAdded(State state);
    public void stateRemoved(State state);
    public void startStateChanged(State oldStartState,State newStartState);
  }

  protected LinkedList listeners = null;

  protected void addChangedListener(Automaton.IChangedListener listener) {
    if (listener==null) throw new IllegalArgumentException("listener==null");
    if (this.listeners==null) this.listeners = new LinkedList();
    this.listeners.add(listener);
  }

  protected boolean removeChangedListener(Automaton.IChangedListener listener) {
    if (this.listeners!=null) {
      final Iterator it = this.listeners.iterator();
      for (int i=this.listeners.size(); i>0; --i) {
        if (listener==it.next()) {
          if (this.listeners.size()>1) it.remove();
          else this.listeners = null;

          return true;
        }
      }
    }
    return false;
  }

  public interface IStateVisitedListener {
    public void stateVisited(Automaton.State state);
    public void stateVisited(Automaton.State state,char ch);
    public void stateUnVisited(Automaton.State state);
  }
  public interface IStateChangedListener {
    public void transitionAdded(Automaton.State.Transition transition);
    public void transitionRemoved(Automaton.State.Transition transition);
  }

  public interface ITransitionVisitedListener {
    public void transitionVisited(Automaton.State.Transition transition);
    public void transitionVisited(Automaton.State.Transition transition,char ch);
  }

  public static final class Wrapper_State {
    public final State state;
    public Wrapper_State next=null;

    public Wrapper_State(State state) {
      this.state = state;
    }
  }

  public interface IState extends Cloneable {
    public IState next(char ch);
    public LinkedSet_State getAllReachableStates();
    public Object clone();
  }

  public class State implements IState {
    private final static int TRUE = 1;
    private final static int FALSE = 0;
    private final static int UNKNOWN = -1;

    transient protected LinkedList visitedListeners = null;
    transient protected LinkedList changedListeners = null;

    public void addVisitedListener(IStateVisitedListener listener) {
      if (this.visitedListeners==null) this.visitedListeners = new LinkedList();
      this.visitedListeners.add(listener);
    }

    public boolean removeVisitedListener(IStateVisitedListener listener) {
      if (this.visitedListeners!=null) {
        final Iterator it = this.visitedListeners.iterator();
        for (int i=this.visitedListeners.size(); i>0; --i) {
          if (listener==it.next()) {
            if (this.visitedListeners.size()>1) it.remove();
            else this.visitedListeners = null;

            return true;
          }
        }
      }
      return false;
    }

    public void addChangedListener(IStateChangedListener listener) {
      if (this.changedListeners==null) this.changedListeners = new LinkedList();
      this.changedListeners.add(listener);
    }

    public boolean removeChangedListener(IStateChangedListener listener) {
      if (this.changedListeners!=null) {
        final Iterator it = this.changedListeners.iterator();
        for (int i=this.changedListeners.size(); i>0; --i) {
          if (listener==it.next()) {
            if (this.changedListeners.size()>1) it.remove();
            else this.changedListeners = null;

            return true;
          }
        }
      }
      return false;
    }

    public final IState visit() {
      if (this.eTransitions==null) {
        if (this.visitedListeners!=null) {
          final Iterator it = this.visitedListeners.iterator();
          for (int i=this.visitedListeners.size(); i>0; --i)
            ((IStateVisitedListener)it.next()).stateVisited(this);
        }
        return this;
      }

      final LinkedSet_State eClosure = Automaton.this.newLinkedSet_State(this);
      for (Wrapper_State w=eClosure.elements; w!=null; w=w.next) {
        if (w.state.visitedListeners!=null) {
          final Iterator it = w.state.visitedListeners.iterator();
          for (int i=w.state.visitedListeners.size(); i>0; --i)
            ((IStateVisitedListener)it.next()).stateVisited(w.state);
        }

        for (Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
          trans.visit(eClosure);
        }
      }

      return eClosure;
    }

    protected final void unVisit() {
      if (this.visitedListeners!=null) {
        final Iterator it = this.visitedListeners.iterator();
        for (int i=this.visitedListeners.size(); i>0; --i)
          ((IStateVisitedListener)it.next()).stateUnVisited(this);
      }
    }


    ///////////////////////////////////////////////////////////////////////////
    //                        T r a n s i t i o n
    ////////////////////////////////////////////////////////////////////////////


    public final class Transition {

      transient LinkedList transitionVisitedListeners = null;

      public void addVisitedListener(ITransitionVisitedListener listener) {
        if (this.transitionVisitedListeners==null) this.transitionVisitedListeners = new LinkedList();
        this.transitionVisitedListeners.add(listener);
      }

      public boolean removeVisitedListener(ITransitionVisitedListener listener) {
        if (this.transitionVisitedListeners!=null) {
          Iterator it = this.transitionVisitedListeners.iterator();
          for (int i=this.transitionVisitedListeners.size(); i>0; --i) {
            if (listener==it.next()) {
              if (this.transitionVisitedListeners.size()>1) it.remove();
              else this.transitionVisitedListeners = null;
              return true;
            }
          }
        }
        return false;
      }

      public final Automaton.State visit() {
        if (this.transitionVisitedListeners!=null) {
          Iterator it = this.transitionVisitedListeners.iterator();
          for (int i=this.transitionVisitedListeners.size(); i>0; --i)
            ((ITransitionVisitedListener)it.next()).transitionVisited(this);
        }
        return this.toState;
      }

      private final void visit(LinkedSet_State statesToVisit) {
        if (this.transitionVisitedListeners!=null) {
          Iterator it = this.transitionVisitedListeners.iterator();
          for (int i=this.transitionVisitedListeners.size(); i>0; --i)
            ((ITransitionVisitedListener)it.next()).transitionVisited(this);
        }
        statesToVisit.add(this.toState);
      }


      private final Automaton.State visit(char ch) {
        if (this.transitionVisitedListeners!=null) {
          Iterator it = this.transitionVisitedListeners.iterator();
          for (int i=this.transitionVisitedListeners.size(); i>0; --i) {
            ((ITransitionVisitedListener)it.next()).transitionVisited(this,ch);
          }
        }
        return this.toState;
      }

      private final void visit(char ch,LinkedSet_State states) {
        if (this.transitionVisitedListeners!=null) {
          Iterator it = this.transitionVisitedListeners.iterator();
          for (int i=this.transitionVisitedListeners.size(); i>0; --i)
            ((ITransitionVisitedListener)it.next()).transitionVisited(this,ch);
        }
        states.add(this.toState);
      }


      public final ISet_char charSet;
      public final State toState;

      public IProperties properties = null;

      public Transition next = null;

      /**
       * constructs a Transition that can transit with charSet's chars to toState.
       * if charSet==null, the Transition will be an epsilon transition, which means
       * that there are no chars needed to get to toState;  in other words a state that has an
       * epsilon transition can get through this epsilon transition to toState
       * without any char, so that we can say that toState melts into the state.
       */
      protected Transition(IProperties properties,ISet_char charSet,State toState) {
        if (toState==null) throw new IllegalArgumentException("toState==null");

        this.properties = properties;
        this.charSet = charSet;
        this.toState = toState;
      }


      public State getFromState() {
        return State.this;
      }

      public State getToState() {
        return this.toState;
      }

      public ISet_char getCharSet() {
        return this.charSet;
      }

      public String toString() {
        final StringBuffer buffer = new StringBuffer();

        buffer.append(State.this);

        if (this.charSet==null) {
          if (this.properties==null) buffer.append(" --> ");
          else buffer.append(" -").append(this.properties).append(": -> ");
        } else {
          if (this.properties==null) buffer.append(" -").append(this.charSet).append("-> ");
          else buffer.append(" -").append(this.properties).append(':').append(this.charSet).append("-> ");
        }

        buffer.append(this.toState);

        return buffer.toString();
      }
    } // end class Transition


    //////////////////////////////////////////////////////////////////
    ////////////////  S T A T E   B O D Y                   //////////
    //////////////////////////////////////////////////////////////////

    public transient int stateNr;

    {
//      synchronized(Automaton.BITSET) {
//        this.stateNr = Automaton.BITSET.setAClearedBit(0);
//      }
        this.stateNr = Automaton.this.currentStateNr++;

//      if (this.stateNr<10) System.out.println("0"+this.stateNr+" "+Automaton.BITSET+" "+Automaton.BITSET.cardinality());
//      else System.out.println(this.stateNr+" "+Automaton.BITSET+" "+Automaton.BITSET.cardinality());
    }


    public Transition transitions = null;
    public Transition eTransitions = null;

    private transient int isDeterministic = State.TRUE;
    private transient SoftReference nDetInterCharSet = null;

    protected State() {}


//    protected void finalize() throws Throwable {
//      try {
//        super.finalize();
//        synchronized(Automaton.BITSET) {
//          Automaton.BITSET.clearBit(this.stateNr);
//        }
//        if (this.stateNr<10) System.out.println("-0"+this.stateNr+" "+Automaton.BITSET+" "+Automaton.BITSET.cardinality());
//        else System.out.println("-"+this.stateNr+" "+Automaton.BITSET+" "+Automaton.BITSET.cardinality());
//
//      } catch(Throwable t) {
//        throw new RuntimeException(t.getMessage());
//      }
//    }


    protected Automaton parent() {
      return Automaton.this;
    }

    protected Transition addTransition(IProperties properties,ISet_char charSet,State toState) {
      final Transition result = new Transition(properties,charSet,toState);
      this.addTransition(result);
      return result;
    }

    protected void addTransition(Transition trans) {
      if (trans.charSet==null) {
        trans.next = this.eTransitions;
        this.eTransitions = trans;
        Automaton.this.isDeterministic = Automaton.FALSE;
      } else {
        trans.next = this.transitions;
        this.transitions = trans;

        if (this.isDeterministic==State.TRUE) this.isDeterministic = State.UNKNOWN;
        if (Automaton.this.isDeterministic==Automaton.TRUE) Automaton.this.isDeterministic = Automaton.UNKNOWN;
      }

      if (this.changedListeners!=null) {
        final Iterator it = this.changedListeners.iterator();
        for (int i=this.changedListeners.size(); i>0; --i) {
          ((IStateChangedListener)it.next()).transitionAdded(trans);
        }
      }
    }

    protected boolean removeTransition(Transition transition) {
      if (transition.getFromState()!=this) throw new IllegalArgumentException("transition.getFromState()!=this");

      if (transition.charSet==null) {
        for (Transition prevTrans=null, trans=this.eTransitions; trans!=null; prevTrans=trans, trans=trans.next) {
          if (trans==transition) {
            if (prevTrans==null) this.eTransitions = trans.next;
            else prevTrans.next = trans.next;

            if (Automaton.this.isDeterministic==Automaton.FALSE) Automaton.this.isDeterministic = Automaton.UNKNOWN;

            if (this.changedListeners!=null) {
              final Iterator it = this.changedListeners.iterator();
              for (int i=this.changedListeners.size(); i>0; --i) {
                ((IStateChangedListener)it.next()).transitionRemoved(transition);
              }
            }
            return true;
          }
        }
      } else {
        for (Transition prevTrans=null, trans=this.transitions; trans!=null; prevTrans=trans, trans=trans.next) {
          if (trans==transition) {
            if (prevTrans==null) this.transitions = trans.next;
            else prevTrans.next = trans.next;

            if (this.isDeterministic==State.FALSE) this.isDeterministic = State.UNKNOWN;
            if (Automaton.this.isDeterministic==Automaton.FALSE) Automaton.this.isDeterministic = Automaton.UNKNOWN;

            if (this.changedListeners!=null) {
              final Iterator it = this.changedListeners.iterator();
              for (int i=this.changedListeners.size(); i>0; --i) {
                ((IStateChangedListener)it.next()).transitionRemoved(transition);
              }
            }
            return true;
          }
        }
      }
      return false;
    }

    protected void removeAllTransitions() {
      for (Transition trans=this.eTransitions; trans!=null; trans=trans.next) {
        this.removeTransition(trans);
      }
      for (Transition trans=this.transitions; trans!=null; trans=trans.next) {
        this.removeTransition(trans);
      }
    }


    protected void setDeterministic(Boolean isDeterministic) {
      if (isDeterministic==null) { this.isDeterministic = State.UNKNOWN; return; }
      if (isDeterministic.booleanValue()) this.isDeterministic = State.TRUE;
      else this.isDeterministic = State.FALSE;
    }

    // checks whether this.transitions are deterministic
    // this.eTransitions are not checked!
    public final boolean isDeterministic() {
      switch(this.isDeterministic) {
        case State.TRUE : return true;
        case State.FALSE : return false;
        case State.UNKNOWN : {
          if (this.transitions==null) {
            this.isDeterministic = State.TRUE;
            return true;
          }
          final ISet_char charSet = (ISet_char)this.transitions.charSet.clone();
          for (Transition trans=this.transitions.next; trans!=null; trans=trans.next) {
            int oldSize = charSet.size();
            charSet.addAll(trans.charSet);
            int newSize = charSet.size();
            if (newSize-oldSize<trans.charSet.size()) {
              this.isDeterministic = State.FALSE;
              return false;
            }
          }

          this.isDeterministic = State.TRUE;
          return true;
        }

        default :
          throw new Error("Unknown deterministic state: "+this.isDeterministic);
      }
    }

    public final IState next(char ch) {
      this.unVisit();
      if (this.isDeterministic()) {

        for (Transition trans=this.transitions; trans!=null; trans=trans.next) {
          if (trans.charSet.contains(ch)) {
            final Automaton.State toState = trans.visit(ch);

            if (toState.eTransitions==null) {
              if (toState.visitedListeners!=null) {
                final Iterator it = this.visitedListeners.iterator();
                for (int i=this.visitedListeners.size(); i>0; --i)
                  ((IStateVisitedListener)it.next()).stateVisited(toState,ch);
              }
              return toState;
            } else {
              final LinkedSet_State statesToVisit = Automaton.this.newLinkedSet_State(toState);
              for (Wrapper_State w=statesToVisit.elements; w!=null; w=w.next) {
                if (w.state.visitedListeners!=null) {
                  final Iterator it = w.state.visitedListeners.iterator();
                  for (int i=w.state.visitedListeners.size(); i>0; --i)
                    ((IStateVisitedListener)it.next()).stateVisited(w.state,ch);
                }

                for (State.Transition t=w.state.eTransitions; t!=null; t=t.next) {
                  t.visit(statesToVisit);
                }
              }
              return statesToVisit;
            }
          }
        }
        return null;

      } else {

        final LinkedSet_State statesToVisit = Automaton.this.newLinkedSet_State();
        for (Transition trans=this.transitions; trans!=null; trans=trans.next) {
          if (trans.charSet.contains(ch)) {
            trans.visit(ch,statesToVisit);
          }
        }

        for (Wrapper_State w=statesToVisit.elements; w!=null; w=w.next) {
          if (w.state.visitedListeners!=null) {
            final Iterator it = w.state.visitedListeners.iterator();
            for (int i=w.state.visitedListeners.size(); i>0; --i)
              ((IStateVisitedListener)it.next()).stateVisited(w.state,ch);
          }

          for (Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
            trans.visit(statesToVisit);
          }
        }

        switch(statesToVisit.size) {
          case 0 : return null;
          case 1 : return statesToVisit.elements.state;
          default : return statesToVisit;
        }
      }

    }

    protected IState getEClosure() {
      if (this.eTransitions==null) return this;

      final LinkedSet_State eClosure = Automaton.this.newLinkedSet_State(this);
      for (Wrapper_State w=eClosure.elements; w!=null; w=w.next) {
        for(Transition trans=w.state.eTransitions; trans!=null; trans=trans.next)
          eClosure.add(trans.toState);
      }
      switch(eClosure.size) {
        case 1 : return eClosure.elements.state;
        default : return eClosure;
      }
    }

    protected void addEClosure(LinkedSet_State eClosure) {
      eClosure.add(this);

      Wrapper_State w=eClosure.lastElement;

      for(Transition trans=this.eTransitions; trans!=null; trans=trans.next)
        eClosure.add(trans.toState);

      for (w=w.next; w!=null; w=w.next) {
        for(Transition trans=w.state.eTransitions; trans!=null; trans=trans.next)
          eClosure.add(trans.toState);
      }
    }

    /**
     * returns all states that are reachable from this states transitions.
     * Note: this state is only element of the returned array, if it is reachable through one of it's transitions
     */

    public LinkedSet_State getAllReachableStates() {
      final LinkedSet_State states = new LinkedSet_State();

      for(Transition trans=this.eTransitions; trans!=null; trans=trans.next) {
        states.add(trans.toState);
      }

      for(Transition trans=this.transitions; trans!=null; trans=trans.next) {
        if (trans.charSet.isEmpty()==false) states.add(trans.toState);
      }

      for (Wrapper_State w=states.elements; w!=null; w=w.next) {
        for(Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
          states.add(trans.toState);
        }
        for(Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
          if (trans.charSet.isEmpty()==false) states.add(trans.toState);
        }
      }

      return states;
    }

    @FindbugsSuppressWarnings("CN_IDIOM_NO_SUPER_CALL")
    public final Object clone() {
      return Automaton.this.cloneState(this).get(this);
    }

    public String toString() {
      return Automaton.this.automatonNr+".("+String.valueOf(this.stateNr)+')';
    }

  } // end class State



  //////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////

  // should be named IdentityLinkedSet_State
  public class LinkedSet_State implements IState {
/*
    protected abstract class Iterator {
      Wrapper_State current = null;
      protected Automaton.State nextState() {
        if (this.current==null) this.current = LinkedSet_State.this.elements;
        else this.current = this.current.next;
        return (this.current==null) ? null : this.current.state;
      }
    }
*/

    public Wrapper_State elements = null;
    protected Wrapper_State lastElement = null;

    transient int size = 0;
    transient int hashCode = 0;
    transient boolean hashCodeIsValid = true;

    public LinkedSet_State(){}

    public LinkedSet_State(Automaton.State state) {
      this.add(state);
    }

    public boolean add(Automaton.State state) {
      // performance leak
      if (this.size!=0 && this.elements.state.parent()!=state.parent()) throw new IllegalArgumentException("this.elements.state.parent()!=state.parent()");

      if (this.contains(state)) return false;

      if (this.lastElement==null) {
        this.elements = new Wrapper_State(state);
        this.lastElement = this.elements;
      } else {
        this.lastElement.next = new Wrapper_State(state);
        this.lastElement = this.lastElement.next;
      }
      this.hashCodeIsValid = false;
      ++this.size;
      return true;
    }


    public void addAll(LinkedSet_State states) {
      for (Wrapper_State wrapper=states.elements; wrapper!=null; wrapper=wrapper.next) {
        this.add(wrapper.state);
      }
    }

    public void addAll(IState state) {
      if (state instanceof State) this.add((State)state);
      else this.addAll((LinkedSet_State)state);
    }


    public boolean remove(Automaton.State state) {
      // performance leak
      if (this.size!=0 && this.elements.state.parent()!=state.parent()) throw new IllegalArgumentException("this.elements.state.parent()!=state.parent()");

      Wrapper_State prev = null;
      for (Wrapper_State wrapper=this.elements; wrapper!=null; wrapper=wrapper.next) {
        if (wrapper.state==state) {
          if (prev==null) this.elements = wrapper.next;
          else prev.next = wrapper.next;

          if (wrapper==this.lastElement) this.lastElement = prev;

          this.hashCodeIsValid = false;
          --this.size;
          return true;
        }
        prev = wrapper;
      }
      return false;
    }
/*
    int removeAll(LinkedSet_State states) {
      int answer = 0;
      Wrapper_State prev = null;
      for (Wrapper_State wrapper=this.elements; wrapper!=null; wrapper=wrapper.next) {
        if (states.contains(wrapper.state)) {
          if (prev==null) this.elements = wrapper.next;
          else prev.next = wrapper.next;

          if (wrapper==this.lastElement) this.lastElement = prev;
          --this.size;
          if (++answer==states.size()) return answer;
        }
        prev = wrapper;
      }
      return answer;
    }
*/

    public boolean contains(Automaton.State state) {
      // performance leak
      if (this.size!=0 && this.elements.state.parent()!=state.parent()) throw new IllegalArgumentException("this.elements.state.parent()!=state.parent()");

      for (Wrapper_State wrapper=this.elements; wrapper!=null; wrapper=wrapper.next) {
        if (wrapper.state==state) return true;
      }
      return false;
    }

    public void clear() {
      this.elements = null;
      this.lastElement = null;
      this.size = 0;
    }

    public int size() {
      return this.size;
    }

    public boolean isEmpty() {
      return this.size==0;
    }
/*
    public LinkedSet_State.Iterator stateIterator() {
      return new LinkedSet_State.Iterator() {
        Wrapper_State current = null;
        public Automaton.State next() {
          if (this.current==null) this.current = LinkedSet_State.this.elements;
          else this.current = this.current.next;
          return (this.current==null) ? null : this.current.state;
        }
      };
    }

    public LinkedSet_State.Iterator stateIterator(final int offset) {
      return new LinkedSet_State.Iterator() {
        Wrapper_State current = null;
        public Automaton.State next() {
          if (this.current==null) {
            this.current = LinkedSet_State.this.elements;
            try {
              for (int i=offset; i>0; --i) this.current = this.current.next;
            } catch(NullPointerException e) {
              if (this.current!=null) throw e;
            }
          } else this.current = this.current.next;
          return (this.current==null) ? null : this.current.state;
        }
      };
    }
*/
    public boolean equals(Object obj) {
      try {
        return this.equals((LinkedSet_State)obj);
      } catch(ClassCastException e) {
        if ((obj instanceof LinkedSet_State)==false) throw new IllegalArgumentException("obj not instanceof LinkedSet_State");
        throw e;
      }
    }

    public boolean equals(LinkedSet_State set) {
      if (set == null) return false;

      if (this==set) return true;
      try {
        if (this.size!=set.size) return false;
        for (Wrapper_State wrapper=set.elements; wrapper!=null; wrapper=wrapper.next) {
          if (this.contains(wrapper.state)==false) return false;
        }
        return true;
      } catch(NullPointerException e) {
        if (set==null) throw new IllegalArgumentException("set==null");
        throw e;
      }
    }

    public int hashCode() {
      if (this.hashCodeIsValid==false) {
        long hash = 0;
        for (Wrapper_State wrapper=this.elements; wrapper!=null; wrapper=wrapper.next) {
//          hash = ( (hash<<32) + wrapper.state.hashCode() ) % 4294967291L;
          hash+= wrapper.state.hashCode();
        }

        this.hashCode = (int)(hash % 4294967291L);
      }
      return this.hashCode;
    }

    public Object clone() {
      try {
        final LinkedSet_State clone = (LinkedSet_State)super.clone();
        clone.clear();
        clone.addAll(this);
        return clone;
      } catch(CloneNotSupportedException e) {
        throw new Error();
      }
    }

    public String toString() {
      final StringBuffer result = new StringBuffer();
      result.append('(');
      for (Wrapper_State wrapper=this.elements; wrapper!=null; wrapper=wrapper.next) {
        if (wrapper!=this.elements) result.append(", ");
        result.append(wrapper.state.toString());
      }
      result.append(')');
      return result.toString();
    }

    // implementing methods of IState

    public LinkedSet_State getAllReachableStates() {
      // this is very tricky
      Wrapper_State wrapper=this.elements;

      for (int i=this.size; i>0; --i) {
        for (State.Transition trans=wrapper.state.transitions; trans!=null; trans=trans.next) {
          this.add(trans.toState);
        }
        wrapper=wrapper.next;
      }

      for (; wrapper!=null; wrapper=wrapper.next) {
        for (State.Transition trans=wrapper.state.eTransitions; trans!=null; trans=trans.next) {
          this.add(trans.toState);
        }
        for (State.Transition trans=wrapper.state.transitions; trans!=null; trans=trans.next) {
          this.add(trans.toState);
        }
      }

      return this;
    }

    public final IState next(char ch) {
      final LinkedSet_State statesToVisit = Automaton.this.newLinkedSet_State();

      for (Wrapper_State w=this.elements; w!=null; w=w.next) w.state.unVisit();

      for (Wrapper_State w=this.elements; w!=null; w=w.next) {
        if (w.state.isDeterministic()) {
          for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
            if (trans.charSet.contains(ch)) {
              trans.visit(ch,statesToVisit);
              break;
            }
          }
        } else {
          for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
            if (trans.charSet.contains(ch)) trans.visit(ch,statesToVisit);
          }
        }
      }

      for (Wrapper_State w=statesToVisit.elements; w!=null; w=w.next) {
        if (w.state.visitedListeners!=null) {
          final Iterator it = w.state.visitedListeners.iterator();
          for (int i=w.state.visitedListeners.size(); i>0; --i)
            ((IStateVisitedListener)it.next()).stateVisited(w.state,ch);
        }

        for (State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
          trans.visit(statesToVisit);
        }
      }
      switch(statesToVisit.size) {
        case 0 : return null;
        case 1 : return statesToVisit.elements.state;
        default: return statesToVisit;
      }
    }
  } // end class LinkedSet_State


  //////////////////////////////////////////////////////////////////////
  ///////////////// A U T O M A T O N   B O D Y   /////////////////////
  /////////////////////////////////////////////////////////////////////

  protected State startState = null;
  protected LinkedSet_State aStates = new LinkedSet_State();
  protected int isDeterministic = Automaton.TRUE;

  protected int automatonNr = Automaton.currentAutomatonNr++;
  protected int currentStateNr = 0;

  protected abstract LinkedSet_State newLinkedSet_State();
  protected abstract LinkedSet_State newLinkedSet_State(State state);

  protected State createState() {
    return new State();
  }

  protected void setDeterminstic(Boolean isDeterministic) {
    if (isDeterministic==null) { this.isDeterministic = Automaton.UNKNOWN; return; }
    if (isDeterministic.booleanValue()) this.isDeterministic = Automaton.TRUE;
    else this.isDeterministic = Automaton.FALSE;
  }

  protected boolean isDeterministic() {
    /*
    switch(this.isDeterministic) {
      case Automaton.FALSE : return false;
      case Automaton.TRUE : return true;
      case Automaton.UNKNOWN :
        if (this.startState==null || this.isDeterministic(this.startState)) {
          this.isDeterministic = Automaton.TRUE;
          return true;
        } else {
          this.isDeterministic = Automaton.FALSE;
          return false;
        }
    }
    */
    if (this.startState==null || this.isDeterministic(this.startState)) {
      this.isDeterministic = Automaton.TRUE;
      return true;
    } else {
      this.isDeterministic = Automaton.FALSE;
      return false;
    }
  }

  protected boolean isDeterministic(State startState) {
    final LinkedSet_State reachableStates = new LinkedSet_State(startState);
    for (Wrapper_State w=reachableStates.elements; w!=null; w=w.next) {
      if (w.state.eTransitions!=null || w.state.isDeterministic()==false)
        return false;

      for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
        reachableStates.add(trans.toState);
      }
    }
    return true;
  }

  protected State addState() {
    final State result = this.createState();
    this.addState(result);
    return result;
  }

  protected void setStartState(State startState) {
    if (startState==this.startState) return;

    // performance leak
    if (startState!=null) {
     if (startState.parent()!=this) throw new IllegalArgumentException("startState.parent()!=this");
     if (this.aStates.contains(startState)==false)
       throw new IllegalArgumentException("this.states.contains(startState="+startState+")==false");
    }

    final State oldStartState = this.startState;
    this.startState = startState;

    this.isDeterministic = Automaton.UNKNOWN;

    //inform listener
    if (this.listeners!=null) {
      final Iterator it = this.listeners.iterator();
      for (int i=this.listeners.size(); i>0; --i) {
        ((IChangedListener)it.next()).startStateChanged(oldStartState,startState);
      }
    }

  }

  protected State getStartState() {
    return this.startState;
  }

  protected void addState(State state) {
    // performance leak
    if (state.parent()!=this) throw new IllegalArgumentException("state.parent()!=this");
    this.aStates.add(state);

    //inform listener
    if (this.listeners!=null) {
      final Iterator it = this.listeners.iterator();
      for (int i=this.listeners.size(); i>0; --i) {
        ((IChangedListener)it.next()).stateAdded(state);
      }
    }
  }

  protected boolean removeState(State removeState) {
    // performance leak
    if (removeState.parent()!=this) throw new IllegalArgumentException("removeState.parent()!=this");

    if (this.startState==removeState) this.setStartState(null);

    for (Wrapper_State w=this.aStates.elements; w!=null; w=w.next) {
      if (w.state!=removeState) {
        for (State.Transition trans=w.state.transitions; trans!=null; trans = trans.next) {
          if (trans.toState==removeState) w.state.removeTransition(trans);
        }
        for (State.Transition trans=w.state.eTransitions; trans!=null; trans = trans.next) {
          if (trans.toState==removeState) w.state.removeTransition(trans);
        }
      }
    }

    if (this.aStates.remove(removeState)==false) return false;

    //inform listener;
    if (this.listeners!=null) {
      final Iterator it = this.listeners.iterator();
      for (int i=this.listeners.size(); i>0; --i) {
        ((IChangedListener)it.next()).stateRemoved(removeState);
      }
    }

    return true;
  }
/*
  protected int removeStates(LinkedSet_State removeStates) {
    for (Wrapper_State w=this.states.elements; w!=null; w=w.next) {
      if (removeStates.contains(w.state)==false) {
        for (State.Transition trans=w.state.transitions; trans!=null; trans = trans.next) {
          if (removeStates.contains(trans.toState)) w.state.removeTransition(trans);
        }
        for (State.Transition trans=w.state.eTransitions; trans!=null; trans = trans.next) {
          if (removeStates.contains(trans.toState)) w.state.removeTransition(trans);
        }
      }
    }

      //inform listener;

    return this.states.removeAll(removeStates);
  }
*/

  protected void removeUnreachableStates() {
    if (this.startState==null) return;

    LinkedSet_State states = this.startState.getAllReachableStates();
    states.add(this.startState);
    for (Wrapper_State w=this.aStates.elements; w!=null; w=w.next) {
      if (states.contains(w.state)==false) this.removeState(w.state);
    }
  }


  protected void clear() {
    for (Wrapper_State w=this.aStates.elements; w!=null; w=w.next) {
      this.removeState(w.state);
    }
  }

  protected java.util.Map cloneState(State state) {
    final HashMap map = new HashMap();
    final LinkedSet_State states = new LinkedSet_State(state);
    for (Wrapper_State w=states.elements; w!=null; w=w.next) {
      for (State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next)
        states.add(trans.toState);

      for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next)
        states.add(trans.toState);

      map.put(w.state,this.addState());
    }
    for (Wrapper_State w=states.elements; w!=null; w=w.next) {
      State newState = (State)map.get(w.state);
      for (State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
        if (trans.properties==null)
          newState.addTransition(null,null,(State)map.get(trans.toState));
        else
          newState.addTransition((IProperties)trans.properties.clone(),null,(State)map.get(trans.toState));
      }
      for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
        if (trans.properties==null)
          newState.addTransition(null,(ISet_char)trans.charSet.clone(),(State)map.get(trans.toState));
         else
          newState.addTransition((IProperties)trans.properties.clone(),(ISet_char)trans.charSet.clone(),(State)map.get(trans.toState));
      }
    }

    return map;
  }

  protected java.util.Map cloneStates(LinkedSet_State states) {
    final HashMap map = new HashMap();
/*
    final LinkedSet_State XXX = new LinkedSet_State();
    XXX.addAll(states); // critical beacuse xxx has another parent than states
    for (Wrapper_State w=XXX.elements; w!=null; w=w.next) {
      for (State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
        XXX.add(trans.toState);
      }
      for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
        XXX.add(trans.toState);
      }

      map.put(w.state,this.addState());
    }
*/

    for (Wrapper_State w=states.elements; w!=null; w=w.next) {
      map.put(w.state,this.addState());
    }

    for (Wrapper_State w=states.elements; w!=null; w=w.next) {
      State newState = (State)map.get(w.state);
      for (State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
        if (trans.properties==null)
          newState.addTransition(null,null,(State)map.get(trans.toState));
         else
          newState.addTransition((IProperties)trans.properties.clone(),null,(State)map.get(trans.toState));
      }
      for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
        if (trans.properties==null)
          newState.addTransition(null,(ISet_char)trans.charSet.clone(),(State)map.get(trans.toState));
         else
           newState.addTransition((IProperties)trans.properties.clone(),(ISet_char)trans.charSet.clone(),(State)map.get(trans.toState));
      }
    }

    return map;
  }


  public String toString() {
    final StringBuffer buffer = new StringBuffer();

    for (Wrapper_State w=this.aStates.elements; w!=null; w=w.next) {
        buffer.append("  \n").append(w.state);

      if (w.state==this.startState) buffer.append('+');

      for (State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
        buffer.append("    \n  -");
        if (trans.properties!=null) buffer.append(trans.properties).append(": ");
        buffer.append("-> ").append(trans.toState);
      }

      for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
        buffer.append("    \n  -");
        if (trans.properties!=null) buffer.append(trans.properties).append(": ");
        buffer.append(trans.charSet).append("-> ").append(trans.toState);
      }
    }

    return buffer.toString();
  }



  protected Object clone() {
    try {
      Automaton clone = (Automaton)super.clone();
      clone.automatonNr = Automaton.currentAutomatonNr++;
      clone.currentStateNr = 0;
      clone.startState = null;
      clone.listeners = null;
      clone.aStates = clone.newLinkedSet_State();

      final java.util.Map map = clone.cloneStates(this.aStates);

      final Set keys = map.keySet();
      final Iterator it = keys.iterator();
      for (int i=keys.size(); i>0; --i) {
        State oldState = (State)it.next();
        State newState = (State)map.get(oldState);
        newState.stateNr = oldState.stateNr;
        if (clone.currentStateNr<=newState.stateNr)
          clone.currentStateNr = newState.stateNr+1;
      }

      if (this.startState!=null)
        clone.setStartState((State)map.get(this.startState));

      return clone;
    } catch(CloneNotSupportedException e) {
      throw new Error();
    }
  }



}
