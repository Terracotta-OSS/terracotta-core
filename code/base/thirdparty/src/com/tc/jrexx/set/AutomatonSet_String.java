/*
* 01/07/2003 - 15:19:32
*
* AutomatonSet_String.java -
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

public class AutomatonSet_String extends Automaton {
  protected static final ISet_char FULLSET = new com.tc.jrexx.set.CharSet();
  static {
    AutomatonSet_String.FULLSET.complement();
  }

  protected static class SProperties extends HashSet implements IProperties {

    public SProperties() {
    }

    public boolean containsAll(IProperties p) {
      //performance leak
      if ((p instanceof SProperties)==false) throw new IllegalArgumentException("(p instanceof SProperties)==false");
      return super.containsAll((SProperties)p);
    }

    public void addAll(IProperties p) {
      //performance leak
      if ((p instanceof SProperties)==false) throw new IllegalArgumentException("(p instanceof SProperties)==false");
      super.addAll((SProperties)p);
    }

    public void retainAll(IProperties p) {
      //performance leak
      if ((p instanceof SProperties)==false) throw new IllegalArgumentException("(p instanceof SProperties)==false");
      super.retainAll((SProperties)p);
    }

    public void removeAll(IProperties p) {
      //performance leak
      if ((p instanceof SProperties)==false) throw new IllegalArgumentException("(p instanceof SProperties)==false");
      super.removeAll((SProperties)p);
    }

    public Object clone() {
      return super.clone();
    }

  }

  public interface ISStateChangedListener extends IStateChangedListener {
    public void isFinalChanged(SState state,boolean isFinal);
  }


  public interface ISState extends Automaton.IState {
    public boolean isFinal();
  }

  public class SState extends Automaton.State implements ISState {

    public boolean isFinal;

    public SState(boolean isFinal) {
      this.isFinal = isFinal;
    }

    protected Automaton parent() {
      return AutomatonSet_String.this;
    }

    protected void setDeterministic(Boolean isDeterministic) {
      super.setDeterministic(isDeterministic);
    }

    public boolean isFinal() {
      return this.isFinal;
    }

    protected void setFinal(boolean isFinal) {
      if (this.isFinal==isFinal) return;
      this.isFinal = isFinal;
      //inform listener
      if (this.changedListeners!=null) {
        final Iterator it = this.changedListeners.iterator();
        for (int i=this.changedListeners.size(); i>0; --i) {
          ((ISStateChangedListener)it.next()).isFinalChanged(this,isFinal);
        }
      }
    }

/*
    protected void addTransition(State.Transition trans) {
      super.addTransition(trans);
    }
*/
    protected Transition addTransition(IProperties properties,ISet_char charSet,State toState) {
      // performance leak
      if (properties!=null && (properties instanceof SProperties)==false) throw new IllegalArgumentException("(properties instanceof SProperties)==false");
      if ((toState instanceof SState)==false) throw new IllegalArgumentException("(toState("+toState+") instanceof SState)==false");

      return super.addTransition(properties,charSet,toState);
    }

    protected boolean removeTransition(State.Transition trans) {
      return super.removeTransition(trans);
      /*
      if(super.removeTransition(trans)) {
        return true;
      }
      return false;
      */
    }

    protected void removeAllTransitions() {
      super.removeAllTransitions();
    }

    protected IState getEClosure() {
      return super.getEClosure();
    }

    public String toString() {
      if (this.isFinal) return AutomatonSet_String.this.automatonNr+".["+String.valueOf(this.stateNr)+']';
      else return super.toString();
    }

  }

  protected class LinkedSet_SState extends Automaton.LinkedSet_State implements ISState {

    protected LinkedSet_SState() {
      super();
    }

    protected LinkedSet_SState(SState state) {
      super(state);
    }

    public boolean isFinal() {
      for (Wrapper_State w=this.elements; w!=null; w=w.next) {
        if ( ((SState)w.state).isFinal ) return true;
      }
      return false;
    }

    public String toString() {
      final StringBuffer result = new StringBuffer();
      result.append( this.isFinal() ? '[' : '(' );
      for (Wrapper_State wrapper=this.elements; wrapper!=null; wrapper=wrapper.next) {
        if (wrapper!=this.elements) result.append(", ");
        result.append(wrapper.state.toString());
      }
      result.append( this.isFinal() ? ']' : ')' );
      return result.toString();
    }

  }


  protected final ISet_char fullSet;
