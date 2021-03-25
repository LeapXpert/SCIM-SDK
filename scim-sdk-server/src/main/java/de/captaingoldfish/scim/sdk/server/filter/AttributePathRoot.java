package de.captaingoldfish.scim.sdk.server.filter;

import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.filter.antlr.FilterAttributeName;
import de.captaingoldfish.scim.sdk.server.filter.antlr.ScimFilterParser;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * author Pascal Knueppel <br>
 * created at: 28.10.2019 - 23:14 <br>
 * <br>
 * this is a leaf node for resolving patch expressions that will hold the full name of the attribute e.g.
 * name.givenName or userName or emails.primary etc.
 */
@NoArgsConstructor
public class AttributePathRoot extends FilterNode
{

  /**
   * if the attribute path expression has a filter expression
   */
  @Getter
  @Setter
  private FilterNode child;

  /**
   * the fully qualified resource uri if used
   */
  @Setter
  private FilterAttributeName filterAttributeName;

  /**
   * the schema attribute that represents this attribute name
   */
  @Getter
  @Setter
  private SchemaAttribute schemaAttribute;

  /**
   * represents the original expression of this node
   */
  @Setter
  private String originalExpressionString;

  public AttributePathRoot(FilterNode child, ResourceType resourceType, ScimFilterParser.ValuePathContext ctx)
  {
    this.child = child;
    this.filterAttributeName = new FilterAttributeName((ScimFilterParser.ValuePathContext)null, ctx.attributePath());
    this.schemaAttribute = RequestUtils.getSchemaAttributeForFilter(resourceType, filterAttributeName);
    setSubAttributeName(ctx.subattribute == null ? null : ctx.subattribute.getText());
  }

  public String getResourceUri()
  {
    return filterAttributeName.getResourceUri();
  }

  public String getShortName()
  {
    return filterAttributeName.getShortName();
  }

  public String getFullName()
  {
    return filterAttributeName.getFullName();
  }

  public String getParentAttributeName()
  {
    return filterAttributeName.getParentAttributeName();
  }

  public String getComplexSubAttributeName()
  {
    return filterAttributeName.getComplexSubAttributeName();
  }

  public String getAttributeName()
  {
    return filterAttributeName.getAttributeName();
  }

  @Override
  public String toString()
  {
    return originalExpressionString == null ? (child == null ? filterAttributeName.toString() : child.toString())
      : originalExpressionString;
  }
}
