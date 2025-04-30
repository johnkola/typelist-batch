package com.bazarbozorg.typelistservice.batch.service;

import com.bazarbozorg.typelistservice.batch.exception.TypeServiceException;
import com.bazarbozorg.typelistservice.client.api.TypeItemManagementApi;
import com.bazarbozorg.typelistservice.client.api.TypeListManagementApi;
import com.bazarbozorg.typelistservice.client.api.TypeSetManagementApi;
import com.bazarbozorg.typelistservice.client.model.TypeItemDTO;
import com.bazarbozorg.typelistservice.client.model.TypeListDTO;
import com.bazarbozorg.typelistservice.client.model.TypeSetDTO;
import com.bazarbozorg.typelistservice.client.model.CreateTypeListRequest;
import com.bazarbozorg.typelistservice.client.model.CreateTypeItemRequest;
import com.bazarbozorg.typelistservice.client.model.CreateTypeSetRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client for interacting with the TypeList web service API using Feign clients
 * from the service client jar.
 */
@Service
public class TypelistWebServiceClient {
    private static final Logger log = LoggerFactory.getLogger(TypelistWebServiceClient.class);

    private final TypeListManagementApi typeListManagementApi;
    private final TypeItemManagementApi typeItemManagementApi;
    private final TypeSetManagementApi typeSetManagementApi;
    private final RetryTemplate apiRetryTemplate;
    
    @Value("${typelist.api.batch-size:100}")
    private int batchSize;
    
    /**
     * Creates a new TypelistWebServiceClient.
     *
     * @param typeListManagementApi The API client for TypeList operations
     * @param typeItemManagementApi The API client for TypeItem operations
     * @param typeSetManagementApi The API client for TypeSet operations
     * @param apiRetryTemplate The retry template for handling retries
     */
    @Autowired
    public TypelistWebServiceClient(
            TypeListManagementApi typeListManagementApi,
            TypeItemManagementApi typeItemManagementApi,
            TypeSetManagementApi typeSetManagementApi,
            @Qualifier("apiRetryTemplate") RetryTemplate apiRetryTemplate) {
        this.typeListManagementApi = typeListManagementApi;
        this.typeItemManagementApi = typeItemManagementApi;
        this.typeSetManagementApi = typeSetManagementApi;
        this.apiRetryTemplate = apiRetryTemplate;
        
        log.info("TypelistWebServiceClient initialized with batch size: {}", batchSize);
    }

    /**
     * Sends TypeList data to the web service with retry support.
     *
     * @param typeList The TypeList data to send
     * @return The created TypeList from the service
     * @throws TypeServiceException if the operation fails after retries
     */
    public TypeListDTO sendTypeList(TypeListDTO typeList) throws TypeServiceException {
        try {
            return apiRetryTemplate.execute((RetryCallback<TypeListDTO, Exception>) context -> {
                // Log retry attempts
                if (context.getRetryCount() > 0) {
                    log.warn("Retrying sendTypeList (attempt #{})", context.getRetryCount() + 1);
                }
                
                // Convert DTO to request object
                CreateTypeListRequest request = new CreateTypeListRequest();
                request.setCode(typeList.getCode());
                request.setName(typeList.getName());
                request.setDescription(typeList.getDescription());
                
                // Make the actual API call using the Feign client
                return typeListManagementApi.createList(request);
            });
        } catch (Exception e) {
            log.error("Failed to send TypeList after retry attempts", e);
            throw new TypeServiceException("Failed to send TypeList data", e);
        }
    }

