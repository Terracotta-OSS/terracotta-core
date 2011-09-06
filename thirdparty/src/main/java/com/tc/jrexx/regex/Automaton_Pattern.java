/*
* 01/07/2003 - 15:19:32
*
* Automaton_Pattern.java -
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

import com.tc.jrexx.set.*;
import com.tc.jrexx.automaton.*;
import java.text.*;
// import java.util.*;


public class Automaton_Pattern extends com.tc.jrexx.set.AutomatonSet_String {

  protected static class PProperties extends SProperties {}

  protected interface IPState extends ISState {

  }

  protected class PState extends AutomatonSet_String.SState implements IPState {
    public PState(boolean isFinal) {
      super(isFinal);
    }

    protected Transition addTransition(IProperties properties,ISet_char charSet,State toState) {
      return super.addTransition(properties,charSet,toState);
    }

    protected boolean removeTransition(Transition trans) {
      return super.removeTransition(trans);
    }

    protected void setFinal(boolean isFinal) {
      super.setFinal(isFinal);
    }

    protected IState getEClosure() {
      return super.getEClosure();
    }

  }

  protected class LinkedSet_PState extends AutomatonSet_String.LinkedSet_SState implements IPState {

    protected LinkedSet_PState() {
      super();
    }

    protected LinkedSet_PState(PState state) {
      super(state);
    }

  }

  // private Map preDefinedAutomatons = null;
  protected String regEx = null;

  protected Automaton_Pattern(ISet_char fullSet) {
    super(fullSet);
  }

  protected Automaton_Pattern() {
    super();
    this.regEx= "";
  }

  protected Automaton_Pattern(String regEx) {
    super();
    this.regEx = "";
    this.addAll(regEx);
  }

  protected Automaton.State getStartState() {
    return super.getStartState();
  }

  protected State createState() {
    return new PState(false);
  }

  protected SState createState(boolean isFinal) {
    return new PState(isFinal);
  }

  protected LinkedSet_State newLinkedSet_State() {
    return new LinkedSet_PState();
  }

  protected LinkedSet_State newLinkedSet_State(State state) {
    return new LinkedSet_PState((PState)state);
  }

  protected void setStartState(SState state) {
    super.setStartState(state);
  }

  protected SState addState(boolean isFinal) {
    return super.addState(isFinal);
  }

  protected boolean removeState(PState removeState) {
    return super.removeState(removeState);
  }

  protected void clear() {
    super.clear();
    this.regEx = "";
  }

  protected LinkedSet_State getStates() {
    return super.getStates();
  }

  protected void minimize() {
    super.minimize();
  }

  protected void removeUselessStates() {
    super.removeUselessStates();
  }

  protected void addAll(SState state) {
    super.addAll(state);
  }

  protected SState complement(SState state) {
    return super.complement(state);
  }

  protected SState concat(SState state_A,SState state_B) {
    return super.concat(state_A,state_B);
  }

  protected SState repeat(SState state,int minTimes,int maxTimes) {
    //performance leak
    if ((state instanceof PState)==false) throw new IllegalArgumentException("(state instanceof PState)==false");

    return super.repeat(state,minTimes,maxTimes);
  }
  protected SState union(SState state_A,SState state_B) {
    return super.union(state_A,state_B);
  }

  protected SState intersect(SState state_A,SState state_B) {
    return super.intersect(state_A,state_B);
  }
/*
  protected PState minus(PState state_A,PState state_B) {
    return (PState)super.minus(state_A,state_B);
  }
*/

  protected void complement() {
    super.complement();
    if (this.regEx==null) return;
    if (this.regEx=="") this.regEx = ".*";
    else this.regEx = "!("+this.regEx+")";
  }

  protected void addAll(String regEx) {
    if (this.regEx==null) return;
    if (this.regEx=="") this.regEx = regEx;
    else {
      this.regEx = new StringBuffer(this.regEx.length()+regEx.length()+5)
                 .append('(').append(this.regEx).append(')')
                 .append('|')
                 .append('(').append(regEx).append(')')
                 .toString();
    }

    this.addAll(this.parseRegEx(regEx));
    this.removeUselessStates();
  }

  protected void retainAll(String regEx) {
    if (this.regEx==null) return;
    if (this.regEx.equals("") || regEx.equals("")) this.regEx = "";
    else {
      this.regEx = new StringBuffer(this.regEx.length()+regEx.length()+5)
                 .append('(').append(this.regEx).append(')')
                 .append('&')
                 .append('(').append(regEx).append(')')
                 .toString();
    }

    this.retainAll(this.parseRegEx(regEx));
    this.removeUselessStates();
  }

  protected void removeAll(String regEx) {
    if (this.regEx==null) return;
    if (this.regEx=="") this.regEx = "";
    else {
      this.regEx = new StringBuffer(this.regEx.length()+regEx.length()+6)
                 .append('(').append(this.regEx).append(')')
                 .append("&!")
                 .append('(').append(regEx).append(')')
                 .toString();
    }

    this.removeAll(this.parseRegEx(regEx));
    this.removeUselessStates();
  }


  protected boolean isDeterministic() {
    return super.isDeterministic();
  }

  protected boolean isDeterministic(State startState) {
    return super.isDeterministic(startState);
  }

  protected void addAll(AutomatonSet_String automaton) {
    super.addAll(automaton);

    Automaton_Pattern pAutomaton = (Automaton_Pattern)automaton;

    if (this.regEx==null || pAutomaton.regEx==null) return;
    if (this.regEx=="") this.regEx = pAutomaton.regEx;
    else {
      this.regEx = new StringBuffer(this.regEx.length()+pAutomaton.regEx.length()+5)
                 .append('(').append(this.regEx).append(')')
                 .append('|')
                 .append('(').append(pAutomaton.regEx).append(')')
                 .toString();
    }
  }

  protected void retainAll(AutomatonSet_String automaton) {
    super.retainAll(automaton);

    Automaton_Pattern pAutomaton = (Automaton_Pattern)automaton;

    if (this.regEx==null || pAutomaton.regEx==null) return;
    if (this.regEx=="" || pAutomaton.regEx=="") this.regEx = "";
    else {
      this.regEx = new StringBuffer(this.regEx.length()+pAutomaton.regEx.length()+5)
                 .append('(').append(this.regEx).append(')')
                 .append('&')
                 .append('(').append(pAutomaton.regEx).append(')')
                 .toString();
    }
  }

  protected void removeAll(AutomatonSet_String automaton) {
    super.removeAll(automaton);

    Automaton_Pattern pAutomaton = (Automaton_Pattern)automaton;

    if (this.regEx==null || pAutomaton.regEx==null) return;
    if (this.regEx=="") this.regEx = "";
    else if (pAutomaton.regEx!="") {
      this.regEx = new StringBuffer(this.regEx.length()+pAutomaton.regEx.length()+6)
                 .append('(').append(this.regEx).append(')')
                 .append("&!")
                 .append('(').append(pAutomaton.regEx).append(')')
                 .toString();
    }
  }



  protected Object clone() {
    final Automaton_Pattern clone =  (Automaton_Pattern)super.clone();
    clone.scanner = clone.newScanner();
    return clone;
  }


  //////////////////////////////////////////////////////////////////////////
  /////     P A R S I N G
  //////////////////////////////////////////////////////////////////////////

      private static final int  ERROR         = -2,  // the possible parser
                              SHIFT         = -3,  // actions used in ACTIONTABLE
                              REDUCE        = -4,  // value -1 is reserved
                              ACCEPT        = -5,  // for unknown constant value

                              RE            =  0,  // NonTerminal Symbols
                              TERM          =  1,  // IMPORTANT: the value represents the
                              ELEMENT       =  2,  //        rowNr in ACTIONTABLE

                              notOp     =  3,  // Terminal Symbols
                              andOp     =  4,  //  .
                              orOp          =  5, //      .
                              groupBegin    =  6, //      .
                              groupEnd    =  7,  //  .
                              repetition    =  8, //      .
                              label    =  9, //      .
                              regExp    = 10,  // IMPORTANT: the value represents the
                              EOF           = 11;  //        rowNr in ACTIONTABLE

      private static final int[][][] ACTIONTABLE = {
      // state  RE         TERM      ELEMENT   notOp      andOp      orOp      groupBegin groupEnd  repetition  label       regExp     EOF
      /*  0 */ {{SHIFT,2},{SHIFT,7},{SHIFT,5},{SHIFT,11},{ERROR, 0},{ERROR, 0},{SHIFT,14},{ERROR, 0},{ERROR, 0},{ERROR, 0},{SHIFT,16},{ERROR, 0}},
      /*  1 */ {{ERROR,0},{ERROR,0},{ERROR,0},{REDUCE,3},{REDUCE,3},{REDUCE,3},{REDUCE,3},{REDUCE,3},{REDUCE,3},{REDUCE,3},{REDUCE,3},{REDUCE,3}},
      /*  2 */ {{ERROR,0},{ERROR,0},{ERROR,0},{ERROR, 0},{ERROR, 0},{ERROR, 0},{ERROR, 0},{ERROR, 0},{ERROR, 0},{ERROR, 0},{ERROR, 0},{ACCEPT,0}},
      /*  3 */ {{ERROR,0},{ERROR,0},{ERROR,0},{ERROR, 0},{ERROR, 0},{ERROR, 0},{ERROR, 0},{REDUCE,1},{ERROR, 0},{ERROR, 0},{ERROR, 0},{REDUCE,1}},
      /*  4 */ {{ERROR,0},{ERROR,0},{ERROR,0},{ERROR, 0},{ERROR, 0},{ERROR, 0},{ERROR, 0},{SHIFT,13},{ERROR, 0},{ERROR, 0},{ERROR, 0},{ERROR, 0}},
      /*  5 */ {{ERROR,0},{SHIFT,8},{SHIFT,5},{SHIFT,11},{SHIFT,10},{REDUCE,6},{SHIFT,14},{REDUCE,6},{SHIFT, 1},{SHIFT,12},{SHIFT,16},{REDUCE,6}},
      /*  6 */ {{ERROR,0},{ERROR,0},{ERROR,0},{ERROR, 0},{ERROR, 0},{REDUCE,9},{ERROR, 0},{REDUCE,9},{SHIFT, 1},{SHIFT,12},{ERROR, 0},{REDUCE,9}},
      /*  7 */ {{ERROR,0},{ERROR,0},{ERROR,0},{ERROR, 0},{ERROR, 0},{SHIFT,15},{ERROR, 0},{REDUCE,0},{ERROR, 0},{ERROR, 0},{ERROR, 0},{REDUCE,0}},
      /*  8 */ {{ERROR,0},{ERROR,0},{ERROR,0},{ERROR, 0},{ERROR, 0},{REDUCE,7},{ERROR, 0},{REDUCE,7},{ERROR, 0},{ERROR, 0},{ERROR, 0},{REDUCE,7}},
      /*  9 */ {{ERROR,0},{ERROR,0},{ERROR,0},{ERROR, 0},{ERROR, 0},{REDUCE,8},{ERROR, 0},{REDUCE,8},{ERROR, 0},{ERROR, 0},{ERROR, 0},{REDUCE,8}},
      /* 10 */ {{ERROR,0},{SHIFT,9},{SHIFT,5},{SHIFT,11},{ERROR, 0},{ERROR, 0},{SHIFT,14},{ERROR, 0},{ERROR, 0},{ERROR, 0},{SHIFT,16},{ERROR, 0}},
      /* 11 */ {{ERROR,0},{ERROR,0},{SHIFT,6},{ERROR, 0},{ERROR, 0},{ERROR, 0},{SHIFT,14},{ERROR, 0},{ERROR, 0},{ERROR, 0},{SHIFT,16},{ERROR, 0}},
      /* 12 */ {{ERROR,0},{ERROR,0},{ERROR,0},{REDUCE,4},{REDUCE,4},{REDUCE,4},{REDUCE,4},{REDUCE,4},{REDUCE,4},{REDUCE,4},{REDUCE,4},{REDUCE,4}},
      /* 13 */ {{ERROR,0},{ERROR,0},{ERROR,0},{REDUCE,2},{REDUCE,2},{REDUCE,2},{REDUCE,2},{REDUCE,2},{REDUCE,2},{REDUCE,2},{REDUCE,2},{REDUCE,2}},
      /* 14 */ {{SHIFT,4},{SHIFT,7},{SHIFT,5},{SHIFT,11},{ERROR, 0},{ERROR, 0},{SHIFT,14},{ERROR, 0},{ERROR, 0},{ERROR, 0},{SHIFT,16},{ERROR, 0}},
      /* 15 */ {{SHIFT,3},{SHIFT,7},{SHIFT,5},{SHIFT,11},{ERROR, 0},{ERROR, 0},{SHIFT,14},{ERROR, 0},{ERROR, 0},{ERROR, 0},{SHIFT,16},{ERROR, 0}},
      /* 16 */ {{ERROR,0},{ERROR,0},{ERROR,0},{REDUCE,5},{REDUCE,5},{REDUCE,5},{REDUCE,5},{REDUCE,5},{REDUCE,5},{REDUCE,5},{REDUCE,5},{REDUCE,5}}
    };


      //  the number after a SHIFT action is the next state to go to (see case SHIFT)
      //  the number after a REDUCE action is the number of a rule (see case REDUCE)


    private static final Integer[] INTEGERS = new Integer[ACTIONTABLE.length];
    static {
      for (int i=0; i<INTEGERS.length; i++) INTEGERS[i] = Integer.valueOf(i);
    }

  protected SState parseRegEx(String regEx) throws InvalidExpression {
    final java.util.List tokenList = this.scanner.scan( regEx );

    final Object[] extdTokenList = tokenList.toArray(new Object[tokenList.size()+1]);
    extdTokenList[extdTokenList.length-1] = Terminal_EOF.INSTANCE;

    java.util.Stack symbolStack = new java.util.Stack();
    java.util.Stack stateStack = new java.util.Stack();

    int extdTokenListIndex = 0;
    Object token = extdTokenList[extdTokenListIndex];

    int stateNr = 0, tokenSymbol=-1, action = Automaton_Pattern.ERROR;
    do {
      if (tokenSymbol==-1) {
        if  (token instanceof SState)   tokenSymbol = Automaton_Pattern.regExp;
        else if  (token instanceof Terminal_Repetition)   tokenSymbol = Automaton_Pattern.repetition;
        else if  (token instanceof Terminal_GroupBegin)   tokenSymbol = Automaton_Pattern.groupBegin;
        else if  (token instanceof Terminal_GroupEnd)   tokenSymbol = Automaton_Pattern.groupEnd;
        else if  (token instanceof String)           tokenSymbol = Automaton_Pattern.label;
        else if  (token instanceof Terminal_OrOp)   tokenSymbol = Automaton_Pattern.orOp;
        else if  (token instanceof Terminal_RegExp)   tokenSymbol = Automaton_Pattern.regExp;
        else if  (token instanceof Terminal_AndOp)   tokenSymbol = Automaton_Pattern.andOp;
        else if  (token instanceof Terminal_NotOp)   tokenSymbol = Automaton_Pattern.notOp;
        else if (token instanceof Terminal_EOF)    tokenSymbol = Automaton_Pattern.EOF;
        else {
          String message = "Unknown symbol/token: " + token;
          message+= "\n(check Parser or Scanner for this symbol/token)";
          throw new RuntimeException(message);
        }
      }

//System.out.println("$ "+symbolStack);
//System.out.print("+ "+stateNr+","+tokenSymbol+" -> ");
    action = Automaton_Pattern.ACTIONTABLE[stateNr][tokenSymbol][0];


    PState finalState,aState;

    switch (action) {
      case Automaton_Pattern.SHIFT :
//System.out.println("SHIFT "+ACTIONTABLE[stateNr][tokenSymbol][1]);
        stateStack.push( Automaton_Pattern.INTEGERS[stateNr] );
        symbolStack.push( token );
        stateNr = Automaton_Pattern.ACTIONTABLE[stateNr][tokenSymbol][1];
        ++extdTokenListIndex;
        token = extdTokenList[extdTokenListIndex];
        tokenSymbol = -1;
        break;

      case Automaton_Pattern.REDUCE :
//System.out.println("REDUCE "+ACTIONTABLE[stateNr][tokenSymbol][1]);
        final int ruleNr = Automaton_Pattern.ACTIONTABLE[stateNr][tokenSymbol][1];

        Object node=null; int nodeSymbol=-1;
        switch(ruleNr) {
          case 0: // RE ::= TERM
          {
            node = symbolStack.pop();
            nodeSymbol = Automaton_Pattern.RE;
            break;
          }
          case 1: // RE ::= TERM orOp RE
          {
            PState re  = (PState)symbolStack.pop();
            /*Terminal_OrOp =*/ symbolStack.pop();
            PState term = (PState)symbolStack.pop();

            node = this.union(term,re);
            nodeSymbol = Automaton_Pattern.RE;
            break;
          }
          case 2: // ELEMENT ::= groupBegin RE groupEnd
          {
            Terminal_GroupEnd end = (Terminal_GroupEnd)symbolStack.pop();
            node = symbolStack.pop();
            Terminal_GroupBegin begin =(Terminal_GroupBegin)symbolStack.pop();
            if (begin.name==null && end.name!=null
                || begin.name!=null && begin.name.equals(end.name)==false)
              throw new IllegalArgumentException("endtag exspected for "+begin+" but found: "+end);

            nodeSymbol = Automaton_Pattern.ELEMENT;
            break;
          }
          case 3: // ELEMENT ::= ELEMENT repetition
          {
            Terminal_Repetition repetition = (Terminal_Repetition)symbolStack.pop();
            PState element = (PState)symbolStack.pop();

            node = repetition.to==Terminal_Repetition.UNLIMITED
                    ? this.repeat(element,repetition.from,0)
                    : this.repeat(element,repetition.from,repetition.to);

            nodeSymbol = Automaton_Pattern.ELEMENT;
            break;
          }

          case 4: // ELEMENT ::= ELEMENT label
          {
            String label = (String)symbolStack.pop();
            String labelDot = null;
            PState element = (PState)symbolStack.pop();

            node = element;
            nodeSymbol = Automaton_Pattern.ELEMENT;
            break;
          }
          case 5: // ELEMENT ::= regExp
          {
            node = symbolStack.pop();
            if (node instanceof Terminal_RegExp) { // or  instanceOf Terminal_RuntimeValue
              /* Automaton_Pattern preDefAutomaton;
              if (this.preDefinedAutomatons==null) preDefAutomaton = null;
              else {
                preDefAutomaton = (Automaton_Pattern)this.preDefinedAutomatons.get(((Terminal_RegExp)node).name);
              }
              if (preDefAutomaton==null) */ throw new IllegalArgumentException(((Terminal_RegExp)node).name+" is not defined");

              /* final Automaton.State startState = preDefAutomaton.getStartState();
              if (startState==null) {
                node = this.addState(false);
              } else {
                java.util.Map map = this.cloneState(startState);
                node = (Automaton_Pattern.PState)map.get(startState);
              } */
              }
            nodeSymbol = Automaton_Pattern.ELEMENT;
            break;
          }
          case 6: // TERM ::= ELEMENT
          {
            node = symbolStack.pop();
            nodeSymbol = Automaton_Pattern.TERM;
            break;
          }
          case 7: // TERM ::= ELEMENT TERM
          {
            PState term = (PState)symbolStack.pop();
            PState element = (PState)symbolStack.pop();

            node = this.concat(element,term);

            nodeSymbol = Automaton_Pattern.TERM;
            break;
          }
          case 8: // TERM ::= ELEMENT andOp TERM
          {
            PState term = (PState)symbolStack.pop();
            /*Terminal_AndOp = */ symbolStack.pop();
            PState element = (PState)symbolStack.pop();

            node = this.intersect(element,term);
            nodeSymbol = Automaton_Pattern.TERM;
            break;
          }
          case 9: // TERM ::= notOp ELEMENT
          {
            PState element = (PState)symbolStack.pop();
            /*Terminal_NotOp = */ symbolStack.pop();

            node = this.complement(element);
            nodeSymbol = Automaton_Pattern.TERM;
            break;
          }
          default :
            String message = "\nProgramming error in RE-Parser:"
                            +"\nACTIONTABLE contains wrong ruleNr "+ruleNr
                            +"\nor case "+ruleNr+" statement missing";
            throw new RuntimeException(message);
        } // end switch(rule)

        for (int i=stateStack.size()-symbolStack.size(); i>1; i--) stateStack.pop();
        stateNr = ((Integer)stateStack.peek()).intValue();
        symbolStack.push( node );
        stateNr = Automaton_Pattern.ACTIONTABLE[stateNr][nodeSymbol][1];
        break;
      }  // end switch(action)

    } while (action!=Automaton_Pattern.ACCEPT && action!=Automaton_Pattern.ERROR);

    if (action==Automaton_Pattern.ERROR) {
      System.out.print("parsed:");
      for (int i=0; i<extdTokenListIndex; ++i) {
        System.out.print(" "+extdTokenList[i]);
      }
      System.out.println("");
      System.out.print("rest: ");
      for (int i=extdTokenListIndex; i<extdTokenList.length-1; ++i) {
        System.out.print(" "+extdTokenList[i]);
      }
      System.out.println("");
      System.out.println("current state: "+stateNr);
      System.out.print("current Token: "+tokenSymbol);

//      for (int i=0; i<Automaton_Pattern.ACTIONTABLE[stateNr].length; ++i) {
//        if (Automaton_Pattern.ACTIONTABLE[stateNr][i][0]!=Automaton_Pattern.ERROR) {
//          System.out.println(
//        }
//      }
//        System.out.println([stateNr][0];
      throw new Error();
    }

//      String expression = ""; int tokenPosition=-1;
//      for (int i=0; i<tokenList.size(); i++) {
//        if (i==extdTokenListIndex) tokenPosition=expression.length();
//        expression+= String.valueOf(tokenList.get(i));
//      }
//      throw new InvalidExpression(
//        expression,
//        String.valueOf( extdTokenList[extdTokenListIndex] ),
//        tokenPosition
//      );
//    }
//    return (SState)this.minimize(((SState)symbolStack.peek()));
//    return (SState)this.makeDeterministic(((SState)symbolStack.peek()));
    return (SState)symbolStack.peek();
  }




