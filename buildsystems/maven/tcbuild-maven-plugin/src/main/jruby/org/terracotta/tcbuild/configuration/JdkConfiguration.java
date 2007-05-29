package org.terracotta.tcbuild.configuration;

public class JdkConfiguration
{
    private String name;
    private String minVersion;
    private String maxVersion;
    private String home;
    private String[] aliases;

    public String[] getAliases()
    {
        return aliases;
    }
    public void setAliases( String[] aliases )
    {
        this.aliases = aliases;
    }
    public String getHome()
    {
        return home;
    }
    public void setHome( String home )
    {
        this.home = home;
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
    public String getName()
    {
        return name;
    }
    public void setName( String name )
    {
        this.name = name;
    }
}