//  protected boolean isMinimized = true;

  protected AutomatonSet_String(ISet_char fullSet) {
    super();
    this.fullSet = fullSet;
  }

  protected void addChangedListener(Automaton.IChangedListener listener) {
    super.addChangedListener(listener);
  }

  protected boolean removeChangedListener(Automaton.IChangedListener listener) {
    return super.removeChangedListener(listener);
  }

  protected boolean isDeterministic() {
    return super.isDeterministic();
  }

  protected Automaton.State getStartState() {
    return this.startState;
  }


  protected AutomatonSet_String() {
    super();
    this.fullSet = AutomatonSet_String.FULLSET;
  }

  protected LinkedSet_State newLinkedSet_State() {
    return new LinkedSet_SState();
  }

  protected LinkedSet_State newLinkedSet_State(State state) {
    return new LinkedSet_SState((SState)state);
  }

  protected State createState() {
    return new SState(false);
  }

  protected SState createState(boolean isFinal) {
    return new SState(isFinal);
  }

  protected SState addState(boolean isFinal) {
    final SState result = this.createState(isFinal);
    this.addState(result);
    return result;
  }

  protected SState addState(boolean isFinal,int stateNr) {
    this.currentStateNr = stateNr;
    return this.addState(isFinal);
  }

  protected boolean removeState(State removeState) {
    return super.removeState(removeState);
  }

  protected void clear() {
    super.clear();
  }

  protected void setDeterministic(Boolean isDeterministic) {
    super.setDeterminstic(isDeterministic);
  }

  protected void setStartState(State startState) {
    super.setStartState(startState);
  }

  protected LinkedSet_State getStates() {
    return this.aStates;
  }


  protected SState complement(SState state) {
    if (state==null) {
      SState totalFinalState = this.addState(true);
      totalFinalState.addTransition(null,(ISet_char)this.fullSet.clone(),totalFinalState);
      return totalFinalState;
    }

    if (this.isDeterministic(state)==false) {
      // remove all properties
      LinkedSet_State states = new LinkedSet_State(state);
      for (Wrapper_State w=states.elements; w!=null; w=w.next) {
        for (State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
          states.add(trans.toState);
          trans.properties = null;
        }
        for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
          states.add(trans.toState);
          trans.properties = null;
        }
      }

      state = this.makeDeterministic(state);
    }


    SState totalFinalState = null;

    LinkedSet_State reachableStates = new LinkedSet_State(state);
    for (Wrapper_State w=reachableStates.elements; w!=null; w=w.next) {
      ISet_char charSet = (ISet_char)this.fullSet.clone();
      for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
        reachableStates.add(trans.toState);
        charSet.removeAll(trans.charSet);
      }

      SState sstate = (SState)w.state;
      if (charSet.isEmpty()==false) {
        if (totalFinalState==null) {
          totalFinalState = this.addState(true);
          totalFinalState.addTransition(null,(ISet_char)this.fullSet.clone(),totalFinalState);
        }

        sstate.addTransition(null,charSet,totalFinalState);
      }
      sstate.setFinal(!sstate.isFinal);
    }

    return state;
  }

  protected SState optional(SState state) {
    if (state.isFinal) return state;
    final SState newState = this.addState(true);
    newState.addTransition(null,null,state);
    return newState;
  }

  protected SState concat(SState state_A,SState state_B) {
    final LinkedSet_State states = new LinkedSet_State(state_A);
    for (Wrapper_State w = states.elements; w!=null; w=w.next) {
      for (State.Transition trans = w.state.eTransitions; trans!=null; trans=trans.next)
        states.add(trans.toState);

      for (State.Transition trans = w.state.transitions; trans!=null; trans=trans.next)
        states.add(trans.toState);

      SState sState = (SState)w.state;
      if (sState.isFinal) {
        sState.setFinal(false);
        sState.addTransition(null,null,state_B);
      }
    }
    return state_A;
  }

  protected SState repeat(SState element,int minTimes,int maxTimes) {
    SState startState = element;

    if (minTimes==0) {
      startState = this.optional(element);
      minTimes = 1;
    } else {
      for (int i=minTimes-1; i>0; --i) {
        SState newState = (SState)element.clone();
        startState = (SState)this.concat(newState,startState);
      }
    }

    if (maxTimes==0) {
      final LinkedSet_State states = new LinkedSet_State(element);

      for (Wrapper_State w=states.elements; w!=null; w=w.next) {
        for (Automaton.State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next)
          states.add(trans.toState);

        for (Automaton.State.Transition trans=w.state.transitions; trans!=null; trans=trans.next)
          states.add(trans.toState);

        if ( ((SState)w.state).isFinal) ((SState)w.state).addTransition(null,null,element);
      }
    } else {
      for (int i=maxTimes-minTimes; i>0; --i) {
        SState newState = (SState)element.clone();

        LinkedSet_State states = element.getAllReachableStates();
        states.add(element);

        for (Wrapper_State w=states.elements; w!=null; w=w.next) {
          if ( ((SState)w.state).isFinal )
            ((SState)w.state).addTransition(null,null,newState);
        }

        element = newState;
      }
    }

    return startState;
  }

  protected SState union(SState state_A,SState state_B) {
    final SState newState = this.addState(false);
    newState.addTransition(null,null,state_A);
    newState.addTransition(null,null,state_B);
    return newState;
  }

  protected SState intersect(SState state_A,SState state_B) {
    // A & B = !(!A + !B)
    return
      this.complement(
        this.union(
          this.complement(state_A),
          this.complement(state_B)
        )
      );
  }

  protected SState minus(SState state_A,SState state_B) {
    // A \ B = A & !B = !(!A + !!B) = !(!A + B)
    return
      this.complement(
        this.union(
          this.complement(state_A),
          state_B
        )
      );
  }


  protected void addAll(SState state) {
    if (this.startState==null) this.setStartState(state);
    else this.setStartState(this.union((SState)this.startState,state));
  }

  protected void retainAll(SState state) {
    if (this.startState==null) return;
    this.setStartState(this.intersect((SState)this.startState,state));
  }

  protected void removeAll(SState state) {
    if (this.startState==null) return;
    this.setStartState(this.minus((SState)this.startState,state));
  }

  protected void concatAll(SState state) {
    if (this.startState==null) return;
    this.setStartState(this.concat((SState)this.startState,state));
  }


  protected void removeUselessStates() {
    if (5==6) {
      this.removeUnreachableStates();
      return;
    }
    final LinkedSet_State usefullStates = new LinkedSet_State();
    if (this.startState!=null) {
      final LinkedSet_State uselessStates = this.startState.getAllReachableStates();
      uselessStates.add(this.startState);
      for (Wrapper_State w=uselessStates.elements; w!=null; w=w.next) {
        if (((SState)w.state).isFinal) {
          if (uselessStates.remove(w.state)==false) throw new Error();
/*
          if (prev==null) uselessStates.elements = w.next;
          else prev.next = w.next;
*/
          if (usefullStates.add(w.state)==false) throw new Error();
        }
      }
//System.out.println(uselessStates);
      for(boolean flag=true; flag;) {
        flag = false;
        loop: for (Wrapper_State w=uselessStates.elements; w!=null; w=w.next) {
          for (State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
            if (usefullStates.contains(trans.toState)) {
              if (uselessStates.remove(w.state)==false) throw new Error();
              if (usefullStates.add(w.state)==false) throw new Error();
              flag = true;
              continue loop;
            }
          }
          for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
            if (trans.charSet.isEmpty()==false && usefullStates.contains(trans.toState)) {
              if (uselessStates.remove(w.state)==false) throw new Error();
              if (usefullStates.add(w.state)==false) throw new Error();
              flag = true;
              continue loop;
            }
          }
        }
      }
    }

    for (Wrapper_State w=this.aStates.elements; w!=null; w=w.next) {
      if (usefullStates.contains(w.state)==false) {
        if (this.removeState(w.state)==false) throw new Error();
//        System.out.println("####"+w.state.stateNr+"####");
      }
    }
  }

  protected void complement() {
    this.setStartState( this.complement( (SState)this.startState) );
    this.removeUselessStates();
  }

  protected void addAll(AutomatonSet_String automaton) {
    if (automaton.startState==null) return;

//    final LinkedSet_State reachableStates = automaton.startState.getAllReachableStates();
//    reachableStates.addAll(automaton.startState);
//    java.util.Map map = this.cloneStates(reachableStates);
    java.util.Map map = this.cloneState(automaton.startState);
    this.addAll((SState)map.get(automaton.startState));
  }

  protected void retainAll(AutomatonSet_String automaton) {
    if (automaton.startState==null) return;

//    final LinkedSet_State reachableStates = automaton.startState.getAllReachableStates();
//    reachableStates.addAll(automaton.startState);
//    java.util.Map map = this.cloneStates(reachableStates);
    java.util.Map map = this.cloneState(automaton.startState);
    this.retainAll((SState)map.get(automaton.startState));
    this.removeUselessStates();
  }

  protected void removeAll(AutomatonSet_String automaton) {
    if (automaton.startState==null) return;

//    final LinkedSet_State reachableStates = automaton.startState.getAllReachableStates();
//    reachableStates.addAll(automaton.startState);
//    java.util.Map map = this.cloneStates(reachableStates);
    java.util.Map map = this.cloneState(automaton.startState);
    this.removeAll((SState)map.get(automaton.startState));
    this.removeUselessStates();
  }

  protected void concatAll(AutomatonSet_String automaton) {
    if (automaton.startState==null) return;

//    java.util.Map map = this.cloneStates(automaton.startState.getAllReachableStates());
    java.util.Map map = this.cloneState(automaton.startState);
    this.concatAll((SState)map.get(automaton.startState));
  }

  protected Map cloneState(State state) {
    final Map map = super.cloneState(state);
    final Set keys = map.keySet();
    final Iterator it = keys.iterator();
    for (int i=keys.size(); i>0; --i) {
      SState oldState = (SState)it.next();
      SState newState = (SState)map.get(oldState);
      newState.setFinal(oldState.isFinal);
    }
    return map;
  }

  protected Map cloneStates(LinkedSet_State states) {
    final Map map = super.cloneStates(states);
    final Set keys = map.keySet();
    final Iterator it = keys.iterator();
    for (int i=keys.size(); i>0; --i) {
      SState oldState = (SState)it.next();
      SState newState = (SState)map.get(oldState);
      newState.setFinal(oldState.isFinal);
    }
    return map;
  }

  protected Object clone() {
    return super.clone();
  }


  private static class Transition {
    final ISet_char charSet;
    final EClosure toEClosure;
    IProperties properties = null;
    Transition next = null;

    Transition(IProperties properties,ISet_char charSet,EClosure toEClosure) {
      this.properties = properties;
      this.charSet = charSet;
      this.toEClosure = toEClosure;
    }
  }


  class EClosure {
    final LinkedSet_SState states;
    Transition eTransitions = null;
    Transition transitions = null;


    SState state = null;

    EClosure(LinkedSet_SState eStates) {
      this.states = eStates;
    }
/*
    EClosure(SState state) {
      this.state = state;
    }
*/
    Transition addTransition(IProperties properties,ISet_char charSet,EClosure toEClosure) {
      Transition newTrans = new Transition(properties,charSet,toEClosure);
      newTrans.next = this.transitions;
      this.transitions = newTrans;
      return newTrans;
    }

    boolean removeTransition(Transition transition) {
      for (Transition prevTrans=null, trans=this.transitions; trans!=null; prevTrans=trans, trans=trans.next) {
        if (trans==transition) {
          if (prevTrans==null) this.transitions = trans.next;
          else prevTrans.next = trans.next;

          return true;
        }
      }
      return false;
    }

    public boolean equals(Object obj) {
      return this.states.equals( ((EClosure)obj).states );
    }
    public int hashCode() {
      return this.states.hashCode();
    }

  }

  class EClosureSet {
    final EClosure eClosure;
    EClosureSet next = null;

    EClosureSet(EClosure eClosure) {
      this.eClosure = eClosure;
    }

    boolean add(EClosure eClosure) {
      EClosureSet prev = null;

      for (EClosureSet eCS=this; eCS!=null; prev=eCS,eCS=eCS.next) {
        if (eCS.eClosure==eClosure) return false;
      }

      prev.next = new EClosureSet(eClosure);
      return true;
    }
  }

  protected SState makeDeterministic(SState state) {
    //performance leak
    if ((state instanceof SState)==false) throw new IllegalArgumentException("state instanceof SState)==false");
    if (AutomatonSet_String.this.isDeterministic(state)) return state;

    final HashMap eStates2EClosure = new HashMap();


    IState istate = state.getEClosure();
    LinkedSet_SState startEStates = (istate instanceof SState)
                                    ? (LinkedSet_SState)this.newLinkedSet_State((SState)istate)
                                    : (LinkedSet_SState)istate;

    EClosure startEClosure = new EClosure(startEStates);
    eStates2EClosure.put(startEStates,startEClosure);

    final EClosureSet eClosureSet = new EClosureSet(startEClosure);
    for (EClosureSet eCS=eClosureSet; eCS!=null; eCS=eCS.next) {
      final EClosure eClosure = eCS.eClosure;

      for (Wrapper_State w=eClosure.states.elements; w!=null; w=w.next) {
        loop: for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
          ISet_char trans_charSet = trans.charSet;
          istate = ((SState)trans.toState).getEClosure();
          LinkedSet_SState trans_toEStates = (istate instanceof SState)
                                             ? (LinkedSet_SState)this.newLinkedSet_State(trans.toState)
                                             : (LinkedSet_SState)istate;

          inner: for (Transition newTrans=eClosure.transitions; newTrans!=null; newTrans=newTrans.next) {
            ISet_char intersection = (ISet_char)newTrans.charSet.clone();
            intersection.retainAll(trans_charSet);

            if (intersection.isEmpty()==false) {

                if (trans_toEStates.equals(newTrans.toEClosure.states)==false) {
                  if (newTrans.charSet.size()==intersection.size())
                    eClosure.removeTransition(newTrans);
                  else
                    newTrans.charSet.removeAll(intersection);

                  /////////////////////////

                  LinkedSet_SState tmpEStates = ((LinkedSet_SState)trans_toEStates.clone());
                  tmpEStates.addAll(newTrans.toEClosure.states);

                  EClosure newToEClosure = (EClosure)eStates2EClosure.get(tmpEStates);
                  if (newToEClosure==null) {
                    newToEClosure = new EClosure(tmpEStates);
                    eStates2EClosure.put(tmpEStates,newToEClosure);
                  }

                  if (trans.properties==null) eClosure.addTransition(null,intersection,newToEClosure);
                  else eClosure.addTransition( (IProperties)trans.properties.clone(),intersection,newToEClosure);
                }

                if (trans_charSet.size()==intersection.size()) continue loop;

                if (trans_charSet==trans.charSet) trans_charSet = (ISet_char)trans.charSet.clone();
                trans_charSet.removeAll(intersection);
            }
          }

          if (trans_charSet.isEmpty()==false) {
            EClosure toEClosure = (EClosure)eStates2EClosure.get(trans_toEStates);
            if (toEClosure!=null) {
              for (Transition newTrans=eClosure.transitions; newTrans!=null; newTrans=newTrans.next) {
                if (newTrans.toEClosure==toEClosure) {
                  if (newTrans.properties==null && trans.properties==null
                      || newTrans.properties!=null && newTrans.properties.equals(trans.properties)) {
                    newTrans.charSet.addAll(trans.charSet);
                    continue loop;
                  }
                }
              }
            } else {
              toEClosure = new EClosure(trans_toEStates);
              eStates2EClosure.put(trans_toEStates,toEClosure);
            }

            if (trans_charSet==trans.charSet) trans_charSet = (ISet_char)trans.charSet.clone();
            if (trans.properties==null) eClosure.addTransition(null,trans_charSet,toEClosure);
            else {
              eClosure.addTransition((IProperties)trans.properties.clone(),trans_charSet,toEClosure);
            }
          }
        }
      }

      if (eClosure.state==null) eClosure.state = this.addState(eClosure.states.isFinal());
      for (Transition trans=eClosure.transitions; trans!=null; trans=trans.next) {
        if (trans.toEClosure.state==null)
          trans.toEClosure.state = this.addState(trans.toEClosure.states.isFinal());

        State.Transition newTrans = eClosure.state.addTransition(trans.properties,trans.charSet,trans.toEClosure.state);

        eClosureSet.add(trans.toEClosure);
      }

    }

    this.isDeterministic = Automaton.UNKNOWN;
    return startEClosure.state;
  }


  protected void minimize() {
    if (this.startState==null) return;
    SState state = (SState)this.startState;

    int states = this.aStates.size();
    this.setStartState(this.minimize(state));
//    this.setStartState(this.makeDeterministic(state));

    this.removeUnreachableStates();
    if (this.aStates.size()>states)
      throw new Error("more states("+this.aStates.size()+") after minimzing than before ("+states+")");
  }


  private static class Tupel {
    final SState a;
    final SState b;
    final int hashCode;

    Tupel next = null;

    Tupel(SState a,SState b) {
      if (a==b) throw new Error("a==b");
      this.a = a;
      this.b = b;

      this.hashCode = (int)((((long)a.hashCode()) + ((long)b.hashCode())) % 4294967291L);
    }

    public boolean equals(Object obj) {
      if (this==obj) return true;

      final Tupel tupel = (Tupel)obj;
      if (this.a!=tupel.a && this.a!=tupel.b) return false;
      if (this.b!=tupel.a && this.b!=tupel.b) return false;
      return true;
    }

    public int hashCode() {
      return this.hashCode;
    }
  }

  protected SState minimize(SState state) {


//System.out.println("states before makeDeterministic: "+state.getAllReachableStates().size());
    SState newStartState = this.makeDeterministic(state);
//System.out.println("states after  makeDeterministic: "+newStartState.getAllReachableStates().size());
if (this.aStates.contains(newStartState)==false) throw new Error("this.states.contains(newStartState)==false");

    LinkedSet_State states = newStartState.getAllReachableStates();
    states.add(newStartState);

    final SState totalState = this.addState(false);
    states.add(totalState);
//System.out.println("totalState: "+totalState);

    HashSet tupelList_ne = new HashSet();
    Tupel tupelList = null;
    for (Wrapper_State w1=states.elements; w1!=null; w1=w1.next) {
      ISet_char rest = (ISet_char)this.fullSet.clone();
      for (State.Transition trans=w1.state.transitions; trans!=null; trans=trans.next) {
        rest.removeAll(trans.charSet);
      }
      if (rest.isEmpty()==false) ((SState)w1.state).addTransition(null,rest,totalState);

      for (Wrapper_State w2=w1.next; w2!=null; w2=w2.next) {
        Tupel tupel = new Tupel((SState)w1.state,(SState)w2.state);
        if (tupel.a.isFinal ^ tupel.b.isFinal) tupelList_ne.add(tupel);
        else {
          tupel.next = tupelList;
          tupelList = tupel;
        }
      }
    }

//System.out.println("++++++++++++++++++++++++++++++++++++++");
//for (Tupel tupel=tupelList; tupel!=null; tupel=tupel.next) {
//  System.out.println(tupel.a +"=="+tupel.b );
//}
//Iterator _it = tupelList_ne.iterator();
//while (_it.hasNext() ) {
//  Tupel t = (Tupel)_it.next();
//  System.out.println( t.a+"!="+t.b );
//}

    boolean flag = true;
    while(flag) {
      flag = false;
      loop: for (Tupel tupel=tupelList,prev=null; tupel!=null; tupel=tupel.next) {
        for (State.Transition trans_a=tupel.a.transitions; trans_a!=null; trans_a=trans_a.next) {
          for (State.Transition trans_b=tupel.b.transitions; trans_b!=null; trans_b=trans_b.next) {
            if (trans_a.toState!=trans_b.toState) {
              Tupel newTupel = new Tupel( (SState)trans_a.toState,(SState)trans_b.toState);
              if (tupelList_ne.contains(newTupel)) {
                ISet_char intersection = (ISet_char)trans_a.charSet.clone();
                intersection.retainAll(trans_b.charSet);
                if (intersection.isEmpty()==false) {
                  if (prev==null) tupelList = tupel.next;
                  else prev.next = tupel.next;

                  tupelList_ne.add(tupel);

                  flag = true;
                  continue loop;
                }
              }
            }
          }
        }
        prev = tupel;
      }
    }

//System.out.println("#############################");
//for (Tupel tupel=tupelList; tupel!=null; tupel=tupel.next) {
//  System.out.println(tupel.a.stateNr +"=="+tupel.b.stateNr );
//}
// _it = tupelList_ne.iterator();
//while (_it.hasNext() ) {
//  Tupel t = (Tupel)_it.next();
//  System.out.println( t.a.stateNr+"!="+t.b.stateNr );
//}

    //should be IdentityMap
    final HashMap map = new HashMap();
    for(Tupel tupel=tupelList; tupel!=null; tupel=tupel.next) {
      SState eqState = (SState)map.get(tupel.a);
      if (eqState!=null) map.put(tupel.b,eqState);
      else {
        eqState = (SState)map.get(tupel.b);
        if (eqState!=null) map.put(tupel.a,eqState);
        else if (tupel.b!=totalState) map.put(tupel.a,tupel.b);
        else map.put(tupel.b,tupel.a);
      }
    }

//System.out.println("***********************************");
//Iterator it_ = map.keySet().iterator();
//while (it_.hasNext()) {
//  State key = (State)it_.next();
//  System.out.println(key.stateNr+"="+((State)map.get(key)).stateNr);
//}

    this.removeState(totalState);

    for (Wrapper_State w=states.elements; w!=null; w=w.next) {
      SState newState = (SState)map.get(w.state);
      if (newState==null) {
        for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
          SState newToState = (SState)map.get(trans.toState);
          if (newToState!=null) {
            ((SState)w.state).removeTransition(trans);

            for (State.Transition tmp=w.state.transitions; tmp!=null; tmp=tmp.next) {
              if (tmp.toState==newToState) {
                if (tmp.properties==null && trans.properties==null
                    || tmp.properties!=null && tmp.properties.equals(trans.properties)) {
                  ((SState)w.state).removeTransition(tmp);
                  trans.charSet.addAll(tmp.charSet);
                  break;
                }
              }
            }

            ((SState)w.state).addTransition(trans.properties,trans.charSet,newToState);
          }
        }
      } else {
        loop: for (State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
          SState newToState = (SState)map.get(trans.toState);
          if (newToState==null) newToState = (SState)trans.toState;

          ((SState)w.state).removeTransition(trans);

          for (State.Transition tmp=newState.transitions; tmp!=null; tmp=tmp.next) {
            if (tmp.toState==newToState) {
              if (tmp.properties==null && trans.properties==null
                  || tmp.properties!=null && tmp.properties.equals(trans.properties)) {
                continue loop;
              }
            }
          }

          newState.addTransition(trans.properties,trans.charSet,newToState);
        }
      }
    }

    Iterator it = map.keySet().iterator();
    for (int i=map.size(); i>0; --i) this.removeState( (SState)it.next() );

    SState newNewStartState = (SState)map.get(newStartState);
    if (newNewStartState!=null) return newNewStartState;
    return newStartState;
  }

}