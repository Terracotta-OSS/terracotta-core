package org.terracotta.tcbuild.configuration;

import java.util.Map;

public class TCConfigurations
{
    private AppserverConfiguration[] appservers;
    private JdkConfiguration[] jdks;
    private Map buildControl;

    public AppserverConfiguration[] getAppservers()
    {
        return appservers;
    }

    public void setAppservers( AppserverConfiguration[] appservers )
    {
        this.appservers = appservers;
    }

    public Map getBuildControl()
    {
        return buildControl;
    }

    public void setBuildControl( Map buildControl )
    {
        this.buildControl = buildControl;
    }

    public JdkConfiguration[] getJdks()
    {
        return jdks;
    }

    public void setJdks( JdkConfiguration[] jdks )
    {
        this.jdks = jdks;
    }
}
