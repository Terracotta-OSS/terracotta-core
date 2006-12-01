/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package jmx;

/**
 * This is the management interface explicitly defined for the "SimpleStandard" standard MBean.
 * The "SimpleStandard" standard MBean implements this interface 
 * in order to be manageable through a JMX agent.
 *
 * The "SimpleStandardMBean" interface shows how to expose for management:
 * - a read/write attribute (named "State") through its getter and setter methods,
 * - a read-only attribute (named "NbChanges") through its getter method,
 * - an operation (named "reset").
 */
public interface SimpleStandardMBean {

    /**
     * Getter: set the "State" attribute of the "SimpleStandard" standard MBean.
     *
     * @return the current value of the "State" attribute.
     */
    public String getState() ;
    
    /** 
     * Setter: set the "State" attribute of the "SimpleStandard" standard MBean.
     *
     * @param <VAR>s</VAR> the new value of the "State" attribute.
     */
    public void setState(String s) ;
    
    /**
     * Getter: get the "NbChanges" attribute of the "SimpleStandard" standard MBean.
     *
     * @return the current value of the "NbChanges" attribute.
     */
    public Integer getNbChanges() ;
    
    public int getNbChangesInt();
    
    public String[] getAllStates();
    
    /**
     * Operation: reset to their initial values the "State" and "NbChanges" 
     * attributes of the "SimpleStandard" standard MBean.
     */
    public void reset() ;
}
