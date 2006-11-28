/*
* 01/07/2003 - 15:19:32
*
* Pattern.java -
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

import com.tc.jrexx.set.ISet_char;

import java.util.*;


import com.tc.jrexx.automaton.*;

import java.lang.ref.SoftReference;

/**
 * Regular expression based on a minimized deterministic automaton (DFA) and designed as an immutable set of strings.
 * <br>Use this class to create a regular expression and match strings against it.
 * <br>for example:
 * <br>to check whether a given string is a number try<br>
 * <br><code>new Pattern("[0-9]+").contains(s)</code>
 *
 * @author Ralf Meyer
 * @version 1.1
 */
public class Pattern implements Cloneable {

  protected static final HashMap AUTOMATON_MAP = new HashMap();

  protected static final Automaton_Pattern get(String regEx,boolean cache) {
    if (cache) {
      synchronized(AUTOMATON_MAP) {
        final SoftReference reference = (SoftReference)AUTOMATON_MAP.get(regEx);

        if (reference!=null) {
          Automaton_Pattern automaton = (Automaton_Pattern)reference.get();
          if (automaton!=null) return automaton;
        }

        final Automaton_Pattern automaton = new Automaton_Pattern(regEx);

        final Automaton.LinkedSet_State states = automaton.getStates();
        for (Automaton.Wrapper_State w = states.elements; w!=null; w=w.next) {
          for (Automaton.State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
            trans.properties = null;
          }
          for (Automaton.State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
            trans.properties = null;
          }
        }

        automaton.minimize();

        AUTOMATON_MAP.put(regEx,new SoftReference(automaton));
        return automaton;
      }
    } else {
      SoftReference reference = null;
      synchronized(AUTOMATON_MAP) {
        reference = (SoftReference)AUTOMATON_MAP.get(regEx);
      }

      if (reference!=null) {
        Automaton_Pattern automaton = (Automaton_Pattern)reference.get();
        if (automaton!=null) return automaton;
      }

      final Automaton_Pattern automaton = new Automaton_Pattern(regEx);

      final Automaton.LinkedSet_State states = automaton.getStates();
      for (Automaton.Wrapper_State w = states.elements; w!=null; w=w.next) {
        for (Automaton.State.Transition trans=w.state.eTransitions; trans!=null; trans=trans.next) {
          trans.properties = null;
        }
        for (Automaton.State.Transition trans=w.state.transitions; trans!=null; trans=trans.next) {
          trans.properties = null;
        }
      }

      automaton.minimize();

      return automaton;
    }
  }


  protected Automaton_Pattern automaton;
/*
  protected Pattern() {
    this(new Automaton_Pattern());
  }
*/
  protected Pattern(Automaton_Pattern automaton) {
    this.automaton = automaton;
  }

  protected Pattern(ISet_char fullSet) {
    this.automaton = new Automaton_Pattern(fullSet);
  }

  /**
   * creates a minimized deterministic automaton (DFA) from the given regEx pattern.
   */
  public Pattern(String regEx) {
    this(Pattern.get(regEx,true));
  }

  public boolean contains(String s) {
    return this.contains(s,0,s.length());
  }

  public boolean contains(String s,int offset) {
    return this.contains(s,offset,s.length()-offset);
  }


  public boolean contains(String s,int offset,int length) {
    Automaton.State state = ((Automaton_Pattern)this.automaton).getStartState();

//int _offset = offset;
//int _length = length;
//long start = System.currentTimeMillis();
//try {
    if (state==null) return false;

    Automaton.LinkedSet_State states = this.automaton.newLinkedSet_State(state);
    Automaton.LinkedSet_State newStates = this.automaton.newLinkedSet_State();

    loop: for (;length>0; ++offset, --length) {
      for (Automaton.State.Transition trans=state.transitions; trans!=null; trans=trans.next) {
        if (trans.charSet.contains(s.charAt(offset))) {
          state = trans.toState;
          continue loop;
        }
      }
      return false;
    }

    return ((Automaton_Pattern.PState)state).isFinal();
//} finally {
//  long end = System.currentTimeMillis();
//  System.out.println("Pattern.contains: "+(end-start));
//  if (length>0) {
//    System.out.println(this.automaton);
//    s = s.substring(_offset,_offset+_length);
//    offset = offset-_offset;
//    if (offset<=100) System.out.println("  can start with: "+s.substring(0,offset)+"\"");
//    else System.out.println("  can start with: \""+s.substring(0,100)+"...\""+s.length());

//    if (s.length()-offset<=100) System.out.println("  stopped for   : "+s.substring(offset)+"\"");
//    else System.out.println("  stopped for   : "+s.substring(offset,offset+100)+"...\""+(s.length()-offset));

//    System.out.println("currentState: "+state);
//  }
//}

  }

