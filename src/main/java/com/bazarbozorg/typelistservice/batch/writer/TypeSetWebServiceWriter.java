package com.bazarbozorg.typelistservice.batch.writer;

import com.bazarbozorg.typelistservice.batch.exception.TypeServiceException;
import com.bazarbozorg.typelistservice.batch.service.TypelistWebServiceClient;
import com.bazarbozorg.typelistservice.client.model.TypeSetDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Batch ItemWriter implementation that uses TypelistWebServiceClient
 * to write TypeSetDTO objects to the TypeList web service.
 */
@Component
@Qualifier("typeSetWriter")
public class TypeSetWebServiceWriter implements ItemWriter<TypeSetDTO> {
    private static final Logger logger = LoggerFactory.getLogger(TypeSetWebServiceWriter.class);
    
    private final TypelistWebServiceClient webServiceClient;
    
    @Autowired
    public TypeSetWebServiceWriter(TypelistWebServiceClient webServiceClient) {
        this.webServiceClient = webServiceClient;
    }
    
    @Override
    public void write(Chunk<? extends TypeSetDTO> items) throws Exception {
        logger.info("Writing {} TypeSet items to web service", items.size());
        
        try {
            // Convert Chunk to List for processing
            List<TypeSetDTO> itemsList = new ArrayList<>(items.getItems());
            
            // Use the updated client's sendTypeSets method which handles item association internally
            webServiceClient.sendTypeSets(itemsList);
            
            logger.info("Successfully wrote {} TypeSet items to web service", itemsList.size());
        } catch (Exception e) {
            logger.error("Error writing TypeSets to web service: {}", e.getMessage(), e);
            throw new TypeServiceException("Failed to update TypeSets", e);
        }
    }
}