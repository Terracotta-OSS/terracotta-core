package org.terracotta.tcbuild.configuration;

import java.util.Map;

public class TCConfigurations
{
    private AppserverConfiguration[] appservers;
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
}