    /**
     * Sends a batch of TypeItems to the web service with retry support.
     * Processes in sub-batches according to configured batch size.
     *
     * @param typeItems The TypeItems to send
     * @throws TypeServiceException if the operation fails after retries
     */
    public void sendTypeItems(List<TypeItemDTO> typeItems) throws TypeServiceException {
        if (typeItems == null || typeItems.isEmpty()) {
            log.warn("No TypeItems to send");
            return;
        }
        
        // Process in batches
        for (List<TypeItemDTO> batch : createBatches(typeItems, batchSize)) {
            for (TypeItemDTO item : batch) {
                try {
                    apiRetryTemplate.execute((RetryCallback<TypeItemDTO, Exception>) context -> {
                        // Log retry attempts
                        if (context.getRetryCount() > 0) {
                            log.warn("Retrying sendTypeItem (attempt #{})", context.getRetryCount() + 1);
                        }
                        
                        // Convert DTO to request object
                        CreateTypeItemRequest request = new CreateTypeItemRequest();
                        request.setCode(item.getCode());
                        request.setName(item.getName());
                        request.setDescription(item.getDescription());
                        request.setParentTypeListId(item.getParentTypeListId());
                        
                        // Make the actual API call using the Feign client
                        return typeItemManagementApi.createItem(request);
                    });
                } catch (Exception e) {
                    log.error("Failed to send TypeItem {} after retry attempts", item.getCode(), e);
                    throw new TypeServiceException("Failed to send TypeItem data: " + item.getCode(), e);
                }
            }
            log.info("Successfully processed batch of {} TypeItems", batch.size());
        }
    }

    /**
     * Sends a batch of TypeSets to the web service with retry support.
     * Processes in sub-batches according to configured batch size.
     *
     * @param typeSets The TypeSets to send
     * @throws TypeServiceException if the operation fails after retries
     */
    public void sendTypeSets(List<TypeSetDTO> typeSets) throws TypeServiceException {
        if (typeSets == null || typeSets.isEmpty()) {
            log.warn("No TypeSets to send");
            return;
        }
        
        // Process in batches
        for (List<TypeSetDTO> batch : createBatches(typeSets, batchSize)) {
            for (TypeSetDTO set : batch) {
                try {
                    // First create the TypeSet
                    TypeSetDTO createdSet = apiRetryTemplate.execute((RetryCallback<TypeSetDTO, Exception>) context -> {
                        // Log retry attempts
                        if (context.getRetryCount() > 0) {
                            log.warn("Retrying createTypeSet (attempt #{})", context.getRetryCount() + 1);
                        }
                        
                        // Convert DTO to request object
                        CreateTypeSetRequest request = new CreateTypeSetRequest();
                        request.setCode(set.getCode());
                        request.setName(set.getName());
                        request.setDescription(set.getDescription());
                        request.setParentTypeListId(set.getParentTypeListId());
                        
                        // Make the actual API call using the Feign client
                        return typeSetManagementApi.createSet(request);
                    });
                    
                    // If there are items in the set, add them
                    if (set.getItemIds() != null && !set.getItemIds().isEmpty()) {
                        apiRetryTemplate.execute((RetryCallback<Long, Exception>) context -> {
                            // Log retry attempts
                            if (context.getRetryCount() > 0) {
                                log.warn("Retrying addItemsToTypeSet (attempt #{})", context.getRetryCount() + 1);
                            }
                            
                            // Make the actual API call to add items to the set
                            return typeSetManagementApi.addItemsToTypeSet(
                                    set.getItemIds(),
                                    createdSet.getPublicId(),
                                    createdSet.getVersion());
                        });
                    }
                } catch (Exception e) {
                    log.error("Failed to send TypeSet {} after retry attempts", set.getCode(), e);
                    throw new TypeServiceException("Failed to send TypeSet data: " + set.getCode(), e);
                }
            }
            log.info("Successfully processed batch of {} TypeSets", batch.size());
        }
    }
    
    /**
     * Helper method to split a list into smaller batches.
     *
     * @param <T> The type of items in the list
     * @param items The full list of items
     * @param batchSize The maximum size of each batch
     * @return A list of batches
     */
    private <T> List<List<T>> createBatches(List<T> items, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        
        for (int i = 0; i < items.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, items.size());
            batches.add(items.subList(i, endIndex));
        }
        
        log.debug("Split {} items into {} batches of max size {}", 
                items.size(), batches.size(), batchSize);
        
        return batches;
    }
}