interface TerminalFormat {
  public Object parseObject(char[] source, ParsePosition status);
  public int maxLength();
}

/*
final class TerminalFormat_SPECIALLITERALS implements TerminalFormat {

  public TerminalFormat_SPECIALLITERALS() {};

  public Object parseObject(char[] source, ParsePosition status) {
    final int index = status.getIndex();

    switch (source[index]) {
      case '|' :   status.setIndex(index+1); return Terminal_OrOp.INSTANCE;
      case '(' :   status.setIndex(index+1); return Terminal_GroupBegin.INSTANCE;
      case ')' :   status.setIndex(index+1); return Terminal_GroupEnd.INSTANCE;
      case '*' :   status.setIndex(index+1); return new Terminal_Repetition(0,Terminal_Repetition.UNLIMITED);
      case '+' :   status.setIndex(index+1); return new Terminal_Repetition(1,Terminal_Repetition.UNLIMITED);
      case '?' :   status.setIndex(index+1); return new Terminal_Repetition(0,1);
      case '.' :   status.setIndex(index+1); return Det_AnyLiteral.INSTANCE;
      default  :   return null;  // throw new ParseException
    }
  }

  public int maxLength() {return 1;}

}
*/

final class TerminalFormat_LITERAL implements TerminalFormat {

  private final static char ESCAPE_CHAR = '\\';