  public boolean contains(char[] chars) {
    return this.contains(chars,0,chars.length);
  }

  public boolean contains(char[] chars,int offset) {
    return this.contains(chars,offset,chars.length-offset);
  }


  public boolean contains(char[] chars,int offset,int length) {
    Automaton.State state = ((Automaton_Pattern)this.automaton).getStartState();
    if (state==null) return false;

    loop: for (;length>0; ++offset, --length) {
      for (Automaton.State.Transition trans=state.transitions; trans!=null; trans=trans.next) {
        if (trans.charSet.contains(chars[offset])) {
          state = trans.toState;
          continue loop;
        }
      }
      return false;
    }

    return ((Automaton_Pattern.PState)state).isFinal;
  }

  public boolean contains(java.io.Reader in) throws java.io.IOException {
    Automaton.State state = ((Automaton_Pattern)this.automaton).getStartState();
    if (state==null) return false;

    loop: for (int ch=in.read(); ch!=-1; ch=in.read()) {
      for (Automaton.State.Transition trans=state.transitions; trans!=null; trans=trans.next) {
        if (trans.charSet.contains((char)ch)) {
          state = trans.toState;
          continue loop;
        }
      }
      return false;
    }
    return ((Automaton_Pattern.PState)state).isFinal;
  }


  public String getRegEx() {
    return this.automaton.regEx;
  }

  public String toString() {
    return this.getRegEx();
  }

/*
  private void writeObject(java.io.OutputStream out) throws IOException {
    out.writeObject(this.toAutomatonData());
  }
  private void readObject(java.io.InputStream in) throws IOException,ClassNotFoundException {
    SAutomatonData a = (SAutomatonData)in.readObject();
    this.init(a);
  }


  public SAutomatonData toAutomatonData() {
    return new PAutomaton(this.automaton).toData();
  }

  public void toAutomatonData(OutputStream automatonDataStream) throws IOException {
    new OutputStream(automatonDataStream).writeObject(this);
  }


  protected void init(SAutomatonData a) {
    this.automaton = (Automaton_Pattern)new PAutomaton(a).getAutomaton();
  }
*/

  public Object clone() {
    try {
      Pattern clone = (Pattern)super.clone();
      clone.automaton = (Automaton_Pattern)clone.automaton.clone();
      return clone;
    } catch(CloneNotSupportedException e) {
      throw new Error("should never happen");
    }
  }

/*
  public Iterator iterator() {
    return new Iterator() {
      HashMap fromStates = null;
      HashMap toStates = new HashMap();
      Iterator it_states = null;
      ISet_char.Iterator it_charSet = null;
      {
        Object startState = Pattern.this.automaton.getStartState();
        if (startState!=null) {
          LinkedList lists = new LinkedList(); lists.add(new LinkedList());
          toStates.put(startState,lists);
        }


      }

      public boolean hasNext() {
        return false;
      }
      public Object next() {
        it_charSet.next();
        //if (this.it_states==null) it_states = fromStates.keySet().iterator();
        if (it_states.hasNext()==false) {
          this.fromStates = toStates;
          this.toStates = new HashMap();
          this.it_states
        }

        IStatePro.ITransition[] trans = state.getTransitions();

        Iterator it_lists = lists.iterator();
        while (it_lists.hasNext()) {
          LinkedList elements = (LinkedList)it_lists.next();

          for (int i=0; i<trans.length; ++i) {
            LinkedList toLists = (LinkedList)toStates.get(trans[i].getToState());
            if (toLists==null) {
              toLists = new LinkedList();
              toStates.put(trans[i].getToState(),toLists);
            }

            LinkedList toElements = new LinkedList(elements);
            toElements.add(trans[i].getCharSet());
            toLists.add(toElements);
          }
        }

        return null;
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
*/
}


