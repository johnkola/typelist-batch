package com.bazarbozorg.typelistservice.batch.writer;

import com.bazarbozorg.typelistservice.batch.exception.TypeServiceException;
import com.bazarbozorg.typelistservice.batch.service.TypelistWebServiceClient;
import com.bazarbozorg.typelistservice.client.model.TypeListDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring Batch ItemWriter implementation that uses TypelistWebServiceClient
 * to write TypeListDTO objects to the TypeList web service.
 */
@Component
@Qualifier("typeListWriter")
public class TypeListWebServiceWriter implements ItemWriter<TypeListDTO> {
    private static final Logger logger = LoggerFactory.getLogger(TypeListWebServiceWriter.class);
    
    private final TypelistWebServiceClient webServiceClient;
    
    @Autowired
    public TypeListWebServiceWriter(TypelistWebServiceClient webServiceClient) {
        this.webServiceClient = webServiceClient;
    }
    
    @Override
    public void write(Chunk<? extends TypeListDTO> items) throws Exception {
        logger.info("Writing {} TypeList items to web service", items.size());
        
        try {
            List<TypeListDTO> itemsList = new ArrayList<>(items.getItems());
            AtomicInteger successCount = new AtomicInteger(0);
            
            // Process each TypeList one by one
            for (TypeListDTO typeList : itemsList) {
                try {
                    // Use sendTypeList method from the updated client
                    webServiceClient.sendTypeList(typeList);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    logger.error("Error processing TypeList '{}': {}", typeList.getCode(), e.getMessage());
                    // Continue with next item rather than failing entire batch
                }
            }
            
            logger.info("Successfully wrote {}/{} TypeList items to web service", 
                    successCount.get(), itemsList.size());
            
            // If nothing was processed successfully, throw an exception
            if (successCount.get() == 0 && !itemsList.isEmpty()) {
                throw new TypeServiceException("Failed to process any TypeList items");
            }
        } catch (TypeServiceException e) {
            // Re-throw TypeServiceException
            throw e;
        } catch (Exception e) {
            logger.error("Error writing TypeLists to web service: {}", e.getMessage(), e);
            throw new TypeServiceException("Failed to update TypeLists", e);
        }
    }
}