  public TerminalFormat_LITERAL() {
        };

  public Object parseObject(char[] source, ParsePosition status) {
          int index = status.getIndex();

          switch (source[index]) {
            case '\\' : {
              ++index;
              if (index==source.length) return null;
              status.setIndex(index+1);

              final PState startState = (PState)Automaton_Pattern.this.addState(false);
              startState.addTransition(
                null,
                new CharSet(source[index]),
                Automaton_Pattern.this.addState(true)
              );
              return startState;
            }

            case '|' : status.setIndex(index+1); return Terminal_OrOp.INSTANCE;
            case '&' : status.setIndex(index+1); return Terminal_AndOp.INSTANCE;
            case '!' : status.setIndex(index+1); return Terminal_NotOp.INSTANCE;
            case '(' : status.setIndex(index+1); return Terminal_GroupBegin.INSTANCE;
            case ')' : status.setIndex(index+1); return Terminal_GroupEnd.INSTANCE;
            case '*' : status.setIndex(index+1); return new Terminal_Repetition(0,Terminal_Repetition.UNLIMITED);
            case '+' : status.setIndex(index+1); return new Terminal_Repetition(1,Terminal_Repetition.UNLIMITED);
            case '?' : status.setIndex(index+1); return new Terminal_Repetition(0,1);
            case '.' : {
              status.setIndex(index+1);
              ISet_char charSet = new CharSet();
              charSet.complement();
              final PState startState = (PState)Automaton_Pattern.this.addState(false);
              startState.addTransition(null,charSet,Automaton_Pattern.this.addState(true));
              return startState;
            }
            case '{' :
            case '}' :
            case '[' :
            case ']' :
            case '<' :
            case '>' : return null;

            default : {
              status.setIndex(index+1);
              final PState startState = (PState)Automaton_Pattern.this.addState(false);
              startState.addTransition(
                null,
                new CharSet(source[index]),
                Automaton_Pattern.this.addState(true)
              );
              return startState;
            }
          }
  }

