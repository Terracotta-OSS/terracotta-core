/*
@COPYRIGHT@
*/
package demo.webflow;

import java.io.Serializable;

/**
* Encapsulates logic for the demo webflow
*/
public class WebFlowBean 
implements Serializable 
{
   private static final long serialVersionUID = 1L;

   public static final String STATEA   = "stateA";
   public static final String STATEB   = "stateB";
   public static final String STATEC   = "stateC";
   public static final String STATED   = "stateD";
   public static final String COMPLETE = "complete";

   private String state = STATEA;  
   private String valueA;  
   private String valueB;  
   private String valueC;  
   private String valueD;  

   public String getState() 
   {
      return state;
   }

   public String setState(String state) 
   {
      this.state = state;
      return state;
   }

   public String setA(String value) 
   {
      this.valueA = value;
      setState(value == null ? STATEA : STATEB);
      return getState();
   }

   public String setB(String value) 
   {
      this.valueB = value;
      setState(value == null ? STATEB : STATEC);
      return getState();
   }

   public String setC(String value) 
   {
      this.valueC = value;
      setState(value == null ? STATEC : STATED);
      return getState();
   }

   public String setD(String value) 
   {
      this.valueD = value;
      setState((valueA != null) && (valueB != null) && (valueC != null) && (valueD != null) ? COMPLETE : STATED);
      return getState();
   }

   public String getA() 
   {
      return valueA;
   }

   public String getB() 
   {
      return valueB;
   }

   public String getC() 
   {
      return valueC;
   }

   public String getD() 
   {
      return valueD;
   }
}
