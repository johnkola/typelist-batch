package com.bazarbozorg.typelistservice.batch.processor;

import com.bazarbozorg.typelistservice.client.model.TypeListDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Processor implementation for TypeListDTO objects.
 * Handles validation, transformation, and enrichment of TypeList entities.
 */
@Component
public class TypeListProcessor implements ItemProcessor<TypeListDTO, TypeListDTO> {

    private static final Logger log = LoggerFactory.getLogger(TypeListProcessor.class);
    
    @Value("${batch.processing.transform-fields:true}")
    private boolean transformFields;
    
    @Value("${batch.processing.filter-invalid:true}")
    private boolean filterInvalid;
    
    // Map of property transformation functions
    private final Map<String, Function<String, String>> propertyTransformers = new HashMap<>();
    
    /**
     * Constructor with default property transformers
     */
    public TypeListProcessor() {
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
     * Process the TypeListDTO by applying transformations and validations
     *
     * @param typeList The input TypeListDTO
     * @return The processed TypeListDTO or null if the list should be filtered out
     */
    @Override
    public TypeListDTO process(TypeListDTO typeList) throws Exception {
        if (typeList == null) {
            return null;
        }
        
        try {
            // Apply property transformations if enabled
            if (transformFields) {
                applyPropertyTransformations(typeList);
            }
            
            // Validate the list
            if (filterInvalid && !isValid(typeList)) {
                log.warn("Invalid TypeList filtered out: code={}", typeList.getCode());
                return null; // Returning null filters out the item
            }
            
            // Enrich the list
            enrichTypeList(typeList);
            
            log.debug("Successfully processed TypeList: code={}", typeList.getCode());
            return typeList;
        } catch (Exception e) {
            log.error("Error processing TypeList: code={}, error={}", 
                    typeList.getCode(), e.getMessage(), e);
            if (filterInvalid) {
                return null; // Filter out on error
            }
            throw e; // Re-throw if we're not filtering
        }
    }
    
    /**
     * Applies registered transformations to TypeList properties
     */
    private void applyPropertyTransformations(TypeListDTO typeList) {
        // Apply transformations to standard fields
        if (typeList.getName() != null && propertyTransformers.containsKey("name")) {
            typeList.setName(propertyTransformers.get("name").apply(typeList.getName()));
        }
        
        if (typeList.getCode() != null && propertyTransformers.containsKey("code")) {
            typeList.setCode(propertyTransformers.get("code").apply(typeList.getCode()));
        }
        
        if (typeList.getDescription() != null && propertyTransformers.containsKey("description")) {
            typeList.setDescription(propertyTransformers.get("description").apply(typeList.getDescription()));
        }
        
        // Apply transformations to dynamic properties if they exist
        if (typeList.getProperties() != null) {
            Map<String, String> updatedProps = new HashMap<>();
            
            typeList.getProperties().forEach((key, value) -> {
                if (value != null && propertyTransformers.containsKey(key)) {
                    updatedProps.put(key, propertyTransformers.get(key).apply((String) value));
                } else {
                    updatedProps.put(key, value);
                }
            });
            
            typeList.setProperties(updatedProps);
        }
    }
    
    /**
     * Validates that the TypeListDTO meets business rules
     */
    private boolean isValid(TypeListDTO typeList) {
        // Required field check
        if (Objects.isNull(typeList.getCode())) {
            log.debug("TypeList invalid: code is null");
            return false;
        }
        
        if (typeList.getCode().trim().isEmpty()) {
            log.debug("TypeList invalid: code is empty");
            return false;
        }
        
        if (Objects.isNull(typeList.getName())) {
            log.debug("TypeList invalid: name is null");
            return false;
        }
        
        if (typeList.getName().trim().isEmpty()) {
            log.debug("TypeList invalid: name is empty");
            return false;
        }
        
        // Add additional validation as needed
        // For example, validate code format, check for reserved words, etc.
        
        return true;
    }
    
    /**
     * Enriches the TypeList with additional calculated or derived fields
     */
    private void enrichTypeList(TypeListDTO typeList) {
        // Initialize properties map if null
        if (typeList.getProperties() == null) {
            typeList.setProperties(new HashMap<>());
        }
        
        // Add processing metadata
        typeList.getProperties().put("_processedAt", LocalDateTime.now().toString());
        typeList.getProperties().put("_processorVersion", "1.0");
        
        // If the publicId is not set and this appears to be a new TypeList,
        // we could consider generating a temporary ID for reference tracking
        // (the actual ID will be assigned by the service)
        if (typeList.getPublicId() == null) {
            typeList.getProperties().put("_tempId", UUID.randomUUID().toString());
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