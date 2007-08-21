package weblogic.j2ee.descriptor;

public interface FilterMappingBean {

    public abstract String getFilterName();

    public abstract void setFilterName(String s);

    public abstract String getUrlPattern();

    public abstract void setUrlPattern(String s);

    public abstract String getServletName();

    public abstract void setServletName(String s);

    public abstract String[] getDispatchers();

    public abstract void addDispatcher(String s);

    public abstract void removeDispatcher(String s);

    public abstract void setDispatchers(String as[]);

    public abstract String getId();

    public abstract void setId(String s);
}
