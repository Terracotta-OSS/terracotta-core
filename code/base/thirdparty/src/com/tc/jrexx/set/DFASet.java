package com.tc.jrexx.set;

/*
* 01/07/2003 - 15:19:32
*
* Automaton.java -
* Copyright (C) 2003 Buero fuer Softwarearchitektur GbR
InputStream* ralf.meyer@karneim.com
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
import java.io.*;
import java.util.*;

/**
 * DFASet is an immutable Set of strings based on a minimized deterministic automaton (DFA).
 * @author Ralf Meyer
 */
public class DFASet {

  protected static class State {
    protected static class Transition {
      protected final CharSet charSet;
      protected final int toState;

      protected Transition(CharSet charSet,int toState) {
        this.charSet = charSet;
        this.toState = toState;
      }
    }

    protected final boolean isFinal;
    protected final Transition[] transitions;

    protected State(boolean isFinal,Transition[] transitions) {
      this.isFinal = isFinal;
      this.transitions = transitions;
    }
  }

  protected final State[] states;
  protected final Integer startState;

  protected DFASet(State[] states,Integer startState) {
    this.states = states;
    this.startState = startState;
  }

  public DFASet(FSAData automaton) {
    if (automaton==null) throw new IllegalArgumentException("automaton==null");

    HashMap map = new HashMap();

    State[] newStates =
      new State[automaton.states==null ? 0 : automaton.states.length];

    for (int i=0; i<newStates.length; ++i) {
      FSAData.State state = automaton.states[i];
      if (state==null) throw new IllegalArgumentException((i+1)+". state of automaton is null");

      State.Transition[] newTransitions =
        new State.Transition[state.transitions==null ? 0 : state.transitions.length];

      for (int t=0; t<newTransitions.length; ++t) {
        FSAData.State.Transition trans = state.transitions[t];
        if (trans==null) throw new IllegalArgumentException((t+1)+". transition of state "+state.number+" is null");
        if (trans.charSet==null) throw new IllegalArgumentException("charSet of "+(t+1)+". transition of state "+state.number+" is null");

        CharSet newCharSet = (CharSet)map.get(trans.charSet);
        if (newCharSet==null) {
          newCharSet = new CharSet(trans.charSet);
          map.put(trans.charSet,newCharSet);
        }

        int toStateNr = 0;
        try {
          while(automaton.states[toStateNr].number!=trans.toStateNumber) ++toStateNr;
        } catch(ArrayIndexOutOfBoundsException e) {
          throw new IllegalArgumentException(
            "toState "+trans.toStateNumber+" of "+(t+1)+". transition of state "+state.number+" does not exist"
          );
        }

        newTransitions[t] = new State.Transition(newCharSet,toStateNr);
      }

      newStates[i] = new State(state.isFinal,newTransitions);
    }

    this.states = newStates;

    if (automaton.startStateNumber==null) this.startState = null;
    else {
      int automatonStartStateNr = automaton.startStateNumber.intValue();
      int startStateNr = 0;
      try {
        while(automaton.states[startStateNr].number!=automatonStartStateNr) ++startStateNr;
      } catch(ArrayIndexOutOfBoundsException e) {
        throw new IllegalArgumentException(
          "startState "+automaton.startStateNumber+" does not exist"
        );
      }
      this.startState = Integer.valueOf(startStateNr);
    }
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


  public DFASet(InputStream dfaDataStream) throws IOException,ClassNotFoundException {
    this(DFASet.toFSAData(new ObjectInputStream(dfaDataStream).readObject()));
  }

  public boolean contains(char[] chars) {
    return this.contains(chars,0,chars.length);
  }

  public boolean contains(char[] chars,int offset) {
    return this.contains(chars,offset,chars.length-offset);
  }

  public boolean contains(char[] chars,int offset,int length) {
    if (this.startState==null) return false;
    State state = this.states[this.startState.intValue()];

    loop: for (;length>0; ++offset, --length) {
      for (int i=0; i<state.transitions.length; ++i) {
        if (state.transitions[i].charSet.contains(chars[offset])) {
          state = this.states[state.transitions[i].toState];
          continue loop;
        }
      }
      return false;
    }

    return state.isFinal;
  }

  public boolean contains(String s) {
    return this.contains(s,0,s.length());
  }

  public boolean contains(String s,int offset) {
    return this.contains(s,offset,s.length()-offset);
  }

  public boolean contains(String s,int offset,int length) {
    if (this.startState==null) return false;
    State state = this.states[this.startState.intValue()];

    loop: for (;length>0; ++offset, --length) {
      for (int i=0; i<state.transitions.length; ++i) {
        if (state.transitions[i].charSet.contains(s.charAt(offset))) {
          state = this.states[state.transitions[i].toState];
          continue loop;
        }
      }
      return false;
    }

    return state.isFinal;
  }


  public boolean contains(java.io.Reader in) throws java.io.IOException {
    if (this.startState==null) return false;
    State state = this.states[this.startState.intValue()];

    loop: for (int ch=in.read(); ch!=-1; ch=in.read()) {
      for (int i=0; i<state.transitions.length; ++i) {
        if (state.transitions[i].charSet.contains((char)ch)) {
          state = this.states[state.transitions[i].toState];
          continue loop;
        }
      }
      return false;
    }

    return state.isFinal;
  }

}