  public int maxLength() {return 2;}

}


final class TerminalFormat_LITERALSET implements TerminalFormat {

  private static final int START = 0;
  private static final int FIRSTCHAR = 1;
  private static final int NORMAL = 2;
  private static final int ESCAPED = 3;

  public TerminalFormat_LITERALSET() {
//          this.automaton = automaton;
          //startState = automaton.addState(false);
          //automaton.addTransition(new CharSet('.'),automaton.addState(true));
        };

  public Object parseObject(char[] source, ParsePosition status) {
          int index = status.getIndex();
          final int sourceLength = source.length;

          ISet_char charSet = new CharSet();
          StringBuffer chars = new StringBuffer();
          boolean complement = false;
          boolean intervall = false;
          int state = START;
          while (index<sourceLength) {
            char ch = source[index];
            switch(state) {
              case START :
                switch(ch) {
                  case '[' : state = FIRSTCHAR; break;
                  default : return null;
                }
                break;
              case FIRSTCHAR :
                switch(ch) {
                  case ']' : return null;
                  case '\\' : state = ESCAPED; break;
                  case '^' : complement = true; state = NORMAL; break;
                  default : chars.append(ch); state = NORMAL;
                }
                break;
              case NORMAL :
                switch(ch) {
                  case '\\' : state = ESCAPED; break;
                  case ']' : { // END
                    index++;
                    status.setIndex(index);

                    charSet.addAll(chars.toString());
                    if (complement) charSet.complement();

                    final PState startState = (PState)Automaton_Pattern.this.addState(false);
                    startState.addTransition(null,charSet,Automaton_Pattern.this.addState(true));
                    return startState;
                  }
                  default :
                    if (intervall) {
                      char from = chars.charAt(chars.length()-1);
                      if (from>ch) return null;
                      for (char c=++from; c<=ch; c++) charSet.add(c);
                      intervall = false;
                    } else {
                      if (ch=='-') {
                        if (chars.length()==0) return null;
                        intervall = true;
                      } else chars.append(ch);
                    }
                    // STATE = NORMAL; (not necessary because state is NORMAL)
                }
                break;
              case ESCAPED :
                switch(ch) {
                  default :
                    if (intervall) {
                      char from = (char)(((int)chars.charAt(chars.length()-1))+1);
                      for (char c=from; c<=ch; c++) charSet.add(c);
                      intervall = false;
                    } else chars.append(ch);
                    state = NORMAL;
                }
                break;
              default :
                String message = "unknown state " + state;
                throw new RuntimeException(message);
            }

            index++;
          }

          return null;
  }

