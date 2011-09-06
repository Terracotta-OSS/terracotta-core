/*
* 01/07/2003 - 15:19:32
*
* PatternPro.java -
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

import java.lang.ref.*;
import com.tc.jrexx.automaton.*;
import com.tc.jrexx.set.*;

/**
 * Regular expression based on a minimized deterministic automaton (FSA) and designed as a set of strings.
 * <br>Use this class to manipulate a reular expression through set oprations or automaton methods
 * <br>PatternPro differs from Pattern that the contributed set of strings is mutable through the methods
 * addAll, removeAll and retainAll.
 * <br>Further PaternPro provides access to its PAutomaton through the getAutomaton method.
 * So it is possible to inspect the automaton's states through PAutomaton's methods.
 * @author Ralf Meyer
 * @version 1.0
 */
public class PatternPro extends Pattern {

  WeakReference automatonWrapper = null;
//  WeakReference patternWrapper = null;

  protected PatternPro(ISet_char fullSet) {
    super(fullSet);
  }

  protected PatternPro(Automaton_Pattern automaton) {
    super(automaton);
  }

  protected AutomatonSet_String getInnerAutomaton() {
    return this.automaton;
  }

  public PatternPro() {
    this(new Automaton_Pattern());
  }

  /**
   * creates a PatternPro with the given automaton. The automaton will not be cloned:
   * two PatternPro can use the same automaton.
   */
  public PatternPro(PAutomaton automaton) {
    super((Automaton_Pattern)automaton.getAutomaton());
    this.automatonWrapper = new WeakReference(automaton);
  }

  /**
   * copy constructor
   */
  public PatternPro(Pattern p) {
    super((Automaton_Pattern)p.automaton.clone());
  }

  public PatternPro(String regEx) {
    super(Pattern.get(regEx,false));
  }



  public void setRegEx(String regEx) {
    this.automaton = Pattern.get(regEx,false);
  }


  /**
   * if p is an instance of PatternPro
   * use setAutomaton(p.getAutomaton());
   * else setAutomaton(new PatternPro(p).getAutomaton())
   *
   * @deprecated
   */
  public void setPattern(Pattern p) {
    this.automaton = (Automaton_Pattern)p.automaton.clone();
    if (p.getClass()!=Pattern.class) this.automaton.minimize();
  }



  public void setAutomaton(PAutomaton a) {
    this.automatonWrapper = new WeakReference(a);
    this.automaton = (Automaton_Pattern)a.getAutomaton();
  }

  /**
   * don't needed: you have a PatternPro which extends Pattern.
   * (Pattern)this.clone() has the same effect
   * @deprecated
   */
  public Pattern getPattern() {
    return new Pattern((Automaton_Pattern)this.automaton.clone());
  }

  public PAutomaton getAutomaton() {
    if (this.automatonWrapper==null) {
      PAutomaton answer = new PAutomaton(this.automaton);
      this.automatonWrapper = new WeakReference(answer);
      return answer;
    }

    PAutomaton answer = (PAutomaton)this.automatonWrapper.get();
    if (answer!=null) return answer;

    answer = new PAutomaton(this.automaton);
    this.automatonWrapper = new WeakReference(answer);
    return answer;
  }

  public boolean contains(String s,int offset,int length) {
    if (this.automaton.isDeterministic()) return super.contains(s,offset,length);

    Automaton.State state = ((Automaton_Pattern)this.automaton).getStartState();

//int _offset = offset;
//int _length = length;
//long start = System.currentTimeMillis();
//try {
    if (state==null) return false;

    Automaton.IState istate = ((Automaton_Pattern.PState)state).getEClosure();
    Automaton.LinkedSet_State states =
        (istate instanceof Automaton_Pattern.PState)
        ? this.automaton.newLinkedSet_State((Automaton_Pattern.PState)istate)
        : (Automaton.LinkedSet_State)istate;

    Automaton.LinkedSet_State newStates = this.automaton.newLinkedSet_State();

    for (;length>0; ++offset, --length) {
      loop: for (Automaton.Wrapper_State w=states.elements; w!=null; w=w.next) {
        if (w.state.isDeterministic()) {
          for (Automaton.State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
            if (trans.charSet.contains(s.charAt(offset))) {
              newStates.add(trans.toState);
              continue loop;
            }
          }
        } else {
          for (Automaton.State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
            if (trans.charSet.contains(s.charAt(offset))) {
              newStates.add(trans.toState);
            }
          }
        }
      }

      for (Automaton.Wrapper_State w=newStates.elements; w!=null; w=w.next) {
        for (Automaton.State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
          newStates.add(trans.toState);
        }
      }

      if (newStates.isEmpty()) return false;

      Automaton.LinkedSet_State tmp = states;
      states = newStates;
      newStates = tmp;
      newStates.clear();
    }
    return ((Automaton_Pattern.LinkedSet_PState)states).isFinal();

//} finally {
//  long end = System.currentTimeMillis();
//  System.out.println("Pattern.contains: "+(end-start));
//  if (length>0) {
//    System.out.println(this.automaton);
//    s = s.substring(_offset,_offset+_length);
//    offset = offset-_offset;
//    if (offset<=100) System.out.println("  can start with: "+s.substring(0,offset)+"\"");
//    else System.out.println("  can start with: \""+s.substring(0,100)+"...\""+s.length());
//
//    if (s.length()-offset<=100) System.out.println("  stopped for   : "+s.substring(offset)+"\"");
//    else System.out.println("  stopped for   : "+s.substring(offset,offset+100)+"...\""+(s.length()-offset));
//
//    System.out.println("currentState: "+state);
//  }
//}
  }

