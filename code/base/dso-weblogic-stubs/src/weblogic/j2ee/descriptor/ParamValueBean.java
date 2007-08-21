package weblogic.j2ee.descriptor;

public interface ParamValueBean {

    public abstract String[] getDescriptions();

    public abstract void addDescription(String s);

    public abstract void removeDescription(String s);

    public abstract void setDescriptions(String as[]);

    public abstract String getParamName();

    public abstract void setParamName(String s);

    public abstract String getParamValue();

    public abstract void setParamValue(String s);

    public abstract String getId();

    public abstract void setId(String s);
}
