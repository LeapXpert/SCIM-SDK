package de.captaingoldfish.scim.sdk.server.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.captaingoldfish.scim.sdk.common.constants.ClassPathReferences;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import lombok.var;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AttributeExpressionLeafTest {
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

  @Test
  public void testSerialize() throws JsonProcessingException {
    final String filter2 = "userName eq \"hehehehe\"";
    FilterNode filterNode2 = RequestUtils.parseFilter(userResourceType, filter2);
    Assertions.assertNotNull(filterNode2);

    ObjectMapper objectMapper = new ObjectMapper();
    var carAsString = objectMapper.writeValueAsString(filterNode2);

    var filterNode = objectMapper.readValue(carAsString, FilterNode.class);
    Assertions.assertEquals(AttributeExpressionLeaf.class, filterNode.getClass());
  }
}
