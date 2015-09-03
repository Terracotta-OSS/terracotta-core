package org.terracotta.passthrough;


/**
 * Contains the data required to uniquely identify an entity ref (either an entity or where an entity can be created).
 * Contains the class name and entity name.
 */
public class PassthroughEntityTuple {
  private final String entityClassName;
  private final String entityName;

  public PassthroughEntityTuple(String entityClassName, String entityName) {
    this.entityClassName = entityClassName;
    this.entityName = entityName;
  }

  @Override
  public boolean equals(Object obj) {
    boolean isEqual = (obj == this);
    if (!isEqual && (obj instanceof PassthroughEntityTuple)) {
      PassthroughEntityTuple other = (PassthroughEntityTuple)obj;
      isEqual = this.entityClassName.equals(other.entityClassName)
          && this.entityName.equals(other.entityName);
    }
    return isEqual;
  }

  @Override
  public int hashCode() {
    return this.entityClassName.hashCode() ^ this.entityName.hashCode();
  }
}