  public int maxLength() {return PScanner.UNLIMITED_MAX_LENGTH;}
}


final class TerminalFormat_GroupBegin implements TerminalFormat {

  public TerminalFormat_GroupBegin() {}

  public Object parseObject(char[] source, ParsePosition status) {
    final int sourceLength = source.length;
    int index = status.getIndex();
    if (index>=sourceLength) {
      String message = "";
      throw new ArrayIndexOutOfBoundsException(message);
    }

    if (source[index]!='<') return null;

    index++; final int startIndex = index;
    while (index<sourceLength && source[index]!='>' && source[index]!='.') index++;
    if (index==sourceLength) return null;
    if (source[index]=='.') return null;

    status.setIndex(index+1);

//    if (startIndex==index) return Terminal_GroupBegin.INSTANCE;
    return new Terminal_GroupBegin(new String(source,startIndex,index-startIndex));
  }

  public int maxLength() {return PScanner.UNLIMITED_MAX_LENGTH;}
}

final class TerminalFormat_GroupEnd implements TerminalFormat {

  public TerminalFormat_GroupEnd() {}

  public Object parseObject(char[] source, ParsePosition status) {
    final int sourceLength = source.length;
    int index = status.getIndex();
    if (index>=sourceLength) {
      String message = "";
      throw new ArrayIndexOutOfBoundsException(message);
    }

    if (source[index]!='<') return null;
    index++;
    if (source[index]!='/') return null;

    index++; final int startIndex = index;
    while (index<sourceLength && source[index]!='>') index++;
    if (index==sourceLength) return null;

    status.setIndex(index+1);

//    if (startIndex+1==index) return Terminal_GroupEnd.INSTANCE;
    return new Terminal_GroupEnd(new String(source,startIndex,index-startIndex));
  }