  public boolean contains(char[] chars,int offset,int length) {
    if (this.automaton.isDeterministic()) return super.contains(chars,offset,length);

    Automaton.State state = ((Automaton_Pattern)this.automaton).getStartState();
    if (state==null) return false;

    Automaton.IState istate = ((Automaton_Pattern.PState)state).getEClosure();
    Automaton.LinkedSet_State states =
        (istate instanceof Automaton_Pattern.PState)
        ? this.automaton.newLinkedSet_State((Automaton_Pattern.PState)istate)
        : (Automaton.LinkedSet_State)istate;

    Automaton.LinkedSet_State newStates = this.automaton.newLinkedSet_State();

    for (;length>0; ++offset, --length) {
      loop: for (Automaton.Wrapper_State w=states.elements; w!=null; w=w.next) {
        if (w.state.isDeterministic()) {
          for (Automaton.State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
            if (trans.charSet.contains(chars[offset])) {
              newStates.add(trans.toState);
              continue loop;
            }
          }
        } else {
          for (Automaton.State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
            if (trans.charSet.contains(chars[offset])) {
              newStates.add(trans.toState);
            }
          }
        }
      }

      for (Automaton.Wrapper_State w=newStates.elements; w!=null; w=w.next) {
        for (Automaton.State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
          newStates.add(trans.toState);
        }
      }

      if (newStates.isEmpty()) return false;

      Automaton.LinkedSet_State tmp = states;
      states = newStates;
      newStates = tmp;
      newStates.clear();
    }
    return ((Automaton_Pattern.LinkedSet_PState)states).isFinal();
  }

  public boolean contains(java.io.Reader in) throws java.io.IOException {
    if (this.automaton.isDeterministic()) return super.contains(in);

    Automaton.State state = ((Automaton_Pattern)this.automaton).getStartState();
    if (state==null) return false;

    Automaton.IState istate = ((Automaton_Pattern.PState)state).getEClosure();
    Automaton.LinkedSet_State states =
        (istate instanceof Automaton_Pattern.PState)
        ? this.automaton.newLinkedSet_State((Automaton_Pattern.PState)istate)
        : (Automaton.LinkedSet_State)istate;

    Automaton.LinkedSet_State newStates = this.automaton.newLinkedSet_State();

    for (int ch=in.read(); ch!=-1; ch=in.read()) {
      loop: for (Automaton.Wrapper_State w=states.elements; w!=null; w=w.next) {
        if (w.state.isDeterministic()) {
          for (Automaton.State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
            if (trans.charSet.contains((char)ch)) {
              newStates.add(trans.toState);
              continue loop;
            }
          }
        } else {
          for (Automaton.State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
            if (trans.charSet.contains((char)ch)) {
              newStates.add(trans.toState);
            }
          }
        }
      }

      for (Automaton.Wrapper_State w=newStates.elements; w!=null; w=w.next) {
        for (Automaton.State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
          newStates.add(trans.toState);
        }
      }

      if (newStates.isEmpty()) return false;

      Automaton.LinkedSet_State tmp = states;
      states = newStates;
      newStates = tmp;
      newStates.clear();
    }
    return ((Automaton_Pattern.LinkedSet_PState)states).isFinal();  }

  public void complement() {
    this.automaton.complement();
  }

  public void addAll(String regEx) {
    this.automaton.addAll(regEx);
    this.automaton.minimize();
  }

  public void retainAll(String regEx) {
    this.automaton.retainAll(regEx);
    this.automaton.minimize();
  }

  public void removeAll(String regEx) {
    this.automaton.removeAll(regEx);
    this.automaton.minimize();
  }

  public void addAll(Pattern pattern) {
    this.automaton.addAll(pattern.automaton);
    this.automaton.minimize();
  }

  public void retainAll(Pattern pattern) {
    this.automaton.retainAll(pattern.automaton);
    this.automaton.minimize();
  }

  public void removeAll(Pattern pattern) {
    this.automaton.removeAll(pattern.automaton);
    this.automaton.minimize();
  }

  public void addAll(PAutomaton a) {
    this.automaton.addAll((Automaton_Pattern)a.getAutomaton());
    this.automaton.minimize();
  }

  public void retainAll(PAutomaton a) {
    this.automaton.retainAll((Automaton_Pattern)a.getAutomaton());
    this.automaton.minimize();
  }

  public void removeAll(PAutomaton a) {
    this.automaton.removeAll((Automaton_Pattern)a.getAutomaton());
    this.automaton.minimize();
  }

  public void clear() {
    this.automaton.clear();
  }

}