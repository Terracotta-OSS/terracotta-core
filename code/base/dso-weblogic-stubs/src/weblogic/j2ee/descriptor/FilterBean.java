package weblogic.j2ee.descriptor;

public interface FilterBean
{

    public abstract String[] getDescriptions();

    public abstract void addDescription(String s);

    public abstract void removeDescription(String s);

    public abstract void setDescriptions(String as[]);

    public abstract String[] getDisplayNames();

    public abstract void addDisplayName(String s);

    public abstract void removeDisplayName(String s);

    public abstract void setDisplayNames(String as[]);

    public abstract IconBean[] getIcons();

    public abstract IconBean createIcon();

    public abstract void destroyIcon(IconBean iconbean);

    public abstract String getFilterName();

    public abstract void setFilterName(String s);

    public abstract String getFilterClass();

    public abstract void setFilterClass(String s);

    public abstract ParamValueBean[] getInitParams();

    public abstract ParamValueBean createInitParam();

    public abstract void destroyInitParam(ParamValueBean paramvaluebean);

    public abstract String getId();

    public abstract void setId(String s);
}
