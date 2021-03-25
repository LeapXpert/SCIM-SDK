package de.captaingoldfish.scim.sdk.server.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import lombok.NoArgsConstructor;
import lombok.Setter;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.enums.Comparator;
import de.captaingoldfish.scim.sdk.common.constants.enums.Mutability;
import de.captaingoldfish.scim.sdk.common.constants.enums.ReferenceTypes;
import de.captaingoldfish.scim.sdk.common.constants.enums.Type;
import de.captaingoldfish.scim.sdk.common.constants.enums.Uniqueness;
import de.captaingoldfish.scim.sdk.common.exceptions.InvalidFilterException;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.filter.antlr.CompareValue;
import de.captaingoldfish.scim.sdk.server.filter.antlr.FilterAttributeName;
import de.captaingoldfish.scim.sdk.server.filter.antlr.ScimFilterParser;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;


/**
 * author Pascal Knueppel <br>
 * created at: 16.10.2019 - 12:37 <br>
 * <br>
 * Represents a comparable expression in the scim filter language like "userName eq 'chuck_norris'"
 */
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public final class AttributeExpressionLeaf extends FilterNode
{

  /**
   * the scim attribute name. This must be an attribute name that was previously registered with a resource
   * schema. if the attribute cannot be found in the represented {@link ResourceType} an
   * {@link InvalidFilterException} is thrown
   */
  @Setter
  @Getter
  @JsonProperty("filterAttributeName")
  private FilterAttributeName attributeName;

  /**
   * the comparator that tells us how the comparison should be executed
   */
  @Getter(AccessLevel.PUBLIC)
  @Setter
  private Comparator comparator;

  /**
   * the value of the comparison itself
   */
  @Setter
  @Getter
  @JsonProperty("value")
  private CompareValue compareValue;

  /**
   * the meta information of this attribute
   */
  @JsonIgnore
  @Getter
  @Setter
  private SchemaAttribute schemaAttribute;

  /**
   * tells us if the referenced value is part of an extension schema or part of the main schema
   */
  @Getter
  @Setter
  private boolean mainSchemaNode;

  public AttributeExpressionLeaf(ScimFilterParser.AttributeExpressionContext context, ResourceType resourceType)
  {
    ScimFilterParser.ValuePathContext attributeValuePath = getParentValuePath(context);
    this.comparator = Comparator.valueOf(getCompareOperatorValue(context));
    FilterAttributeName attributeName = new FilterAttributeName(attributeValuePath, context.attributePath());
    String parentName = attributeName.getParentAttributeName();

    if (attributeValuePath != null)
    {
      String subName = attributeValuePath.subattribute == null ? null : attributeValuePath.subattribute.getText();
      if (subName != null)
      {
        String fullSubname = StringUtils.stripToEmpty(parentName) + subName;
        SchemaAttribute subAttributeSchema = RequestUtils.getSchemaAttributeByAttributeName(resourceType, fullSubname);
        super.setSubAttributeName(subAttributeSchema == null ? null : subAttributeSchema.getName());
      }
    }

    this.schemaAttribute = RequestUtils.getSchemaAttributeForFilter(resourceType, attributeName);
    if (parentName != null && !parentName.equals(schemaAttribute.getParent().getName()))
    {
      this.attributeName = new FilterAttributeName(schemaAttribute.getParent().getScimNodeName(),
                                                   context.attributePath());
    }
    else
    {
      this.attributeName = attributeName;
    }
    this.compareValue = context.compareValue() == null ? null
      : new CompareValue(context.compareValue(), schemaAttribute);
    validateFilterComparator();
    this.mainSchemaNode = resourceType.getMainSchema().getId().equals(schemaAttribute.getSchema().getId());
  }

  @JsonIgnore
  public String getParentAttributeName()
  {
    return attributeName.getParentAttributeName();
  }

  /**
   * checks if this expression was initiated from a {@link ScimFilterParser.ValuePathContext} and returns the
   * parent attribute path context if present
   */
  private ScimFilterParser.ValuePathContext getParentValuePath(ParserRuleContext context)
  {
    if (ScimFilterParser.ValuePathContext.class.isAssignableFrom(context.getClass()))
    {
      return (ScimFilterParser.ValuePathContext)context;
    }
    else if (context.getParent() != null)
    {
      return getParentValuePath(context.getParent());
    }
    else
    {
      return null;
    }
  }

  /**
   * if the schema attribute is of {@link Type#BOOLEAN} than several operators are not allowed and must throw an
   * exception
   */
  private void validateFilterComparator()
  {
    if (Type.BOOLEAN.equals(schemaAttribute.getType()))
    {
      switch (comparator)
      {
        case GE:
        case GT:
        case LE:
        case LT:
        case SW:
        case EW:
        case CO:
          throw new InvalidFilterException("the comparator '" + comparator + "' is not allowed on attribute type '"
                                           + schemaAttribute.getType() + "'", null);
      }
    }
    if (Type.DATE_TIME.equals(schemaAttribute.getType()))
    {
      switch (comparator)
      {
        case EQ:
        case NE:
        case GE:
        case GT:
        case LE:
        case LT:
          if (!compareValue.isDateTime() && !compareValue.isNull())
          {
            throw new InvalidFilterException("the comparator '" + comparator + "' in combination with the given value"
                                             + " '" + compareValue.getValue() + "' is not allowed on attribute type '"
                                             + schemaAttribute.getType() + "'", null);
          }
          break;
        case PR:
          break;
        default:
          if (!compareValue.isDateTime() && !compareValue.isString() && !compareValue.isNull())
          {
            throw new InvalidFilterException("the comparator '" + comparator + "' in combination with the given value"
                                             + " '" + compareValue.getValue() + "' is not allowed on attribute type '"
                                             + schemaAttribute.getType() + "'", null);
          }
      }
    }
  }

  /**
   * tries to get the compare operator. This must be handled differently in cases when it is the
   * {@link Comparator#PR} operator because no value will be present then and the present comparator will be a
   * {@link org.antlr.v4.runtime.tree.TerminalNode} instead of a {@link ScimFilterParser.CompareOperatorContext}
   * node
   *
   * @param context the antlr context to extract the {@link Comparator} value
   * @return the {@link Comparator} value as string in upper case
   */
  private String getCompareOperatorValue(ScimFilterParser.AttributeExpressionContext context)
  {
    if (context.compareOperator() == null)
    {
      return context.children.get(1).getText().toUpperCase();
    }
    return context.compareOperator().getText().toUpperCase();
  }

  @JsonIgnore
  public String getResourceUri()
  {
    return attributeName.getResourceUri();
  }

  @JsonIgnore
  public String getShortName()
  {
    return attributeName.getShortName();
  }

  @JsonIgnore
  public String getFullName()
  {
    return attributeName.getFullName();
  }

  @JsonIgnore
  public String getAttributeName()
  {
    return attributeName.getAttributeName();
  }

  @JsonIgnore
  public String getComplexSubAttributeName()
  {
    return attributeName.getComplexSubAttributeName();
  }

  @JsonIgnore
  public String getValue()
  {
    return compareValue == null ? null : compareValue.getValue();
  }

  @JsonIgnore
  public Optional<Boolean> getBooleanValue()
  {
    return compareValue == null ? Optional.empty() : compareValue.getBooleanValue();
  }

  @JsonIgnore
  public Optional<BigDecimal> getNumberValue()
  {
    return compareValue == null ? Optional.empty() : compareValue.getNumberValue();
  }

  @JsonIgnore
  public Optional<String> getStringValue()
  {
    return compareValue == null ? Optional.empty() : compareValue.getStringValue();
  }

  @JsonIgnore
  public Optional<Instant> getDateTime()
  {
    return compareValue == null ? Optional.empty() : compareValue.getDateTime();
  }

  @JsonIgnore
  public Type getType()
  {
    return schemaAttribute.getType();
  }

  @JsonIgnore
  public Mutability getMutability()
  {
    return schemaAttribute.getMutability();
  }

  @JsonIgnore
  public Uniqueness getUniqueness()
  {
    return schemaAttribute.getUniqueness();
  }

  @JsonIgnore
  public boolean isMultiValued()
  {
    return schemaAttribute.isMultiValued();
  }

  @JsonIgnore
  public boolean isRequired()
  {
    return schemaAttribute.isRequired();
  }

  @JsonIgnore
  public boolean isCaseExact()
  {
    return schemaAttribute.isCaseExact();
  }

  @JsonIgnore
  public List<String> getCanonicalValues()
  {
    return schemaAttribute.getCanonicalValues();
  }

  @JsonIgnore
  public List<ReferenceTypes> getReferenceTypes()
  {
    return schemaAttribute.getReferenceTypes();
  }

  @JsonIgnore
  public boolean isNull()
  {
    return compareValue == null || compareValue.isNull();
  }

  @Override
  public String toString()
  {
    return attributeName + " " + comparator + (compareValue == null ? "" : " " + compareValue);
  }
}
