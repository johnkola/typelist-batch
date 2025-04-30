package com.bazarbozorg.typelistservice.batch.processor;

import com.bazarbozorg.typelistservice.client.model.TypeSetDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * Processor implementation for TypeSetDTO objects.
 * Handles validation, transformation, and enrichment of TypeSet entities.
 */
@Component
public class TypeSetProcessor implements ItemProcessor<TypeSetDTO, TypeSetDTO> {

    private static final Logger log = LoggerFactory.getLogger(TypeSetProcessor.class);
    
    @Value("${batch.processing.transform-fields:true}")
    private boolean transformFields;
    
    @Value("${batch.processing.filter-invalid:true}")
    private boolean filterInvalid;
    
    // Map of property transformation functions
    private final Map<String, Function<String, String>> propertyTransformers = new HashMap<>();
    
    /**
     * Constructor with default property transformers
     */
    public TypeSetProcessor() {
        // Register default transformers
        registerDefaultTransformers();
    }
    
    /**
     * Registers default property transformation functions
     */
    private void registerDefaultTransformers() {
        // Common data cleaning transformations
        propertyTransformers.put("name", this::trimAndNormalize);
        propertyTransformers.put("code", this::trimAndNormalize);
        propertyTransformers.put("description", this::trimAndNormalize);
    }
    
    /**
     * Process the TypeSetDTO by applying transformations and validations
     *
     * @param typeSet The input TypeSetDTO
     * @return The processed TypeSetDTO or null if the item should be filtered out
     */
    @Override
    public TypeSetDTO process(TypeSetDTO typeSet) throws Exception {
        if (typeSet == null) {
            return null;
        }
        
        try {
            // Apply property transformations if enabled
            if (transformFields) {
                applyPropertyTransformations(typeSet);
            }
            
            // Validate the TypeSet
            if (filterInvalid && !isValid(typeSet)) {
                log.warn("Invalid TypeSet filtered out: code={}", typeSet.getCode());
                return null; // Returning null filters out the item
            }
            
            // Enrich the TypeSet
            enrichTypeSet(typeSet);
            
            log.debug("Successfully processed TypeSet: code={}", typeSet.getCode());
            return typeSet;
        } catch (Exception e) {
            log.error("Error processing TypeSet: code={}, error={}", 
                    typeSet.getCode(), e.getMessage(), e);
            if (filterInvalid) {
                return null; // Filter out on error
            }
            throw e; // Re-throw if we're not filtering
        }
    }
    
    /**
     * Applies registered transformations to TypeSet properties
     */
    private void applyPropertyTransformations(TypeSetDTO typeSet) {
        // Apply transformations to standard fields
        if (typeSet.getName() != null && propertyTransformers.containsKey("name")) {
            typeSet.setName(propertyTransformers.get("name").apply(typeSet.getName()));
        }
        
        if (typeSet.getCode() != null && propertyTransformers.containsKey("code")) {
            typeSet.setCode(propertyTransformers.get("code").apply(typeSet.getCode()));
        }
        
        if (typeSet.getDescription() != null && propertyTransformers.containsKey("description")) {
            typeSet.setDescription(propertyTransformers.get("description").apply(typeSet.getDescription()));
        }
        
        // Apply transformations to dynamic properties if they exist
        if (typeSet.getProperties() != null) {
            Map<String, String> updatedProps = new HashMap<>();
            
            typeSet.getProperties().forEach((key, value) -> {
                if (value != null && propertyTransformers.containsKey(key)) {
                    updatedProps.put(key, propertyTransformers.get(key).apply((String) value));
                } else {
                    updatedProps.put(key, value);
                }
            });
            
            typeSet.setProperties(updatedProps);
        }
    }
    
    /**
     * Validates that the TypeSetDTO meets business rules
     */
    private boolean isValid(TypeSetDTO typeSet) {
        // Required field check
        if (Objects.isNull(typeSet.getCode())) {
            log.debug("TypeSet invalid: code is null");
            return false;
        }
        
        if (typeSet.getCode().trim().isEmpty()) {
            log.debug("TypeSet invalid: code is empty");
            return false;
        }
        
        if (Objects.isNull(typeSet.getParentTypeListId())) {
            log.debug("TypeSet invalid: parentTypeListId is null");
            return false;
        }
        
        if (Objects.isNull(typeSet.getName())) {
            log.debug("TypeSet invalid: name is null");
            return false;
        }
        
        if (typeSet.getName().trim().isEmpty()) {
            log.debug("TypeSet invalid: name is empty");
            return false;
        }
        
        // Add additional validation rules as needed
        // For example, validate that the referenced TypeList exists
        
        return true;
    }
    
    /**
     * Enriches the TypeSet with additional calculated or derived fields
     */
    private void enrichTypeSet(TypeSetDTO typeSet) {
        // Initialize properties map if null
        if (typeSet.getProperties() == null) {
            typeSet.setProperties(new HashMap<>());
        }
        
        // Initialize itemIds list if null
        if (typeSet.getItemIds() == null) {
            typeSet.setItemIds(new ArrayList<>());
        }
        
        // Add processing metadata
        typeSet.getProperties().put("_processedAt", LocalDateTime.now().toString());
        typeSet.getProperties().put("_processorVersion", "1.0");
        typeSet.getProperties().put("_itemCount", String.valueOf(typeSet.getItemIds().size()));
        
        // If the publicId is not set and this appears to be a new TypeSet,
        // we could consider generating a temporary ID for reference tracking
        // (the actual ID will be assigned by the service)
        if (typeSet.getPublicId() == null) {
            typeSet.getProperties().put("_tempId", UUID.randomUUID().toString());
        }
    }
    
    /**
     * Utility method to trim and normalize string values
     */
    private String trimAndNormalize(String value) {
        if (value == null) {
            return null;
        }
        
        // Trim whitespace and normalize
        String normalized = value.trim();
        
        // Handle other normalization (remove multiple spaces, etc.)
        normalized = normalized.replaceAll("\\s+", " ");
        
        return normalized;
    }
}