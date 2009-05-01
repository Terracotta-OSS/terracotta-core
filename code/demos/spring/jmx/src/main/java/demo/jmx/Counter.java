/*
@COPYRIGHT@
*/
package demo.jmx;

import org.springframework.beans.factory.BeanNameAware;

/**
* Simple service bean
*/
public class Counter 
implements ICounter, BeanNameAware 
{
   private volatile int counter = 0;
   private String name;

   public int next() 
   {
      synchronized(this) 
      {
         return counter++;
      }
   }

   public int getCurrent() 
   {
      return counter;
   }

   public String getName() 
   {
      return this.name;
   }

   public void setBeanName(String name) 
   {
      this.name = name;
   }
}

