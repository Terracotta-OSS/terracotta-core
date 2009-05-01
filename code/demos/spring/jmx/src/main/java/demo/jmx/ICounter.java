/*
@COPYRIGHT@
*/
package demo.jmx;

/**
* Counter interface used for AOP and JMX proxy
*/
public interface ICounter 
{
   public int next();
   public int getCurrent();
   public String getName();
}
