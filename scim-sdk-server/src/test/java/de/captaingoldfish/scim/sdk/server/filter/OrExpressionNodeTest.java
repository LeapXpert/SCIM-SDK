package de.captaingoldfish.scim.sdk.server.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.var;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import nl.jqno.equalsverifier.EqualsVerifier;


/**
 * author Pascal Knueppel <br>
 * created at: 19.10.2019 - 23:38 <br>
 * <br>
 */
public class OrExpressionNodeTest
{

  /**
   * needed to extract the {@link ResourceType}s which are necessary to check if the given
   * filter-attribute-names are valid or not
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the user resource type
   */
  private ResourceType userResourceType;

  /**
   * initializes a new {@link ResourceTypeFactory} for the following tests
   */
  @BeforeEach
  public void initialize()
  {
    this.resourceTypeFactory = new ResourceTypeFactory();
    JsonNode userResourceTypeJson = JsonHelper.loadJsonDocument(ClassPathReferences.USER_RESOURCE_TYPE_JSON);
    JsonNode userSchema = JsonHelper.loadJsonDocument(ClassPathReferences.USER_SCHEMA_JSON);
    JsonNode enterpriseUser = JsonHelper.loadJsonDocument(ClassPathReferences.ENTERPRISE_USER_SCHEMA_JSON);
    this.userResourceType = resourceTypeFactory.registerResourceType(null,
                                                                     userResourceTypeJson,
                                                                     userSchema,
                                                                     enterpriseUser);
  }

  /**
   * verifies that the equals method does work as expected
   */
  @Test
  public void testEquals()
  {
    final String filter1 = "userName eq \"false\" and (name.givenName PR OR nickName eq \"blubb\") and "
                           + "displayName co \"chuck\" or meta.created gt \"2019-10-17T01:07:00Z\"";
    FilterNode filterNode1 = RequestUtils.parseFilter(userResourceType, filter1);
    Assertions.assertNotNull(filterNode1);
    final String filter2 = "userName eq \"false\" and (name.givenName PR OR nickName eq \"blubb\") and "
                           + "displayName co \"chuck\"";
    FilterNode filterNode2 = RequestUtils.parseFilter(userResourceType, filter2);
    Assertions.assertNotNull(filterNode2);
    EqualsVerifier.forClass(OrExpressionNode.class)
                  .usingGetClass()
                  .withIgnoredFields("parent", "subAttributeName")
                  .suppress(Warning.NONFINAL_FIELDS)
                  .withPrefabValues(FilterNode.class, filterNode1, filterNode2)
                  .verify();
  }

  @Test
  public void testSerialize() throws JsonProcessingException {
    final String filter2 = "name.givenName PR OR nickName eq \"blubb\"";
    FilterNode filterNode2 = RequestUtils.parseFilter(userResourceType, filter2);
    Assertions.assertNotNull(filterNode2);

    ObjectMapper objectMapper = new ObjectMapper();
    var carAsString = objectMapper.writeValueAsString(filterNode2);

    var filterNode = objectMapper.readValue(carAsString, FilterNode.class);
    Assertions.assertEquals(OrExpressionNode.class, filterNode.getClass());
    EqualsVerifier.forClass(OrExpressionNode.class)
        .usingGetClass()
        .withIgnoredFields("parent", "subAttributeName")
        .suppress(Warning.NONFINAL_FIELDS)
        .withPrefabValues(FilterNode.class, filterNode, filterNode2)
        .verify();
  }
}