  public int maxLength() {return PScanner.UNLIMITED_MAX_LENGTH;}
}


final class TerminalFormat_REPETITION implements TerminalFormat {

  private final static int START = 0;
  private final static int FROM_FIRSTCHAR = 1;
  private final static int FROM_NORMAL = 2;
  private final static int TO_FIRSTCHAR = 3;
  private final static int TO_NORMAL = 4;

  public TerminalFormat_REPETITION() {
  };

  public Object parseObject(char[] source, ParsePosition status) {
    int index = status.getIndex();
    final int sourceLength = source.length;

    StringBuffer chars = new StringBuffer();
    int from = 0;
    int state = START;
    while (index<sourceLength) {
      char ch = source[index];
      switch(state) {
        case START :
          switch(ch) {
                  case '{' : state = FROM_FIRSTCHAR; break;
                  default : return null;
          }
          break;
        case FROM_FIRSTCHAR :
          switch(ch) {
                  case '0' : case '1' : case '2' : case '3' : case '4' :
                  case '5' : case '6' : case '7' : case '8' : case '9' :
                          chars.append(ch);
                          state = FROM_NORMAL;
                          break;
                  default : return null;
          }
          break;
        case FROM_NORMAL :
          switch(ch) {
                  case '0' : case '1' : case '2' : case '3' : case '4' :
                  case '5' : case '6' : case '7' : case '8' : case '9' :
                          chars.append(ch);
                          //state = NORMAL; // not necessary because state is NORMAL
                          break;
                  case ',' :
                          from = Integer.parseInt(chars.toString());
                          chars.setLength(0);
                          state = TO_FIRSTCHAR;
                          break;
                  case '}' : // END
                          index++; status.setIndex(index);
                          final int count = Integer.parseInt(chars.toString());
                          return new Terminal_Repetition(count,count);
                  default : return null;
          }
          break;
        case  TO_FIRSTCHAR :
          switch(ch) {
                  case '0' : case '1' : case '2' : case '3' : case '4' :
                  case '5' : case '6' : case '7' : case '8' : case '9' :
                          chars.append(ch);
                          state = TO_NORMAL;
                          break;
                  case '*' : // may be END
                          index++;
                          if (index==sourceLength) return null;
                          if (source[index]!='}') return null;
                          index++; status.setIndex(index);
                          return new Terminal_Repetition(from,Terminal_Repetition.UNLIMITED);
                  default : return null;
          }
          break;
        case TO_NORMAL :
          switch(ch) {
                  case '0' : case '1' : case '2' : case '3' : case '4' :
                  case '5' : case '6' : case '7' : case '8' : case '9' :
                          chars.append(ch);
                          state = TO_NORMAL;
                          break;
                  case '}' : // END
                          index++; status.setIndex(index);
                          final int to = Integer.parseInt(chars.toString());
                          return new Terminal_Repetition(from,to);
                  default : return null;
          }
          break;
      }

      index++;
    }

    return null;
  }


