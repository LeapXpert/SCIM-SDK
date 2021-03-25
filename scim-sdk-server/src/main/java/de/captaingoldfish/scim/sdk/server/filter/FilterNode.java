package de.captaingoldfish.scim.sdk.server.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * author Pascal Knueppel <br>
 * created at: 16.10.2019 - 16:07 <br>
 * <br>
 * the abstract tree declaration that will be build when the SCIM filter expression is parsed
 */
@JsonTypeInfo(
    use = Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
    @Type(value = AndExpressionNode.class, name = "AndExpressionNode"),
    @Type(value = NotExpressionNode.class, name = "NotExpressionNode"),
    @Type(value = OrExpressionNode.class, name = "OrExpressionNode"),
    @Type(value = AttributeExpressionLeaf.class, name = "AttributeExpressionLeaf"),
    @Type(value = AttributePathRoot.class, name = "AttributePathRoot")
})
public abstract class FilterNode implements Serializable
{

  /**
   * each node should now its parent node just in case
   */
  @Getter
  @Setter(AccessLevel.PROTECTED)
  @JsonIgnore
  private FilterNode parent;

  /**
   * this attribute is relevant for resolving value-paths on patch operations
   */
  @Getter
  private String subAttributeName;

  /**
   * this attribute is relevant for resolving value-paths on patch operations
   */
  public void setSubAttributeName(String subAttributeName)
  {
    this.subAttributeName = subAttributeName;
  }
}

