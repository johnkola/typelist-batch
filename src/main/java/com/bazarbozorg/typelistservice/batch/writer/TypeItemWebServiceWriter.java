package com.bazarbozorg.typelistservice.batch.writer;

import com.bazarbozorg.typelistservice.batch.exception.TypeServiceException;
import com.bazarbozorg.typelistservice.batch.service.TypelistWebServiceClient;
import com.bazarbozorg.typelistservice.client.model.TypeItemDTO;
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
 * to write TypeItemDTO objects to the TypeList web service.
 */
@Component
@Qualifier("typeItemWriter")
public class TypeItemWebServiceWriter implements ItemWriter<TypeItemDTO> {
    private static final Logger logger = LoggerFactory.getLogger(TypeItemWebServiceWriter.class);
    
    private final TypelistWebServiceClient webServiceClient;
    
    @Autowired
    public TypeItemWebServiceWriter(TypelistWebServiceClient webServiceClient) {
        this.webServiceClient = webServiceClient;
    }
    
    @Override
    public void write(Chunk<? extends TypeItemDTO> items) throws Exception {
        logger.info("Writing {} items to TypeList web service", items.size());
        
        try {
            // Convert Chunk to List for processing
            List<TypeItemDTO> itemsList = new ArrayList<>(items.getItems());
            
            // Send items to web service using the sendTypeItems method
            webServiceClient.sendTypeItems(itemsList);
            
            logger.info("Successfully wrote {} items to TypeList web service", itemsList.size());
        } catch (Exception e) {
            logger.error("Error writing to TypeList web service: {}", e.getMessage(), e);
            throw new TypeServiceException("Failed to update TypeList web service", e);
        }
    }
}