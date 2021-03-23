package de.captaingoldfish.scim.sdk.server.endpoints;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import de.captaingoldfish.scim.sdk.common.constants.AttributeNames;
import de.captaingoldfish.scim.sdk.common.constants.EndpointPaths;
import de.captaingoldfish.scim.sdk.common.constants.HttpHeader;
import de.captaingoldfish.scim.sdk.common.constants.HttpStatus;
import de.captaingoldfish.scim.sdk.common.constants.ResourceTypeNames;
import de.captaingoldfish.scim.sdk.common.constants.SchemaUris;
import de.captaingoldfish.scim.sdk.common.constants.ScimType;
import de.captaingoldfish.scim.sdk.common.constants.enums.PatchOp;
import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.etag.ETag;
import de.captaingoldfish.scim.sdk.common.exceptions.BadRequestException;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.DocumentValidationException;
import de.captaingoldfish.scim.sdk.common.exceptions.InternalServerException;
import de.captaingoldfish.scim.sdk.common.exceptions.NotImplementedException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.exceptions.ScimException;
import de.captaingoldfish.scim.sdk.common.request.PatchOpRequest;
import de.captaingoldfish.scim.sdk.common.request.PatchRequestOperation;
import de.captaingoldfish.scim.sdk.common.request.SearchRequest;
import de.captaingoldfish.scim.sdk.common.resources.EnterpriseUser;
import de.captaingoldfish.scim.sdk.common.resources.Group;
import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.resources.User;
import de.captaingoldfish.scim.sdk.common.resources.base.ScimObjectNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import de.captaingoldfish.scim.sdk.common.resources.complex.Name;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.AuthenticationScheme;
import de.captaingoldfish.scim.sdk.common.resources.multicomplex.Member;
import de.captaingoldfish.scim.sdk.common.response.CreateResponse;
import de.captaingoldfish.scim.sdk.common.response.DeleteResponse;
import de.captaingoldfish.scim.sdk.common.response.ErrorResponse;
import de.captaingoldfish.scim.sdk.common.response.GetResponse;
import de.captaingoldfish.scim.sdk.common.response.ListResponse;
import de.captaingoldfish.scim.sdk.common.response.ScimResponse;
import de.captaingoldfish.scim.sdk.common.response.UpdateResponse;
import de.captaingoldfish.scim.sdk.common.schemas.Schema;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.common.utils.JsonHelper;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.endpoints.base.GroupEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.ResourceTypeEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.ServiceProviderEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.base.UserEndpointDefinition;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.GroupHandlerImpl;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.ResourceTypeHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.ServiceProviderHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.handler.UserHandlerImpl;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceType;
import de.captaingoldfish.scim.sdk.server.schemas.ResourceTypeFactory;
import de.captaingoldfish.scim.sdk.server.schemas.custom.ResourceTypeFeatures;
import de.captaingoldfish.scim.sdk.server.utils.FileReferences;
import de.captaingoldfish.scim.sdk.server.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;


/**
 * author Pascal Knueppel <br>
 * created at: 07.10.2019 - 23:54 <br>
 * <br>
 */
@Slf4j
public class ResourceEndpointHandlerTest implements FileReferences
{

  /**
   * the resource type factory in which the resources will be registered
   */
  private ResourceTypeFactory resourceTypeFactory;

  /**
   * the resource endpoints implementation that will handle any request
   */
  private ResourceEndpointHandler resourceEndpointHandler;

  /**
   * a mockito spy to verify that the methods are called correctly by the {@link ResourceEndpointHandler}
   * implementation
   */
  private UserHandlerImpl userHandler;

  /**
   * a mockito spy to verify that the methods are called correctly by the {@link ResourceEndpointHandler}
   * implementation
   */
  private GroupHandlerImpl groupHandler;

  /**
   * a mockito spy to verify that the methods are called correctly by the {@link ResourceEndpointHandler}
   * implementation
   */
  private ResourceTypeHandler resourceTypeHandler;

  /**
   * a mockito spy to verify which methods are getting called under specific circumstances
   */
  private ServiceProviderHandler serviceProviderHandler;


  /**
   * initializes this test
   */
  @BeforeEach
  public void initialize()
  {
    userHandler = Mockito.spy(new UserHandlerImpl(true));
    groupHandler = Mockito.spy(new GroupHandlerImpl());
    UserEndpointDefinition userEndpoint = new UserEndpointDefinition(userHandler);
    GroupEndpointDefinition groupEndpoint = new GroupEndpointDefinition(groupHandler);

    ServiceProvider serviceProvider = ServiceProvider.builder().build();
    this.resourceEndpointHandler = new ResourceEndpointHandler(serviceProvider, userEndpoint, groupEndpoint);
    this.resourceTypeFactory = resourceEndpointHandler.getResourceTypeFactory();

    resourceTypeHandler = new ResourceTypeHandler(resourceTypeFactory);
    resourceTypeHandler = Mockito.spy(resourceTypeHandler);
    EndpointDefinition endpointDefinition = new ResourceTypeEndpointDefinition(resourceTypeHandler);
    ResourceType resourceType = resourceEndpointHandler.registerEndpoint(endpointDefinition);

    serviceProviderHandler = Mockito.spy(new ServiceProviderHandler(serviceProvider));
    endpointDefinition = new ServiceProviderEndpointDefinition(serviceProvider);
    endpointDefinition.setResourceHandler(serviceProviderHandler);
    resourceEndpointHandler.registerEndpoint(endpointDefinition);
  }

  /**
   * When creating the {@link ResourceEndpointHandler} without any {@link EndpointDefinition}s no exception must
   * be thrown because this might cause problems for the developer. This is something that was discovered when
   * setting up the sample project
   */
  @Test
  public void testCreateEndpointWithoutResourceEndpoints()
  {
    Assertions.assertDoesNotThrow(() -> new ResourceEndpointHandler(ServiceProvider.builder().build()));
  }

  /**
   * this test will create, read, update, read, delete and read a user instance
   */
  @Test
  public void testLifeOfResource()
  {
    final String endpoint = "/Users";
    User createdUser = createUser(endpoint);
    String userId = createdUser.getId().orElse(null);
    User readUser = getUser(endpoint, userId);
    Assertions.assertEquals(createdUser.getId().get(), readUser.getId().get());
    Assertions.assertEquals(createdUser.getUserName(), readUser.getUserName());

    User updatedUser = updateUser(endpoint, readUser);
    Assertions.assertEquals(userId, updatedUser.getId().get());
    ScimResponse deleteResponse = resourceEndpointHandler.deleteResource(endpoint,
                                                                         userId,
                                                                         Collections.emptyMap(),
                                                                         null);
    MatcherAssert.assertThat(deleteResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
    Mockito.verify(userHandler, Mockito.times(1)).deleteResource(userId, null);
    ScimResponse scimResponse = Assertions.assertDoesNotThrow(() -> resourceEndpointHandler.getResource(endpoint,
                                                                                                        userId));
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(ResourceNotFoundException.class));
    Assertions.assertEquals(HttpStatus.NOT_FOUND, errorResponse.getHttpStatus());
  }

  /**
   * creates a simple user and returns the created resource
   *
   * @param endpoint the resource endpoint that should be used
   * @return the created user
   */
  private User createUser(String endpoint)
  {
    User u = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    u.setMeta(Meta.builder().created(Instant.now()).lastModified(Instant.now()).build());
    ScimResponse scimResponse = resourceEndpointHandler.createResource(endpoint,
                                                                       u.toString(),
                                                                       getBaseUrlSupplier(),
                                                                       null);
    Mockito.verify(userHandler, Mockito.times(1)).createResource(Mockito.any(), Mockito.isNull());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
    Assertions.assertEquals(HttpStatus.CREATED, scimResponse.getHttpStatus());
    String createResponse = scimResponse.toString();
    Assertions.assertNotNull(createResponse);
    User user = JsonHelper.readJsonDocument(createResponse, User.class);
    String userId = user.getId().orElse(null);
    Assertions.assertNotNull(userId);
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            scimResponse.getHttpHeaders().get(HttpHeader.CONTENT_TYPE_HEADER));
    Assertions.assertNotNull(scimResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    Meta meta = user.getMeta().get();
    Assertions.assertEquals("User", meta.getResourceType().get());
    Assertions.assertEquals(getLocation(endpoint, userId), meta.getLocation().get());
    Assertions.assertTrue(meta.getCreated().isPresent());
    Assertions.assertTrue(meta.getLastModified().isPresent());
    // TODO check that the last modified value is correct
    return user;
  }

