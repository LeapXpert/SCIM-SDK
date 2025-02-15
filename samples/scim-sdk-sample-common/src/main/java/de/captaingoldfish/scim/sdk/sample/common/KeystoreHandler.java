package de.captaingoldfish.scim.sdk.sample.common;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import de.captaingoldfish.scim.sdk.common.constants.enums.SortOrder;
import de.captaingoldfish.scim.sdk.common.exceptions.ConflictException;
import de.captaingoldfish.scim.sdk.common.exceptions.ResourceNotFoundException;
import de.captaingoldfish.scim.sdk.common.schemas.SchemaAttribute;
import de.captaingoldfish.scim.sdk.server.endpoints.ResourceHandler;
import de.captaingoldfish.scim.sdk.server.endpoints.authorize.Authorization;
import de.captaingoldfish.scim.sdk.server.filter.FilterNode;
import de.captaingoldfish.scim.sdk.server.response.PartialListResponse;


/**
 * <br>
 * <br>
 * created at: 02.05.2020
 *
 * @author Pascal Knüppel
 */
public class KeystoreHandler extends ResourceHandler<ScimKeystore>
{

  private Map<String, ScimKeystore> keystoreMap = new HashMap<>();

  @Override
  public ScimKeystore createResource(ScimKeystore resource, Authorization authorization,
      Map<String, String> httpHeaders)
  {
    // names should be unique so find if the name of the new resource is already taken
    ScimKeystore oldResource = keystoreMap.values()
                                          .stream()
                                          .filter(keystore -> StringUtils.equalsIgnoreCase(keystore.getName(),
                                                                                           resource.getName()))
                                          .findAny()
                                          .orElse(null);
    if (oldResource != null)
    {
      throw new ConflictException("keystore with name '" + resource.getName() + "' is already taken");
    }
    final String id = UUID.randomUUID().toString();
    resource.setId(id);
    resource.getMeta().ifPresent(meta -> {
      meta.setCreated(Instant.now());
      meta.setLastModified(Instant.now());
    });
    keystoreMap.put(id, resource);
    return resource;
  }

  @Override
  public ScimKeystore getResource(String id,
                                  Authorization authorization,
                                  List<SchemaAttribute> attributes,
                                  List<SchemaAttribute> excludedAttributes,
                                  Map<String, String> httpHeaders)
  {
    return keystoreMap.get(id);
  }

  /**
   * @param attributes For convenience only: If you wish to only select the specific attributes on database
   *          level (note that the api will take care of this. You may only use this parameter to increase
   *          database performance)
   * @param excludedAttributes For convenience usage only: if you wish to exclude parameters like big-subtypes
   *          on database level (note that the api will take care of this. You may only use this parameter to
   *          increase * database performance)
   * @param authorization optional attribute: contains authorization information and may contain additional
   * @param httpHeaders
   */
  @Override
  public PartialListResponse<ScimKeystore> listResources(long startIndex,
      int count,
      FilterNode filter,
      SchemaAttribute sortBy,
      SortOrder sortOrder,
      List<SchemaAttribute> attributes,
      List<SchemaAttribute> excludedAttributes,
      Authorization authorization, Map<String, String> httpHeaders)
  {
    // filtering is not performed here. Note that the api provides an auto-filtering feature
    // sorting is not performed here. Note that the api provides an auto-sorting feature
    List<ScimKeystore> scimKeystores = new ArrayList<>(keystoreMap.values());
    int effectiveStartIndex = (int)Math.min(startIndex, scimKeystores.size() - 1);
    int effectiveCount = (int)Math.min(startIndex + count, scimKeystores.size() - 1);
    scimKeystores = scimKeystores.subList(effectiveStartIndex, effectiveCount);
    return PartialListResponse.<ScimKeystore> builder()
                              .totalResults(scimKeystores.size())
                              .resources(scimKeystores)
                              .build();
  }

  @Override
  public ScimKeystore updateResource(ScimKeystore resourceToUpdate, Authorization authorization,
      Map<String, String> httpHeaders)
  {
    final String id = resourceToUpdate.getId().orElse(null);
    ScimKeystore oldResource = keystoreMap.get(id);
    if (oldResource == null)
    {
      throw new ResourceNotFoundException("resource with id '" + id + "' does not exist");
    }
    resourceToUpdate.getMeta().ifPresent(meta -> meta.setLastModified(Instant.now()));
    keystoreMap.put(id, resourceToUpdate);
    return resourceToUpdate;
  }

  @Override
  public void deleteResource(String id, Authorization authorization,
      Map<String, String> httpHeaders)
  {
    ScimKeystore oldResource = keystoreMap.get(id);
    if (oldResource == null)
    {
      throw new ResourceNotFoundException("resource with id '" + id + "' does not exist");
    }
    keystoreMap.remove(id);
  }
}
