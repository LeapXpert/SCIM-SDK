package de.captaingoldfish.scim.sdk.server.filter;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 16.10.2019 - 16:52 <br>
 * <br>
 * represents an expression that should be negated
 */
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public final class NotExpressionNode extends FilterNode
{

  /**
   * the node that should be negated
   */
  @Getter
  @Setter
  private FilterNode rightNode;

  public NotExpressionNode(FilterNode rightNode)
  {
    rightNode.setParent(this);
    this.rightNode = rightNode;
    setSubAttributeName(rightNode.getSubAttributeName());
  }

  @Override
  public String toString()
  {
    return "not ( " + rightNode.toString() + " )";
  }
}