  /**
   * this test will throw an exception during object creation and check that the error response is correctly
   * returned
   */
  @Test
  public void testCreateUserWithScimException()
  {
    ConflictException exception = new ConflictException("blubb");
    Mockito.doThrow(exception).when(userHandler).createResource(Mockito.any(), Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.createResource("/Users",
                                                                       readResourceFile(USER_RESOURCE),
                                                                       getBaseUrlSupplier(),
                                                                       null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(exception, errorResponse.getScimException());
    Assertions.assertEquals(HttpStatus.CONFLICT, errorResponse.getHttpStatus());
  }

  /**
   * this test will throw an exception during object creation and check that the error response is correctly
   * returned
   */
  @Test
  public void testCreateUserWithRuntimeException()
  {
    RuntimeException exception = new RuntimeException("blubb");
    Mockito.doThrow(exception).when(userHandler).createResource(Mockito.any(), Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.createResource("/Users",
                                                                       readResourceFile(USER_RESOURCE),
                                                                       getBaseUrlSupplier(),
                                                                       null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(InternalServerException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(exception, errorResponse.getScimException().getCause());
    Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorResponse.getHttpStatus());
  }

  /**
   * this test will try to get a resource with an empty id which should cause a
   * {@link ResourceNotFoundException}
   */
  @Test
  public void testGetResourceWithoutId()
  {
    ScimResponse scimResponse = resourceEndpointHandler.getResource("/Users", "", null, getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(ResourceNotFoundException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.NOT_FOUND, errorResponse.getHttpStatus());
  }

  /**
   * if the returned resource by the getResource endpoint has no id a {@link DocumentValidationException} must
   * be thrown
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "123456"})
  public void testGetResourceWithReturnedResourceHasDifferentId(String id)
  {
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    user.setId(null);
    Mockito.doReturn(user)
           .when(userHandler)
           .getResource(Mockito.eq(id),
                        Mockito.isNull(),
                        Mockito.eq(Collections.emptyList()),
                        Mockito.eq(Collections.emptyList()), Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.getResource("/Users", id, null, getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(DocumentValidationException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorResponse.getHttpStatus());
  }

  /**
   * will show that a {@link ScimException} is correctly handled by the
   * {@link ResourceEndpointHandler#getResource(String, String)} method
   */
  @Test
  public void testThrowScimExceptionOnGetResource()
  {
    ResourceNotFoundException exception = new ResourceNotFoundException("blubb", null, null);
    Mockito.doThrow(exception)
           .when(userHandler)
           .getResource(Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.getResource("/Users", "123456", null, getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(ResourceNotFoundException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.NOT_FOUND, errorResponse.getHttpStatus());
  }

  /**
   * will show that a {@link RuntimeException} is correctly handled by the
   * {@link ResourceEndpointHandler#getResource(String, String)} method
   */
  @Test
  public void testThrowRuntimeExceptionOnGetResource()
  {
    RuntimeException exception = new RuntimeException("blubb");
    Mockito.doThrow(exception)
           .when(userHandler)
           .getResource(Mockito.any(),
                        Mockito.isNull(),
                        Mockito.eq(Collections.emptyList()),
                        Mockito.eq(Collections.emptyList()),
                        Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.getResource("/Users", "123456", null, getBaseUrlSupplier());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(InternalServerException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorResponse.getHttpStatus());
  }

  /**
   * verifies that the attribute parameter is handed over correctly to the {@link ResourceHandler}
   */
  @Test
  public void testGetResourceWithAttributesParameter()
  {
    final String userId = UUID.randomUUID().toString();
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    user.setId(userId);

    // set attributes that should be returned
    final List<String> attributeNames = new ArrayList<>(Arrays.asList("userName", "displayName", "externalId"));
    ResourceType userResourceType = resourceTypeFactory.getResourceType(EndpointPaths.USERS);
    final String attributes = String.join(",", attributeNames);
    List<SchemaAttribute> attributeList = RequestUtils.getAttributes(userResourceType, attributes);


    // add meta to bypass schema validation
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    user.setMeta(meta);
    userHandler.getInMemoryMap().put(user.getId().get(), user);

    final int expectedNumberOfAttributesToBeReturned = 5;
    MatcherAssert.assertThat(user.size(), Matchers.greaterThan(expectedNumberOfAttributesToBeReturned));

    ScimResponse scimResponse = resourceEndpointHandler.getResource("/Users",
                                                                    userId,
                                                                    attributes,
                                                                    null,
                                                                    null,
                                                                    getBaseUrlSupplier(),
                                                                    null);
    Mockito.verify(userHandler)
           .getResource(Mockito.eq(userId),
                        Mockito.isNull(),
                        Mockito.eq(attributeList),
                        Mockito.eq(Collections.emptyList()), Mockito.isNull());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    GetResponse getResponse = (GetResponse)scimResponse;
    Assertions.assertEquals(expectedNumberOfAttributesToBeReturned, getResponse.size());

    List<String> returnedAttributeNames = new ArrayList<>();
    getResponse.fieldNames().forEachRemaining(returnedAttributeNames::add);
    MatcherAssert.assertThat(returnedAttributeNames,
                             Matchers.containsInAnyOrder(Matchers.equalTo("schemas"),
                                                         Matchers.equalTo("id"),
                                                         Matchers.equalTo("externalId"),
                                                         Matchers.equalTo("userName"),
                                                         Matchers.equalTo("displayName")));
  }

  /**
   * verifies that the attribute parameter is handed over correctly to the {@link ResourceHandler}
   */
  @Test
  public void testGetResourceWithExcludedAttributesParameter()
  {
    final String userId = UUID.randomUUID().toString();
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    user.setId(userId);
    user.remove(AttributeNames.RFC7643.PASSWORD); // a read only attribute so we remove it before hand

    // set attributes that should be returned
    final List<String> excludedAttributeNames = new ArrayList<>(Arrays.asList("userName", "displayName", "externalId"));
    ResourceType userResourceType = resourceTypeFactory.getResourceType(EndpointPaths.USERS);
    final String excludedAttributes = String.join(",", excludedAttributeNames);
    List<SchemaAttribute> excludedAttributeList = RequestUtils.getAttributes(userResourceType, excludedAttributes);

    // add meta to bypass schema validation
    Meta meta = Meta.builder().created(Instant.now()).lastModified(Instant.now()).build();
    user.setMeta(meta);
    userHandler.getInMemoryMap().put(user.getId().get(), user);

    ScimResponse scimResponse = resourceEndpointHandler.getResource("/Users",
                                                                    userId,
                                                                    null,
                                                                    excludedAttributes,
                                                                    null,
                                                                    getBaseUrlSupplier(),
                                                                    null);
    Mockito.verify(userHandler)
           .getResource(Mockito.eq(userId),
                        Mockito.isNull(),
                        Mockito.eq(Collections.emptyList()),
                        Mockito.eq(excludedAttributeList), Mockito.isNull());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    GetResponse getResponse = (GetResponse)scimResponse;

    final int expectedNumberOfAttributesToBeReturned = user.size() - excludedAttributeNames.size();
    Assertions.assertEquals(expectedNumberOfAttributesToBeReturned, getResponse.size());

    List<String> returnedAttributeNames = new ArrayList<>();
    getResponse.fieldNames().forEachRemaining(returnedAttributeNames::add);
    MatcherAssert.assertThat(returnedAttributeNames,
                             Matchers.not(Matchers.containsInAnyOrder(excludedAttributeNames.stream()
                                                                                            .map(Matchers::equalTo)
                                                                                            .collect(Collectors.toList()))));
  }

  /**
   * if no resource is returned after update a {@link ResourceNotFoundException} should be thrown
   */
  @Test
  public void testSendUnusableResourceToUpdate()
  {
    User user = User.builder().id(UUID.randomUUID().toString()).userName("goldfish").build();
    ScimResponse scimResponse = resourceEndpointHandler.updateResource("/Users",
                                                                       UUID.randomUUID().toString(),
                                                                       user.toString(),
                                                                       null,
                                                                       getBaseUrlSupplier(),
                                                                       null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(ResourceNotFoundException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.NOT_FOUND, errorResponse.getHttpStatus());
  }

  /**
   * if no resource is returned after update a {@link ResourceNotFoundException} should be thrown
   */
  @Test
  public void testDoNotReturnResourceAfterUpdate()
  {
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    Mockito.doReturn(null).when(userHandler).updateResource(Mockito.any(), Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.updateResource("/Users",
                                                                       UUID.randomUUID().toString(),
                                                                       user.toString(),
                                                                       null,
                                                                       getBaseUrlSupplier(),
                                                                       null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(ResourceNotFoundException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.NOT_FOUND, errorResponse.getHttpStatus());
  }

  /**
   * if the returned resource by the getResource endpoint has no id an {@link InternalServerException} must be
   * thrown
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "123456"})
  public void testUpdateResourceWithReturnedResourceHasDifferentId(String id)
  {
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    Mockito.doReturn(JsonHelper.copyResourceToObject(user.deepCopy(), User.class))
           .when(userHandler)
           .updateResource(Mockito.any(), Mockito.isNull());
    user.setId(null);
    ScimResponse scimResponse = resourceEndpointHandler.updateResource("/Users",
                                                                       id,
                                                                       user.toString(),
                                                                       null,
                                                                       getBaseUrlSupplier(),
                                                                       null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(InternalServerException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorResponse.getHttpStatus());
  }

  /**
   * will show that a {@link ScimException} is correctly handled by the
   * {@link ResourceEndpointHandler#updateResource(String, String, String, Map, Supplier, Authorization)} method
   */
  @Test
  public void testThrowScimExceptionOnUpdateResource()
  {
    ResourceNotFoundException exception = new ResourceNotFoundException("blubb", null, null);
    Mockito.doThrow(exception).when(userHandler).updateResource(Mockito.any(), Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.updateResource("/Users",
                                                                       "123456",
                                                                       readResourceFile(USER_RESOURCE),
                                                                       null,
                                                                       null,
                                                                       null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(ResourceNotFoundException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.NOT_FOUND, errorResponse.getHttpStatus());
  }

  /**
   * will show that a {@link RuntimeException} is correctly handled by the
   * {@link ResourceEndpointHandler#updateResource(String, String, String, Map, Supplier, Authorization)} method
   */
  @Test
  public void testThrowRuntimeExceptionOnUpdateResource()
  {
    RuntimeException exception = new RuntimeException("blubb");
    Mockito.doThrow(exception).when(userHandler).updateResource(Mockito.any(), Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.updateResource("/Users",
                                                                       "123456",
                                                                       readResourceFile(USER_RESOURCE),
                                                                       null,
                                                                       null,
                                                                       null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(InternalServerException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorResponse.getHttpStatus());
  }

  /**
   * will show that a {@link ScimException} is correctly handled by the
   * {@link ResourceEndpointHandler#deleteResource(String, String)} method
   */
  @Test
  public void testThrowScimExceptionOnDeleteResource()
  {
    ResourceNotFoundException exception = new ResourceNotFoundException("blubb", null, null);
    Mockito.doThrow(exception).when(userHandler).deleteResource(Mockito.any(), Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.deleteResource("/Users",
                                                                       "123456",
                                                                       Collections.emptyMap(),
                                                                       null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(ResourceNotFoundException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.NOT_FOUND, errorResponse.getHttpStatus());
  }

  /**
   * will show that a {@link BadRequestException} is thrown if the parameters attributes and excludedAttributes
   * are set at the same time on get request
   */
  @Test
  public void testThrowBadRequestIfAttributeAndExcludedAttribtesAreSetOnGet()
  {
    ScimResponse scimResponse = resourceEndpointHandler.getResource("/Users",
                                                                    "123456",
                                                                    "userName",
                                                                    "name",
                                                                    null,
                                                                    getBaseUrlSupplier(),
                                                                    null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(BadRequestException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getHttpStatus());
    Assertions.assertEquals(ScimType.Custom.INVALID_PARAMETERS, errorResponse.getScimException().getScimType());
  }

  /**
   * will show that a {@link BadRequestException} is thrown if the parameters attributes and excludedAttributes
   * are set at the same time on list request
   */
  @Test
  public void testThrowBadRequestIfAttributeAndExcludedAttribtesAreSetOnList()
  {
    ScimResponse scimResponse = resourceEndpointHandler.listResources("/Users",
                                                                      1L,
                                                                      50,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      "displayName",
                                                                      "name",
                                                                      getBaseUrlSupplier(),
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(BadRequestException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getHttpStatus());
    Assertions.assertEquals(ScimType.Custom.INVALID_PARAMETERS, errorResponse.getScimException().getScimType());
  }

  /**
   * will show that the excludedAttributes parameter is also correctly used on extensions
   */
  @Test
  public void testGetResourceWithExcludedExtensionAttribute()
  {
    User user = JsonHelper.loadJsonDocument(USER_RESOURCE, User.class);
    user.setEnterpriseUser(EnterpriseUser.builder().costCenter("costCenter").department("department").build());
    user.setMeta(Meta.builder().created(Instant.now()).lastModified(Instant.now()).build());
    userHandler.getInMemoryMap().put(user.getId().get(), user);
    ScimResponse scimResponse = resourceEndpointHandler.getResource("/Users",
                                                                    user.getId().get(),
                                                                    null,
                                                                    "costCenter",
                                                                    null,
                                                                    getBaseUrlSupplier(),
                                                                    null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    GetResponse getResponse = (GetResponse)scimResponse;
    User returnedUser = JsonHelper.copyResourceToObject(getResponse, User.class);
    Assertions.assertTrue(returnedUser.getEnterpriseUser().isPresent());
    Assertions.assertTrue(returnedUser.getEnterpriseUser().get().getDepartment().isPresent());
    Assertions.assertFalse(returnedUser.getEnterpriseUser().get().getCostCenter().isPresent());
  }

  /**
   * will show that a {@link RuntimeException} is correctly handled by the
   * {@link ResourceEndpointHandler#deleteResource(String, String)} method
   */
  @Test
  public void testThrowRuntimeExceptionOnDeleteResource()
  {
    RuntimeException exception = new RuntimeException("blubb");
    Mockito.doThrow(exception).when(userHandler).deleteResource(Mockito.any(), Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.deleteResource("/Users",
                                                                       "123456",
                                                                       Collections.emptyMap(),
                                                                       null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(InternalServerException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorResponse.getHttpStatus());
  }

  /**
   * tests that if count is set to 1 that only a single entry is returned no matter which startIndex is used.
   * This test depends on at least 4 registered resource types otherwise this test will get
   * {@link BadRequestException}s
   */
  @ParameterizedTest
  @ValueSource(longs = {-1, 0, 1, 2, 3, 4})
  public void testListResourceTypesWithStartIndex(long startIndex)
  {
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.RESOURCE_TYPES,
                                                                      SearchRequest.builder()
                                                                                   .startIndex(startIndex)
                                                                                   .count(1)
                                                                                   .build(),
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, listResponse.getHttpStatus());
    Assertions.assertEquals(resourceTypeFactory.getAllResourceTypes().size(), listResponse.getTotalResults());
    Assertions.assertEquals(1, listResponse.getListedResources().size());
    Assertions.assertEquals(1, listResponse.getItemsPerPage());
  }

  /**
   * get service provider configuration with null value
   */
  @Test
  public void testGetServiceProviderConfiguration()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(true);
    AuthenticationScheme authScheme = AuthenticationScheme.builder()
                                                          .name("Oauth2")
                                                          .description("...")
                                                          .type("...")
                                                          .build();
    resourceEndpointHandler.getServiceProvider().setAuthenticationSchemes(Collections.singletonList(authScheme));
    ScimResponse scimResponse = resourceEndpointHandler.getResource(EndpointPaths.SERVICE_PROVIDER_CONFIG,
                                                                    null,
                                                                    null,
                                                                    null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    GetResponse getResponse = (GetResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, getResponse.getHttpStatus());
    Assertions.assertEquals(EndpointPaths.SERVICE_PROVIDER_CONFIG,
                            getResponse.get(AttributeNames.RFC7643.META)
                                       .get(AttributeNames.RFC7643.LOCATION)
                                       .textValue());
    log.debug(getResponse.toPrettyString());
  }

  /**
   * get service provider configuration with a random id. this must have the same result as with a null value
   */
  @Test
  public void testGetServiceProviderConfigurationWithRandomId()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(true);
    AuthenticationScheme authScheme = AuthenticationScheme.builder()
                                                          .name("Oauth2")
                                                          .description("...")
                                                          .type("...")
                                                          .build();
    resourceEndpointHandler.getServiceProvider().setAuthenticationSchemes(Collections.singletonList(authScheme));
    String randomId = UUID.randomUUID().toString();
    ScimResponse scimResponse = resourceEndpointHandler.getResource(EndpointPaths.SERVICE_PROVIDER_CONFIG,
                                                                    randomId,
                                                                    null,
                                                                    null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    GetResponse getResponse = (GetResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, getResponse.getHttpStatus());
    Assertions.assertEquals(EndpointPaths.SERVICE_PROVIDER_CONFIG,
                            getResponse.get(AttributeNames.RFC7643.META)
                                       .get(AttributeNames.RFC7643.LOCATION)
                                       .textValue());
    log.debug(getResponse.toPrettyString());
  }

  /**
   * get service provider configuration without a authentication scheme. this will result in an internal server
   * error with http status 500
   */
  @Test
  public void testGetServiceProviderConfigurationWithoutAuthScheme()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(true);
    ScimResponse scimResponse = resourceEndpointHandler.getResource(EndpointPaths.SERVICE_PROVIDER_CONFIG,
                                                                    null,
                                                                    null,
                                                                    null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, errorResponse.getHttpStatus());
    Assertions.assertEquals("An internal error has occurred.", errorResponse.getDetail().get());
    log.debug(errorResponse.toPrettyString());
  }

  /**
   * verifies that no results will be returned if the startIndex exceeds the number of results that are
   * available
   */
  @Test
  public void testListResourceTypesWithStartIndexOutOfRange()
  {
    final long startIndex = resourceTypeFactory.getAllResourceTypes().size() + 1;
    ScimResponse scimResponse = Assertions.assertDoesNotThrow(() -> {
      return resourceEndpointHandler.listResources(EndpointPaths.RESOURCE_TYPES,
                                                   SearchRequest.builder().startIndex(startIndex).count(1).build(),
                                                   null,
                                                   null);
    });
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(0, listResponse.getListedResources().size());
    Assertions.assertEquals(resourceTypeFactory.getAllResourceTypes().size(), listResponse.getTotalResults());
  }

  /**
   * this test will check that the implementation is reducing the number of returned entries to the desired
   * number of entries if the developer returned too many
   */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4})
  public void testReturnTooManyEntries(int count)
  {
    final long totalResults = count * 2;
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setMaxResults((int)totalResults);
    List<User> userList = createUsers(totalResults);
    PartialListResponse<User> partialListResponse = PartialListResponse.<User> builder()
                                                                       .totalResults(totalResults)
                                                                       .resources(userList)
                                                                       .build();
    Mockito.doReturn(partialListResponse)
           .when(userHandler)
           .listResources(Mockito.anyInt(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());

    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      count,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(totalResults, listResponse.getTotalResults());
    Assertions.assertEquals(count, listResponse.getItemsPerPage());
    Assertions.assertEquals(1, listResponse.getStartIndex());
    Assertions.assertEquals(count, listResponse.getListedResources().size());
  }

  /**
   * this test will verify that no the sortBy value is always null if the sorting feature is disabled
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "userName", SchemaUris.USER_URI + ":" + "userName", "unknownAttributeName"})
  public void testSortByIfSortDisabled(String sortBy)
  {
    createUsers(1);
    resourceEndpointHandler.getServiceProvider().getSortConfig().setSupported(false);
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      sortBy,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(0),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
  }

  /**
   * this test will verify that no the sortBy value is always null if the sorting feature is disabled
   */
  @ParameterizedTest
  @ValueSource(strings = {"userName", SchemaUris.USER_URI + ":" + "userName"})
  public void testSortByIfSortEnabled(String sortBy)
  {
    createUsers(1);
    resourceEndpointHandler.getServiceProvider().getSortConfig().setSupported(true);
    resourceTypeFactory.getResourceType(EndpointPaths.USERS).getFeatures().setAutoSorting(false);

    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      sortBy,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ResourceType resourceType = resourceTypeFactory.getResourceType(EndpointPaths.USERS);
    SchemaAttribute sortByAttribute = RequestUtils.getSchemaAttributeByAttributeName(resourceType, sortBy);
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(0),
                          Mockito.isNull(),
                          Mockito.eq(sortByAttribute),
                          Mockito.eq(SortOrder.ASCENDING),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.eq(Collections.emptyList()),
                          Mockito.isNull());
  }

  /**
   * this test will verify that an exception is thrown if the sortBy value is unknown by the resource type
   */
  @Test
  public void testSortByIfSortIsEnabledWithUnknownAttribute()
  {
    resourceEndpointHandler.getServiceProvider().getSortConfig().setSupported(true);
    String sortBy = "unknownAttributeName";
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      sortBy,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(BadRequestException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(ScimType.Custom.INVALID_PARAMETERS, errorResponse.getScimException().getScimType());
    MatcherAssert.assertThat(errorResponse.getScimException().getDetail(), Matchers.containsString(sortBy));
  }

  /**
   * this test will verify that the sortOrder value is correctly handled if sorting is disabled. Meaning the
   * result must always be null
   */
  @ParameterizedTest
  @CsvSource({",", "userName,", ",ASCENDING", ",DESCENDING", "userName,ASCENDING", "userName,DESCENDING",})
  public void testSortOrderIfSortDisabled(String sortBy, SortOrder sortOrder)
  {
    resourceEndpointHandler.getServiceProvider().getSortConfig().setSupported(false);
    final String sortOrderString = sortOrder == null ? null : sortOrder.name().toLowerCase();
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      sortBy,
                                                                      sortOrderString,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(0),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
  }

  /**
   * this test will verify that the sortOrder value is correctly handled if sorting is enabled.
   */
  @ParameterizedTest
  @CsvSource({",", "userName,", ",ASCENDING", ",DESCENDING", "userName,ASCENDING", "userName,DESCENDING",})
  public void testSortOrderIfSortEnabled(String sortBy, SortOrder sortOrder)
  {
    resourceEndpointHandler.getServiceProvider().getSortConfig().setSupported(true);
    final String sortOrderString = sortOrder == null ? null : sortOrder.name().toLowerCase();
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      sortBy,
                                                                      sortOrderString,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ResourceType resourceType = resourceTypeFactory.getResourceType(EndpointPaths.USERS);
    SchemaAttribute sortByAttribute = RequestUtils.getSchemaAttributeByAttributeName(resourceType, sortBy);
    SortOrder actualSortOrder = sortOrder == null && sortBy != null ? SortOrder.ASCENDING : sortOrder;
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(0),
                          Mockito.isNull(),
                          Mockito.eq(sortByAttribute),
                          Mockito.eq(actualSortOrder),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
  }

  /**
   * this test will verify that the filter is not parsed if filtering is disabled
   */
  @Test
  public void testFilterIfFilteringDisabled()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(false);
    String filter = "userName eq \"chuck\"";
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      filter,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(0),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
  }

  /**
   * this test will verify that no exception is thrown if filtering is disabled and a filter with an unknown
   * attribute is sent
   */
  @Test
  public void testFilterIfFilteringDisabledWithUnknownAttribute()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(false);
    String filter = "unknownAttributeName eq \"chuck\"";
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      filter,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.not(Matchers.typeCompatibleWith(ErrorResponse.class)));
  }

  /**
   * this test will verify that the filter is parsed if filtering is enabled
   */
  @ParameterizedTest
  @ValueSource(strings = {"userName co \"chu\"", "userName eq \"chuck\"", "userName eq \"5.5\"",
                          "userName eq \"chu\" or name.givenName eq \"Carlos\"",
                          "userName eq \"chu\" and name.givenName eq \"null\"",
                          "userName eq \"chu\" and not( name.givenName eq null )",
                          "((userName eq \"5.5\") and not( name.givenName eq \"Carlos\" OR nickName eq \"blubb\"))",
                          "((userName eQ \"chu\") and not( name.givenName eq \"-6\" or nickName eq \"true\"))",
                          "((userName eq \"false\") and not( name.givenName eq \"6\" or nickName eq \"true\"))",
                          "((userName pR) and not( name.givenName Pr and nickName pr))", "emails.primary eq true",
                          "((userName ne \"false\") and not( name.givenName co \"-6\" or nickName sw \"true\"))",
                          "((userName ew \"false\") and not( name.givenName gt \"6\" or nickName GE \"true\"))",
                          "((userName lt \"false\") and not( name.givenName le \"-6\" or nickName gt \"true\"))",
                          "meta.lastModified ge \"2019-10-17T01:07:00Z\"",
                          "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter eq \"chuck\"",
                          "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value eq \"chuck\"",
                          "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value eq \"5.5\""})
  public void testFilterIfFilteringEnabled(String filter)
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(true);
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      filter,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ResourceType resourceType = resourceTypeFactory.getResourceType(EndpointPaths.USERS);
    FilterNode filterNode = RequestUtils.parseFilter(resourceType, filter);
    Mockito.verify(userHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.eq(0),
                          Mockito.eq(filterNode),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
  }

  /**
   * this test verifies that the framework reacts with an {@link InternalServerException} wrapped in a
   * {@link ErrorResponse} if an exception is thrown in the developer implementation of listResources
   */
  @Test
  public void testThrowNullPointerException()
  {
    Mockito.doThrow(NullPointerException.class)
           .when(userHandler)
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(InternalServerException.class, errorResponse.getScimException().getClass());
  }

  /**
   * verifies that a {@link NotImplementedException} {@link ErrorResponse} is returned if the developer returns
   * null on the
   * {@link ResourceHandler#listResources(long, int, FilterNode, SchemaAttribute, SortOrder, List, List, Authorization)}
   * method
   */
  @Test
  public void testReturnNullInDeveloperImplementationOnListResources()
  {
    Mockito.doReturn(null)
           .when(userHandler)
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.USERS,
                                                                      1L,
                                                                      0,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null,
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(NotImplementedException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, errorResponse.getScimException().getStatus());
  }

  /**
   * verifies that a {@link NotImplementedException} {@link ErrorResponse} is returned if the developer returns
   * null on the {@link ResourceHandler#createResource(ResourceNode, Authorization)} method
   */
  @Test
  public void testReturnNullInDeveloperImplementationOnCreateResource()
  {
    Mockito.doReturn(null).when(userHandler).createResource(Mockito.any(), Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.createResource(EndpointPaths.USERS,
                                                                       readResourceFile(USER_RESOURCE),
                                                                       null,
                                                                       null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(NotImplementedException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, errorResponse.getScimException().getStatus());
  }

  /**
   * verifies that a {@link ResourceNotFoundException} {@link ErrorResponse} is returned if the developer
   * returns null on the {@link ResourceHandler#updateResource(ResourceNode, Authorization)} method
   */
  @Test
  public void testReturnNullInDeveloperImplementationOnUpdateResource()
  {
    Mockito.doReturn(null).when(userHandler).updateResource(Mockito.any(), Mockito.isNull());
    ScimResponse scimResponse = resourceEndpointHandler.updateResource(EndpointPaths.USERS,
                                                                       UUID.randomUUID().toString(),
                                                                       readResourceFile(USER_RESOURCE),
                                                                       null,
                                                                       null,
                                                                       null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(ResourceNotFoundException.class, errorResponse.getScimException().getClass());
    Assertions.assertEquals(HttpStatus.NOT_FOUND, errorResponse.getScimException().getStatus());
  }

  /**
   * the modifier on the the method {@link ResourceEndpointHandler#getServiceProvider()} must be public!
   */
  @Test
  public void testAccessModifierOnServiceProviderGetter() throws NoSuchMethodException
  {
    Method method = ResourceEndpointHandler.class.getMethod("getServiceProvider");
    Assertions.assertTrue(Modifier.isPublic(method.getModifiers()));
  }

  /**
   * verifies that the listResources method will never return more entries than stated in count with count has a
   * value that enforces less than count entries in the last request
   */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  public void testListResourceTypesWithStartIndexAndCount(int count)
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setMaxResults(Integer.MAX_VALUE);
    final int totalResults = resourceTypeFactory.getAllResourceTypes().size();
    for ( int startIndex = 0 ; startIndex < totalResults ; startIndex += count )
    {
      SearchRequest searchRequest = SearchRequest.builder().startIndex(startIndex + 1L).count(count).build();
      ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.RESOURCE_TYPES,
                                                                        searchRequest,
                                                                        null,
                                                                        null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      ListResponse listResponse = (ListResponse)scimResponse;

      MatcherAssert.assertThat(listResponse.getListedResources().size(), Matchers.lessThanOrEqualTo(count));
      Assertions.assertEquals(totalResults, listResponse.getTotalResults());
      Assertions.assertEquals(Math.min(totalResults - startIndex, count), listResponse.getItemsPerPage());
      log.debug("returned entries: {}", listResponse.getListedResources().size());
    }
  }

  /**
   * verifies that the listResources method will never return more entries than stated in count with count has a
   * value that enforces less than count entries in the last request
   */
  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  public void testListResourceTypesWithStartIndexAndCountForSchemas(int count)
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setMaxResults(Integer.MAX_VALUE);
    for ( int startIndex = 0 ; startIndex < resourceTypeFactory.getAllResourceTypes().size() ; startIndex += count )
    {
      SearchRequest searchRequest = SearchRequest.builder().startIndex(startIndex + 1L).count(count).build();
      ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.SCHEMAS,
                                                                        searchRequest,
                                                                        null,
                                                                        null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
      ListResponse listResponse = (ListResponse)scimResponse;
      MatcherAssert.assertThat(listResponse.getListedResources().size(), Matchers.lessThanOrEqualTo(count));

      List<Schema> allSchemas = resourceTypeFactory.getAllResourceTypes()
                                                   .stream()
                                                   .map(ResourceType::getAllSchemas)
                                                   .flatMap(Collection::stream)
                                                   .distinct()
                                                   .collect(Collectors.toList());
      Assertions.assertEquals(allSchemas.size(), listResponse.getTotalResults());
      log.debug("returned entries: {}", listResponse.getListedResources().size());
    }
  }

  /**
   * the test will assert that the filter is not getting parsed if the filter feature is disabled
   */
  @Test
  public void testFilterWithFilteringDisabled()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(false);
    final String filter = "schemaExtensions pr";
    SearchRequest searchRequest = SearchRequest.builder().filter(filter).build();
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.RESOURCE_TYPES,
                                                                      searchRequest,
                                                                      null,
                                                                      null);
    Mockito.verify(resourceTypeHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.anyInt(),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
  }

  /**
   * this test will assert that the {@link FilterNode} is successfully passed to the {@link ResourceHandler} if
   * filtering is enabled and autoFiltering is disabled and that the entries are returned unmodified
   */
  @Test
  public void testFilterWithFilteringOnServiceProviderEnabled()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(true);
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setMaxResults(Integer.MAX_VALUE);
    resourceTypeFactory.getResourceType(EndpointPaths.RESOURCE_TYPES).getFeatures().setAutoFiltering(false);

    final String filter = "schemaExtensions pr";
    SearchRequest searchRequest = SearchRequest.builder().filter(filter).build();
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.RESOURCE_TYPES,
                                                                      searchRequest,
                                                                      null,
                                                                      null);
    ResourceType resourceType = resourceTypeFactory.getResourceType(EndpointPaths.RESOURCE_TYPES);

    Assertions.assertFalse(resourceType.getFeatures().isAutoFiltering());
    FilterNode filterNode = RequestUtils.parseFilter(resourceType, filter);
    Mockito.verify(resourceTypeHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.anyInt(),
                          Mockito.eq(filterNode),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(resourceTypeFactory.getAllResourceTypes().size(), listResponse.getListedResources().size());
  }

  /**
   * this test will assure if autoFiltering is enabled that the filtering is executed successfully on the
   * returned resources
   */
  @Test
  public void testFilterWithFilteringOnServiceProviderAndResourceTypeEnabled()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(true);
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setMaxResults(Integer.MAX_VALUE);
    final String filter = "schemaExtensions pr";
    SearchRequest searchRequest = SearchRequest.builder().filter(filter).build();
    ResourceType resourceType = resourceTypeFactory.getResourceType(EndpointPaths.RESOURCE_TYPES);
    resourceType.setFeatures(ResourceTypeFeatures.builder().autoFiltering(true).build());

    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.RESOURCE_TYPES,
                                                                      searchRequest,
                                                                      null,
                                                                      null);

    Mockito.verify(resourceTypeHandler, Mockito.times(1))
           .listResources(Mockito.eq(1L),
                          Mockito.anyInt(),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    ListResponse listResponse = (ListResponse)scimResponse;
    Collection<ResourceType> resourceTypes = resourceTypeFactory.getAllResourceTypes();

    Assertions.assertNotEquals(resourceTypes.size(), listResponse.getListedResources().size());
    Assertions.assertEquals(resourceTypes.stream().filter(rt -> rt.getSchemaExtensions().size() > 0).count(),
                            listResponse.getListedResources().size());
  }

  /**
   * the {@link ResourceEndpointHandler#registerEndpoint(EndpointDefinition)} method must be accessible for
   * developers
   */
  @Test
  public void testRegisterEndpointMustBePublic() throws NoSuchMethodException
  {
    Method method = ResourceEndpointHandler.class.getMethod("registerEndpoint", EndpointDefinition.class);
    Assertions.assertTrue(Modifier.isPublic(method.getModifiers()));
  }

  /**
   * the {@link ResourceEndpointHandler#getServiceProvider()} method must be accessible for developers
   */
  @Test
  public void testGetServiceProviderMustBePublic() throws NoSuchMethodException
  {
    Method method = ResourceEndpointHandler.class.getMethod("getServiceProvider");
    Assertions.assertTrue(Modifier.isPublic(method.getModifiers()));
  }

  /**
   * Verifies that a {@link BadRequestException} is thrown if the request body is empty on create
   */
  @Test
  public void testCreateUserWithEmptyRequestBody()
  {
    final Supplier<String> baseUrl = () -> "https://localhost/scim/v2";
    ScimResponse scimResponse = Assertions.assertDoesNotThrow(() -> {
      return resourceEndpointHandler.createResource(EndpointPaths.USERS, null, baseUrl, null);
    });
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getHttpStatus());
  }

  /**
   * Verifies that a {@link BadRequestException} is thrown if the request body is invalid on create
   */
  @Test
  public void testCreateUserWithInvalidRequestBodyContent()
  {
    final Supplier<String> baseUrl = () -> "https://localhost/scim/v2";
    final String invalidRequestBody = "<root>invalid</root>";
    ScimResponse scimResponse = Assertions.assertDoesNotThrow(() -> {
      return resourceEndpointHandler.createResource(EndpointPaths.USERS, invalidRequestBody, baseUrl, null);
    });
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getHttpStatus());
  }

  /**
   * Verifies that a {@link BadRequestException} is thrown if the request body is empty on update
   */
  @Test
  public void testUpdateUserWithEmptyRequestBody()
  {
    final Supplier<String> baseUrl = () -> "https://localhost/scim/v2";
    ScimResponse scimResponse = Assertions.assertDoesNotThrow(() -> {
      return resourceEndpointHandler.updateResource(EndpointPaths.USERS, "123456", null, null, baseUrl, null);
    });
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getHttpStatus());
  }

  /**
   * Verifies that a {@link BadRequestException} is thrown if the request body is invalid on update
   */
  @Test
  public void testUpdateUserWithInvalidRequestBodyContent()
  {
    final Supplier<String> baseUrl = () -> "https://localhost/scim/v2";
    final String invalidRequestBody = "<root>invalid</root>";
    ScimResponse scimResponse = Assertions.assertDoesNotThrow(() -> {
      return resourceEndpointHandler.updateResource(EndpointPaths.USERS,
                                                    "123456",
                                                    invalidRequestBody,
                                                    null,
                                                    baseUrl,
                                                    null);
    });
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    MatcherAssert.assertThat(errorResponse.getScimException().getClass(),
                             Matchers.typeCompatibleWith(BadRequestException.class));
    Assertions.assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getHttpStatus());
  }

  /**
   * Verifies that a resource can successfully be patched
   */
  @Test
  public void testPatchResource()
  {
    resourceEndpointHandler.getServiceProvider().getPatchConfig().setSupported(true);
    final Supplier<String> baseUrl = () -> "https://localhost/scim/v2";
    final String path = "name";
    Name name = Name.builder().givenName("goldfish").familyName("captain").build();
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .valueNode(name)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    Meta meta = Meta.builder()
                    .resourceType(ResourceTypeNames.USER)
                    .created(LocalDateTime.now())
                    .lastModified(LocalDateTime.now())
                    .build();
    String id = UUID.randomUUID().toString();
    User user = User.builder().id(id).userName("goldfish").nickName("captain").meta(meta).build();
    userHandler.getInMemoryMap().put(id, user);

    ScimResponse scimResponse = Assertions.assertDoesNotThrow(() -> {
      return resourceEndpointHandler.patchResource(EndpointPaths.USERS,
                                                   id,
                                                   patchOpRequest.toString(),
                                                   Collections.emptyMap(),
                                                   baseUrl);
    });
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    UpdateResponse updateResponse = (UpdateResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.OK, updateResponse.getHttpStatus());
    User copiedUser = JsonHelper.copyResourceToObject(user.deepCopy(), User.class);
    copiedUser.setName(name);
    Assertions.assertEquals(copiedUser, updateResponse);

    GetResponse getResponse = (GetResponse)resourceEndpointHandler.getResource(EndpointPaths.USERS, id, null, baseUrl);
    Assertions.assertEquals(updateResponse, getResponse);
    Assertions.assertEquals(copiedUser, getResponse);
  }

  /**
   * Verifies that a {@link NotImplementedException} is thrown if the service provider has support for patch
   * deactivated
   */
  @Test
  public void testPatchResourceWithoutServiceProviderSupport()
  {
    resourceEndpointHandler.getServiceProvider().getPatchConfig().setSupported(false);
    final Supplier<String> baseUrl = () -> "https://localhost/scim/v2";
    final String path = "name";
    Name name = Name.builder().givenName("goldfish").familyName("captain").build();
    List<PatchRequestOperation> operations = Arrays.asList(PatchRequestOperation.builder()
                                                                                .op(PatchOp.ADD)
                                                                                .path(path)
                                                                                .valueNode(name)
                                                                                .build());
    PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(operations).build();

    ScimResponse scimResponse = Assertions.assertDoesNotThrow(() -> {
      return resourceEndpointHandler.patchResource(EndpointPaths.USERS,
                                                   "123456",
                                                   patchOpRequest.toString(),
                                                   Collections.emptyMap(),
                                                   baseUrl);
    });
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
    ErrorResponse errorResponse = (ErrorResponse)scimResponse;
    Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, errorResponse.getHttpStatus());
  }

  /**
   * this test will verify that the schemas endpoint is using autoFiltering by default
   */
  @Test
  public void testSchemasEndpointUsesAutoFiltering()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setSupported(true);
    Assertions.assertTrue(resourceTypeFactory.getResourceType(EndpointPaths.SCHEMAS).getFeatures().isAutoFiltering());
    String filter = "id ew \"ServiceProviderConfig\"";
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.SCHEMAS,
                                                                      SearchRequest.builder().filter(filter).build(),
                                                                      getBaseUrlSupplier(),
                                                                      null);
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    Assertions.assertEquals(HttpStatus.OK, scimResponse.getHttpStatus());
    ListResponse listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(1, listResponse.getTotalResults());
    Assertions.assertEquals(1, listResponse.getListedResources().size());
    Assertions.assertEquals(1, listResponse.getItemsPerPage());
    log.debug(listResponse.toPrettyString());
  }


  /**
   * this test will verify that the autoSorting attribute is set by default on the resource types endpoint and
   * the schemas endpoint
   */
  @Test
  public void testEndpointsWithDefaultAutoSortingTrue()
  {
    resourceEndpointHandler.getServiceProvider().getSortConfig().setSupported(true);
    Assertions.assertTrue(resourceTypeFactory.getResourceType(EndpointPaths.SCHEMAS).getFeatures().isAutoSorting());
    Assertions.assertTrue(resourceTypeFactory.getResourceType(EndpointPaths.RESOURCE_TYPES)
                                             .getFeatures()
                                             .isAutoSorting());
  }


  /**
   * this test will verify that the autoSorting attribute is considered in the {@link ResourceEndpointHandler}.
   * if autoSorting is set to true the developer should not longer get any data about the sorting information
   * meaning the attributes must be null.
   */
  @Test
  public void testAutoSortingIsUsedInHandler()
  {
    resourceEndpointHandler.getServiceProvider().getFilterConfig().setMaxResults(50);
    resourceEndpointHandler.getServiceProvider().getSortConfig().setSupported(true);
    resourceTypeFactory.getResourceType(EndpointPaths.RESOURCE_TYPES).getFeatures().setAutoSorting(true);

    String sortAttribute = "name";
    ScimResponse scimResponse = resourceEndpointHandler.listResources(EndpointPaths.RESOURCE_TYPES,
                                                                      SearchRequest.builder()
                                                                                   .sortBy(sortAttribute)
                                                                                   .build(),
                                                                      getBaseUrlSupplier(),
                                                                      null);
    Mockito.verify(resourceTypeHandler, Mockito.times(1))
           .listResources(Mockito.anyLong(),
                          Mockito.anyInt(),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.isNull(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.isNull());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ListResponse.class));
    Assertions.assertEquals(HttpStatus.OK, scimResponse.getHttpStatus());
    ListResponse<ScimObjectNode> listResponse = (ListResponse)scimResponse;
    Assertions.assertEquals(5, listResponse.getListedResources().size(), listResponse.toPrettyString());
    MatcherAssert.assertThat(listResponse.getListedResources()
                                         .stream()
                                         .map(node -> node.get("name").textValue())
                                         .collect(Collectors.toList()),
                             Matchers.contains(resourceTypeFactory.getResourceType(EndpointPaths.GROUPS).getName(),
                                               resourceTypeFactory.getResourceType(EndpointPaths.RESOURCE_TYPES)
                                                                  .getName(),
                                               resourceTypeFactory.getResourceType(EndpointPaths.SCHEMAS).getName(),
                                               resourceTypeFactory.getResourceType(EndpointPaths.SERVICE_PROVIDER_CONFIG)
                                                                  .getName(),
                                               resourceTypeFactory.getResourceType(EndpointPaths.USERS).getName()));
  }

  /**
   * verifies that the eTag support is executed on the different endpoints
   */
  @TestFactory
  public List<DynamicTest> testEtagSupportWorksCorrectlyOnAllEndpoint()
  {
    User createdUser;
    {
      resourceEndpointHandler.getServiceProvider().getETagConfig().setSupported(true);
      resourceEndpointHandler.getServiceProvider().getPatchConfig().setSupported(true);
      ResourceType userResourceType = resourceTypeFactory.getResourceTypeByName(ResourceTypeNames.USER).get();
      userResourceType.getFeatures().getETagFeature().setEnabled(true);

      User user = User.builder().userName("goldfish").build();
      ScimResponse scimResponse = resourceEndpointHandler.createResource(EndpointPaths.USERS,
                                                                         user.toString(),
                                                                         getBaseUrlSupplier(),
                                                                         null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
      CreateResponse createResponse = (CreateResponse)scimResponse;
      createdUser = JsonHelper.copyResourceToObject(createResponse, User.class);
      Assertions.assertTrue(createdUser.getMeta().isPresent());
      Assertions.assertTrue(createdUser.getMeta().get().getVersion().isPresent());
      Assertions.assertTrue(createdUser.getMeta().get().getVersion().get().isWeak());
    }

    List<DynamicTest> dynamicTests = new ArrayList<>();
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("get resource with If-Match", () -> {
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_MATCH_HEADER, createdUser.getMeta().get().getVersion().get().getEntityTag());
      ScimResponse scimResponse = resourceEndpointHandler.getResource(EndpointPaths.USERS,
                                                                      createdUser.getId().get(),
                                                                      httpHeaders,
                                                                      getBaseUrlSupplier());
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
      GetResponse getResponse = (GetResponse)scimResponse;
      User user = JsonHelper.copyResourceToObject(getResponse, User.class);
      Assertions.assertEquals(createdUser.getMeta().get().getVersion().get(), user.getMeta().get().getVersion().get());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("get resource with non matching If-Match", () -> {
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_MATCH_HEADER, ETag.builder().tag("123456").build().getEntityTag());
      ScimResponse scimResponse = resourceEndpointHandler.getResource(EndpointPaths.USERS,
                                                                      createdUser.getId().get(),
                                                                      httpHeaders,
                                                                      getBaseUrlSupplier());
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      Assertions.assertEquals(HttpStatus.PRECONDITION_FAILED, errorResponse.getStatus());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("get resource with If-None-Match", () -> {
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER, createdUser.getMeta().get().getVersion().get().getEntityTag());
      ScimResponse scimResponse = resourceEndpointHandler.getResource(EndpointPaths.USERS,
                                                                      createdUser.getId().get(),
                                                                      httpHeaders,
                                                                      getBaseUrlSupplier());
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      Assertions.assertEquals(HttpStatus.NOT_MODIFIED, errorResponse.getStatus());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("get resource with non matching If-None-Match", () -> {
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER, ETag.builder().tag("123456").build().getEntityTag());
      ScimResponse scimResponse = resourceEndpointHandler.getResource(EndpointPaths.USERS,
                                                                      createdUser.getId().get(),
                                                                      httpHeaders,
                                                                      getBaseUrlSupplier());
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
      GetResponse getResponse = (GetResponse)scimResponse;
      Assertions.assertEquals(createdUser, getResponse);
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("update resource with If-Match", () -> {
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_MATCH_HEADER, createdUser.getMeta().get().getVersion().get().getEntityTag());
      User user = User.builder().userName(createdUser.getUserName().get()).nickName("happy").build();
      ScimResponse scimResponse = resourceEndpointHandler.updateResource(EndpointPaths.USERS,
                                                                         createdUser.getId().get(),
                                                                         user.toString(),
                                                                         httpHeaders,
                                                                         getBaseUrlSupplier(),
                                                                         null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      UpdateResponse updateResponse = (UpdateResponse)scimResponse;
      Assertions.assertNotEquals(createdUser, updateResponse);
      user = JsonHelper.copyResourceToObject(updateResponse, User.class);
      createdUser.setNickName(user.getNickName().get());
      createdUser.getMeta().get().setVersion(user.getMeta().get().getVersion().get());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("update resource with non matching If-Match", () -> {
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_MATCH_HEADER, ETag.builder().tag("123456").build().getEntityTag());
      User user = User.builder().userName(createdUser.getUserName().get()).nickName("happy").build();
      ScimResponse scimResponse = resourceEndpointHandler.updateResource(EndpointPaths.USERS,
                                                                         createdUser.getId().get(),
                                                                         user.toString(),
                                                                         httpHeaders,
                                                                         getBaseUrlSupplier(),
                                                                         null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      Assertions.assertEquals(HttpStatus.PRECONDITION_FAILED, errorResponse.getStatus());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("update resource with If-None-Match", () -> {
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER, createdUser.getMeta().get().getVersion().get().getEntityTag());
      User user = User.builder().userName(createdUser.getUserName().get()).nickName("happy").build();
      ScimResponse scimResponse = resourceEndpointHandler.updateResource(EndpointPaths.USERS,
                                                                         createdUser.getId().get(),
                                                                         user.toString(),
                                                                         httpHeaders,
                                                                         getBaseUrlSupplier(),
                                                                         null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      Assertions.assertEquals(HttpStatus.NOT_MODIFIED, errorResponse.getStatus());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("update resource with non matching If-None-Match", () -> {
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER, ETag.builder().tag("123456").build().getEntityTag());
      User user = User.builder().userName(createdUser.getUserName().get()).nickName("bloody mary").build();
      ScimResponse scimResponse = resourceEndpointHandler.updateResource(EndpointPaths.USERS,
                                                                         createdUser.getId().get(),
                                                                         user.toString(),
                                                                         httpHeaders,
                                                                         getBaseUrlSupplier(),
                                                                         null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      UpdateResponse updateResponse = (UpdateResponse)scimResponse;
      Assertions.assertNotEquals(createdUser, updateResponse);
      user = JsonHelper.copyResourceToObject(updateResponse, User.class);
      createdUser.setNickName(user.getNickName().get());
      createdUser.getMeta().get().setVersion(user.getMeta().get().getVersion().get());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("patch resource with If-Match", () -> {
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_MATCH_HEADER, createdUser.getMeta().get().getVersion().get().getEntityTag());
      PatchRequestOperation operation = PatchRequestOperation.builder().path("nickname").op(PatchOp.REMOVE).build();
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(Collections.singletonList(operation)).build();
      ScimResponse scimResponse = resourceEndpointHandler.patchResource(EndpointPaths.USERS,
                                                                        createdUser.getId().get(),
                                                                        patchOpRequest.toString(),
                                                                        httpHeaders,
                                                                        getBaseUrlSupplier());
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      UpdateResponse updateResponse = (UpdateResponse)scimResponse;
      Assertions.assertNotEquals(createdUser, updateResponse);
      User user = JsonHelper.copyResourceToObject(updateResponse, User.class);
      createdUser.setNickName(user.getNickName().orElse(null));
      Assertions.assertFalse(createdUser.getNickName().isPresent());
      createdUser.getMeta().get().setVersion(user.getMeta().get().getVersion().get());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("patch resource with non matching If-Match", () -> {
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_MATCH_HEADER, ETag.builder().tag("123456").build().getEntityTag());
      List<String> values = Arrays.asList("chucky");
      PatchRequestOperation operation = PatchRequestOperation.builder()
                                                             .path("nickname")
                                                             .op(PatchOp.ADD)
                                                             .values(values)
                                                             .build();
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(Collections.singletonList(operation)).build();
      ScimResponse scimResponse = resourceEndpointHandler.patchResource(EndpointPaths.USERS,
                                                                        createdUser.getId().get(),
                                                                        patchOpRequest.toString(),
                                                                        httpHeaders,
                                                                        getBaseUrlSupplier());
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      Assertions.assertEquals(HttpStatus.PRECONDITION_FAILED, errorResponse.getStatus());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("patch resource with If-None-Match", () -> {
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER, createdUser.getMeta().get().getVersion().get().getEntityTag());
      List<String> values = Arrays.asList("chucky");
      PatchRequestOperation operation = PatchRequestOperation.builder()
                                                             .path("nickname")
                                                             .op(PatchOp.ADD)
                                                             .values(values)
                                                             .build();
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(Collections.singletonList(operation)).build();
      ScimResponse scimResponse = resourceEndpointHandler.patchResource(EndpointPaths.USERS,
                                                                        createdUser.getId().get(),
                                                                        patchOpRequest.toString(),
                                                                        httpHeaders,
                                                                        getBaseUrlSupplier());
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      Assertions.assertEquals(HttpStatus.NOT_MODIFIED, errorResponse.getStatus());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("patch resource with non matching If-None-Match", () -> {
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER, ETag.builder().tag("123456").build().getEntityTag());
      List<String> values = Arrays.asList("chucky");
      PatchRequestOperation operation = PatchRequestOperation.builder()
                                                             .path("nickname")
                                                             .op(PatchOp.ADD)
                                                             .values(values)
                                                             .build();
      PatchOpRequest patchOpRequest = PatchOpRequest.builder().operations(Collections.singletonList(operation)).build();
      ScimResponse scimResponse = resourceEndpointHandler.patchResource(EndpointPaths.USERS,
                                                                        createdUser.getId().get(),
                                                                        patchOpRequest.toString(),
                                                                        httpHeaders,
                                                                        getBaseUrlSupplier());
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
      UpdateResponse updateResponse = (UpdateResponse)scimResponse;
      Assertions.assertNotEquals(createdUser, updateResponse);
      User user = JsonHelper.copyResourceToObject(updateResponse, User.class);
      createdUser.setNickName(user.getNickName().orElse(null));
      Assertions.assertTrue(createdUser.getNickName().isPresent());
      createdUser.getMeta().get().setVersion(user.getMeta().get().getVersion().get());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("delete resource with If-Match", () -> {
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_MATCH_HEADER, createdUser.getMeta().get().getVersion().get().getEntityTag());
      ScimResponse scimResponse = resourceEndpointHandler.deleteResource(EndpointPaths.USERS,
                                                                         createdUser.getId().get(),
                                                                         httpHeaders,
                                                                         null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
      DeleteResponse deleteResponse = (DeleteResponse)scimResponse;
      Assertions.assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getHttpStatus());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("delete resource with non matching If-Match", () -> {
      userHandler.getInMemoryMap().put(createdUser.getId().get(), createdUser);
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_MATCH_HEADER, ETag.builder().tag("123456").build().getEntityTag());
      ScimResponse scimResponse = resourceEndpointHandler.deleteResource(EndpointPaths.USERS,
                                                                         createdUser.getId().get(),
                                                                         httpHeaders,
                                                                         null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      Assertions.assertEquals(HttpStatus.PRECONDITION_FAILED, errorResponse.getStatus());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("delete resource with If-None-Match", () -> {
      userHandler.getInMemoryMap().put(createdUser.getId().get(), createdUser);
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER, createdUser.getMeta().get().getVersion().get().getEntityTag());
      ScimResponse scimResponse = resourceEndpointHandler.deleteResource(EndpointPaths.USERS,
                                                                         createdUser.getId().get(),
                                                                         httpHeaders,
                                                                         null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(ErrorResponse.class));
      ErrorResponse errorResponse = (ErrorResponse)scimResponse;
      Assertions.assertEquals(HttpStatus.NOT_MODIFIED, errorResponse.getStatus());
    }));
    /* ************************************************************************************************************/
    dynamicTests.add(DynamicTest.dynamicTest("delete resource with non matching If-None-Match", () -> {
      userHandler.getInMemoryMap().put(createdUser.getId().get(), createdUser);
      Map<String, String> httpHeaders = new HashMap<>();
      httpHeaders.put(HttpHeader.IF_NONE_MATCH_HEADER, ETag.builder().tag("123456").build().getEntityTag());
      ScimResponse scimResponse = resourceEndpointHandler.deleteResource(EndpointPaths.USERS,
                                                                         createdUser.getId().get(),
                                                                         httpHeaders,
                                                                         null);
      MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(DeleteResponse.class));
      DeleteResponse deleteResponse = (DeleteResponse)scimResponse;
      Assertions.assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getHttpStatus());
    }));
    /* ************************************************************************************************************/

    return dynamicTests;
  }

  /**
   * this test will show that resource reference links are set into the $ref attribute if enough information to
   * determine the correct resource is present
   */
  @Test
  public void testResourceReferencesAreSetAutomaticallyIfPossible()
  {
    String userId;
    // first of all create a group and a user that is a member of this group
    {
      User chuck = User.builder().userName("chuck_norris").active(true).build();
      ScimResponse createUserResponse = resourceEndpointHandler.createResource(EndpointPaths.USERS,
                                                                               chuck.toString(),
                                                                               getBaseUrlSupplier(),
                                                                               null);
      MatcherAssert.assertThat(createUserResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
      chuck = JsonHelper.copyResourceToObject(createUserResponse, User.class);
      userId = chuck.getId().get();
    }


    // now we created a user and we will create a group that has this user set as a member. The response must
    // contain the $ref value with a fully qualified url to the user resource
    Group adminGroup = Group.builder()
                            .displayName("admin")
                            .members(Arrays.asList(Member.builder().value(userId).type(ResourceTypeNames.USER).build()))
                            .build();
    ScimResponse createGroupResponse = resourceEndpointHandler.createResource(EndpointPaths.GROUPS,
                                                                              adminGroup.toString(),
                                                                              getBaseUrlSupplier(),
                                                                              null);
    MatcherAssert.assertThat(createGroupResponse.getClass(), Matchers.typeCompatibleWith(CreateResponse.class));
    adminGroup = JsonHelper.copyResourceToObject(createGroupResponse, Group.class);
    Assertions.assertEquals(1, adminGroup.getMembers().size());
    Assertions.assertEquals(getBaseUrlSupplier().get() + EndpointPaths.USERS + "/" + userId,
                            adminGroup.getMembers().get(0).getRef().orElse(null));
  }

  /**
   * reads a user from the endpoint
   *
   * @param endpoint the resource endpoint that should be used
   * @param userId the id of the user that should be read
   * @return the returned user
   */
  private User getUser(String endpoint, String userId)
  {
    ScimResponse scimResponse = resourceEndpointHandler.getResource(endpoint, userId, null, getBaseUrlSupplier());
    Mockito.verify(userHandler, Mockito.times(1))
           .getResource(Mockito.eq(userId),
                        Mockito.isNull(),
                        Mockito.eq(Collections.emptyList()),
                        Mockito.eq(Collections.emptyList()), Mockito.isNull());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(GetResponse.class));
    Assertions.assertEquals(HttpStatus.OK, scimResponse.getHttpStatus());
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            scimResponse.getHttpHeaders().get(HttpHeader.CONTENT_TYPE_HEADER));
    Assertions.assertNotNull(scimResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    User user = JsonHelper.readJsonDocument(scimResponse.toString(), User.class);
    Meta meta = user.getMeta().get();
    Assertions.assertEquals("User", meta.getResourceType().get());
    Assertions.assertEquals(getLocation(endpoint, userId), meta.getLocation().get());
    Assertions.assertTrue(meta.getCreated().isPresent());
    Assertions.assertTrue(meta.getLastModified().isPresent());
    // TODO check that the last modified value is correct
    return user;
  }

  /**
   * takes the given user updates some values and sends the updated user to the resource endpoint
   *
   * @param endpoint the resource endpoint that should be used
   * @param readUser the resource that will be copied and modified for the update
   * @return the updated user
   */
  private User updateUser(String endpoint, User readUser)
  {
    User updateUser = JsonHelper.copyResourceToObject(readUser.deepCopy(), User.class);
    final String usertype = "newUsertype";
    final String nickname = "newNickname";
    final String title = "newTitle";
    updateUser.setUserType(usertype);
    updateUser.setNickName(nickname);
    updateUser.setTitle(title);

    ScimResponse scimResponse = resourceEndpointHandler.updateResource(endpoint,
                                                                       readUser.getId().get(),
                                                                       updateUser.toString(),
                                                                       null,
                                                                       getBaseUrlSupplier(),
                                                                       null);
    Mockito.verify(userHandler, Mockito.times(1)).updateResource(Mockito.any(), Mockito.isNull());
    MatcherAssert.assertThat(scimResponse.getClass(), Matchers.typeCompatibleWith(UpdateResponse.class));
    Assertions.assertEquals(HttpStatus.OK, scimResponse.getHttpStatus());
    Assertions.assertEquals(HttpHeader.SCIM_CONTENT_TYPE,
                            scimResponse.getHttpHeaders().get(HttpHeader.CONTENT_TYPE_HEADER));
    Assertions.assertNotNull(scimResponse.getHttpHeaders().get(HttpHeader.LOCATION_HEADER));
    User updatedUser = JsonHelper.readJsonDocument(scimResponse.toString(), User.class);
    Assertions.assertNotEquals(readUser, updatedUser);
    Assertions.assertEquals(usertype, updatedUser.getUserType().get());
    Assertions.assertEquals(nickname, updatedUser.getNickName().get());
    Assertions.assertEquals(title, updatedUser.getTitle().get());

    Meta meta = updatedUser.getMeta().get();
    Assertions.assertEquals("User", meta.getResourceType().get());
    Assertions.assertEquals(getLocation(endpoint, updatedUser.getId().get()), meta.getLocation().get());
    Assertions.assertTrue(meta.getCreated().isPresent());
    Assertions.assertTrue(meta.getLastModified().isPresent());

    // the following prevents inequalities in a one second range. the last modified value must not necessarily be
    // equals but the rest of the resource must
    meta.setLastModified((String)null);
    updateUser.getMeta().get().setLastModified((String)null);
    Assertions.assertEquals(updateUser, updatedUser);
    return updatedUser;
  }

  /**
   * this method will get the current location uri to a resource
   *
   * @param resourceId the id of the resource
   * @return the full location URL of the endpoint
   */
  private String getLocation(String endpoint, String resourceId)
  {
    return getBaseUrlSupplier().get() + endpoint + "/" + resourceId;
  }

  /**
   * the base uri supplier that is given to the endpoint implementations
   */
  private Supplier<String> getBaseUrlSupplier()
  {
    return () -> "https://goldfish.de/scim/v2";
  }

  /**
   * creates the given number of users
   *
   * @param totalResults the number of users to create
   * @return the list of the created users
   */
  protected List<User> createUsers(long totalResults)
  {
    List<User> userList = new ArrayList<>();
    for ( int i = 0 ; i < totalResults ; i++ )
    {
      ScimResponse scimResponse = resourceEndpointHandler.createResource(EndpointPaths.USERS,
                                                                         readResourceFile(USER_RESOURCE),
                                                                         null,
                                                                         null);
      CreateResponse createResponse = (CreateResponse)scimResponse;
      User user = JsonHelper.readJsonDocument(createResponse.toString(), User.class);
      userList.add(user);
    }
    return userList;
  }
}
