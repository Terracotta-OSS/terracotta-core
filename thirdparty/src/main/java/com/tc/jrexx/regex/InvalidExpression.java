/*
* 01/07/2003 - 15:19:32
*
* InvalidExpression.java -
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

public class InvalidExpression extends RuntimeException {

  private String expression;
  private String illegalToken;
  private int tokenPosition = -1;

  private static String makeMessage(String expression, String illegalToken, int tokenPosition) {
    try {
    String message = "Token \"" + illegalToken + "\" not allowed at this position: ";
         message+= expression.substring( 0,tokenPosition );
         message+= "\u0010" + illegalToken + "\u0011";
         message+= expression.substring( tokenPosition+illegalToken.length() );
      return message;
    } catch ( Exception e) {
      System.out.println( e);
      System.out.println( "EXPRESSION="+expression+" TOKENPOS="+tokenPosition+ " ILLEGALTOKEN="+illegalToken);
    }
    throw new RuntimeException();
  }

  InvalidExpression() {};

  public InvalidExpression(String message) {super(message);}

  public InvalidExpression(String expression,String illegalToken,int tokenPosition) {
    super (InvalidExpression.makeMessage(expression,illegalToken,tokenPosition));
    this.expression = expression;
    this.illegalToken = illegalToken;
    this.tokenPosition = tokenPosition;
  }

  public String getExpression() {
    return this.expression;
  }

  public String getIllegalToken() {
    return this.illegalToken;
  }

  public int getIllegalTokenPosition() {
    return this.tokenPosition;
  }

}