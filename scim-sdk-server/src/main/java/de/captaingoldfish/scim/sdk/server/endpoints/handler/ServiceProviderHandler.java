package de.captaingoldfish.scim.sdk.server.endpoints.handler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.NotImplementedException;
import de.captaingoldfish.scim.sdk.common.resources.ServiceProvider;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;
import lombok.AllArgsConstructor;


/**
 * author Pascal Knueppel <br>
 * created at: 18.10.2019 - 09:38 <br>
 * <br>
 * the service provider configuration endpoint implementation
 */
@AllArgsConstructor
public class ServiceProviderHandler extends ResourceHandler<ServiceProvider>
{

  /**
   * creates the error message for the not supported operations
   */
  private static final Function<String, String> ERROR_MESSAGE_SUPPLIER = operation -> {
    return "the '" + operation + "'-operation is not supported for ServiceProvider configuration endpoint";
  };

  /**
   * each created {@link de.captaingoldfish.scim.sdk.server.endpoints.ResourceEndpointHandler} must get hold of
   * a single {@link ServiceProvider} instance which is shared with this object. so both instances need to hold
   * the same object reference in order for the application to work correctly
   */
  private final ServiceProvider serviceProvider;

  /**
   * creating of service provider configurations not supported
   */
  @Override
  public ServiceProvider createResource(ServiceProvider resource, Authorization authorization)
  {
    throw new NotImplementedException(ERROR_MESSAGE_SUPPLIER.apply("create"));
  }

  /**
   * gets the one and only service provider configuration for this endpoint definition
   *
   * @param id the id is obsolete here should be null
   * @param authorization
   * @param attributes
   * @param excludedAttributes
   * @return the one and only service provider configuration
   */
  @Override
  public ServiceProvider getResource(String id,
                                     Authorization authorization,
                                     List<SchemaAttribute> attributes,
                                     List<SchemaAttribute> excludedAttributes,
                                     Map<String, String> httpHeaders)
  {
    return serviceProvider;
  }

  /**
   * listing of service provider configurations not supported
   */
  @Override
  public PartialListResponse listResources(long startIndex,
                                           int count,
                                           FilterNode filter,
                                           SchemaAttribute sortBy,
                                           SortOrder sortOrder,
                                           List<SchemaAttribute> attributes,
                                           List<SchemaAttribute> excludedAttributes,
                                           Authorization authorization)
  {
    throw new NotImplementedException(ERROR_MESSAGE_SUPPLIER.apply("list"));
  }

  /**
   * updating of service provider configurations not supported
   */
  @Override
  public ServiceProvider updateResource(ServiceProvider resourceToUpdate, Authorization authorization)
  {
    throw new NotImplementedException(ERROR_MESSAGE_SUPPLIER.apply("update"));
  }

  /**
   * deleting of service provider configurations not supported
   */
  @Override
  public void deleteResource(String id, Authorization authorization)
  {
    throw new NotImplementedException(ERROR_MESSAGE_SUPPLIER.apply("delete"));
  }
}
