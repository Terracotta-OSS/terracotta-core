package org.terracotta.tcbuild.configuration;

import java.util.HashMap;

public class ModulesDefConfiguration
{
    private HashMap[] options;
    private ModuleGroup[] moduleGroups;

    public ModuleGroup[] getModuleGroups()
    {
        return moduleGroups;
    }
    public void setModuleGroups( ModuleGroup[] moduleGroups )
    {
        this.moduleGroups = moduleGroups;
    }
    public HashMap[] getOptions()
    {
        return options;
    }
    public void setOptions( HashMap[] options )
    {
        this.options = options;
    }
}

class ModuleGroup
{
    private String name;
    private String[] modules;

    public String[] getModules()
    {
        return modules;
    }
    public void setModules( String[] modules )
    {
        this.modules = modules;
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
