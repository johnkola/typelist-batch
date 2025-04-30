package com.bazarbozorg.typelistservice.batch.processor;

import com.bazarbozorg.typelistservice.client.model.TypeItemDTO;
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
 * Processor implementation for transforming TypeItemDTO objects.
 * Validates, transforms, and enriches items before they are sent to the web service.
 */
@Component
public class TypeItemProcessor implements ItemProcessor<TypeItemDTO, TypeItemDTO> {

    private static final Logger log = LoggerFactory.getLogger(TypeItemProcessor.class);
    
    @Value("${batch.processing.transform-fields:true}")
    private boolean transformFields;
    
    @Value("${batch.processing.filter-invalid:true}")
    private boolean filterInvalid;
    
    // Map of property transformation functions
    private final Map<String, Function<String, String>> propertyTransformers = new HashMap<>();
    
    /**
     * Constructor with default property transformers
     */
    public TypeItemProcessor() {
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
     * Process the TypeItemDTO by applying transformations and validations
     *
     * @param item The input TypeItemDTO
     * @return The processed TypeItemDTO or null if the item should be filtered out
     */
    @Override
    public TypeItemDTO process(TypeItemDTO item) throws Exception {
        if (item == null) {
            return null;
        }
        
        try {
            // Apply property transformations if enabled
            if (transformFields) {
                applyPropertyTransformations(item);
            }
            
            // Validate the item
            if (filterInvalid && !isValid(item)) {
                log.warn("Invalid TypeItem filtered out: code={}", item.getCode());
                return null; // Returning null filters out the item
            }
            
            // Enrich the item
            enrichItem(item);
            
            log.debug("Successfully processed TypeItem: code={}", item.getCode());
            return item;
        } catch (Exception e) {
            log.error("Error processing TypeItem: code={}, error={}", 
                    item.getCode(), e.getMessage(), e);
            if (filterInvalid) {
                return null; // Filter out on error
            }
            throw e; // Re-throw if we're not filtering
        }
    }
    
    /**
     * Applies registered transformations to item properties
     */
    private void applyPropertyTransformations(TypeItemDTO item) {
        // Apply transformations to standard fields
        if (item.getName() != null && propertyTransformers.containsKey("name")) {
            item.setName(propertyTransformers.get("name").apply(item.getName()));
        }
        
        if (item.getCode() != null && propertyTransformers.containsKey("code")) {
            item.setCode(propertyTransformers.get("code").apply(item.getCode()));
        }
        
        if (item.getDescription() != null && propertyTransformers.containsKey("description")) {
            item.setDescription(propertyTransformers.get("description").apply(item.getDescription()));
        }
        
        // Apply transformations to dynamic properties if they exist
        if (item.getProperties() != null) {
            Map<String, String> updatedProps = new HashMap<>();
            
            item.getProperties().forEach((key, value) -> {
                if (value != null && propertyTransformers.containsKey(key)) {
                    updatedProps.put(key, propertyTransformers.get(key).apply((String) value));
                } else {
                    updatedProps.put(key, value);
                }
            });
            
            item.setProperties(updatedProps);
        }
    }
    
    /**
     * Validates that the TypeItemDTO meets business rules
     */
    private boolean isValid(TypeItemDTO item) {
        // Required field check
        if (Objects.isNull(item.getCode())) {
            log.debug("TypeItem invalid: code is null");
            return false;
        }
        
        if (item.getCode().trim().isEmpty()) {
            log.debug("TypeItem invalid: code is empty");
            return false;
        }
        
        if (Objects.isNull(item.getParentTypeListId())) {
            log.debug("TypeItem invalid: parentTypeListId is null");
            return false;
        }
        
        if (Objects.isNull(item.getName())) {
            log.debug("TypeItem invalid: name is null");
            return false;
        }
        
        if (item.getName().trim().isEmpty()) {
            log.debug("TypeItem invalid: name is empty");
            return false;
        }
        
        // Add additional validation rules as needed
        // For example, validate code format, check property values, etc.
        
        return true;
    }
    
    /**
     * Enriches the item with additional calculated or derived fields
     */
    private void enrichItem(TypeItemDTO item) {
        // Initialize properties map if null
        if (item.getProperties() == null) {
            item.setProperties(new HashMap<>());
        }
        
        // Add processing metadata
        item.getProperties().put("_processedAt", LocalDateTime.now().toString());
        item.getProperties().put("_processorVersion", "1.0");
        
        // If the publicId is not set and this appears to be a new item,
        // we could consider generating a temporary ID for reference tracking
        // (the actual ID will be assigned by the service)
        if (item.getPublicId() == null) {
            item.getProperties().put("_tempId", UUID.randomUUID().toString());
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