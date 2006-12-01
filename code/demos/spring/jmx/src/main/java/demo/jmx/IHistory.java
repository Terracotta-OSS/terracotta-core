/*
@COPYRIGHT@
*/
package demo.jmx;

/**
* IHistory interface used to expose history data to JMX
*/
public interface IHistory 
{
   String[] getHistory();  
   boolean getEnabled();
   void setEnabled(boolean enabled); 
   void reset();
}