  public int maxLength() {return PScanner.UNLIMITED_MAX_LENGTH;}
}

final class TerminalFormat_LABEL implements TerminalFormat {
  public TerminalFormat_LABEL() {
  };

  public Object parseObject(char[] source, ParsePosition status) {
    int startIndex = status.getIndex();
    int index = startIndex;
    if (source[index++]!='{') return null;
    if (source[index++]!='=') return null;

    while (index<source.length &&
          ('A'<=source[index] && source[index]<='Z'
        || 'a'<=source[index] && source[index]<='z'
        || '0'<=source[index] && source[index]<='9')) ++index;

    if (index==source.length) return null;
    if (source[index]!='}') return null;

    status.setIndex(index+1);
    return new String(source,startIndex+2,index-startIndex-2);
  }
  public int maxLength() {return PScanner.UNLIMITED_MAX_LENGTH;}
}

final class TerminalFormat_RegExp implements TerminalFormat {
  public TerminalFormat_RegExp() {
  };

  public Object parseObject(char[] source, ParsePosition status) {
    int startIndex = status.getIndex();
    int index = startIndex;
    if (source[index++]!='{') return null;

    if (('A'<=source[index] && source[index]<='Z' || 'a'<=source[index] && source[index]<='z')==false) return null;
    ++index;
    while (index<source.length &&
          ('A'<=source[index] && source[index]<='Z'
        || 'a'<=source[index] && source[index]<='z'
        || '0'<=source[index] && source[index]<='9'
        || source[index]=='_' || source[index]=='/' || source[index]=='-')) ++index;

    if (index==source.length) return null;
    if (source[index]!='}') return null;

    status.setIndex(index+1);
    return new Terminal_RegExp(new String(source,startIndex+1,index-startIndex-1));
  }
  public int maxLength() {return PScanner.UNLIMITED_MAX_LENGTH;}
}



  protected PScanner scanner = this.newScanner();
  protected PScanner newScanner() {
    return new PScanner(
      new TerminalFormat[] {
        //new TerminalFormat_SPECIALLITERALS() // RegEx_SpecialLiteralsFormat();
        new TerminalFormat_LITERALSET()       // RegEx_LiteralSe<tFormat();
        //,new TerminalFormat_STRING()          // RegEx_StringFormat();
        ,new TerminalFormat_REPETITION()       // RegEx_RepetitionFormat();
        ,new TerminalFormat_LABEL()
        ,new TerminalFormat_GroupBegin()
        ,new TerminalFormat_GroupEnd()
        ,new TerminalFormat_LITERAL()    // RegEx_LiteralFormat();
        ,new TerminalFormat_RegExp()
      },
      /*terminalFormatsAreExclusive=*/true
    );
  }

}
