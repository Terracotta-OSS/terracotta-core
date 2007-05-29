package org.terracotta.tcbuild.configuration;

public class AppserverConfiguration
{
    private String name;
    private String minVersion;
    private String maxVersion;

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getMaxVersion()
    {
        return maxVersion;
    }

    public void setMaxVersion( String maxVersion )
    {
        this.maxVersion = maxVersion;
    }

    public String getMinVersion()
    {
        return minVersion;
    }

    public void setMinVersion( String minVersion )
    {
        this.minVersion = minVersion;
    }
}
