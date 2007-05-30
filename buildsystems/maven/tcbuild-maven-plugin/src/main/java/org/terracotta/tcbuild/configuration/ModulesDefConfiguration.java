package org.terracotta.tcbuild.configuration;

import java.util.Map;

public class ModulesDefConfiguration
{
    private Map[] options;
    private ModuleGroup[] moduleGroups;

    public ModuleGroup[] getModuleGroups()
    {
        return moduleGroups;
    }
    public void setModuleGroups( ModuleGroup[] moduleGroups )
    {
        this.moduleGroups = moduleGroups;
    }
    public Map[] getOptions()
    {
        return options;
    }
    public void setOptions( Map[] options )
    {
        this.options = options;
    }
}
