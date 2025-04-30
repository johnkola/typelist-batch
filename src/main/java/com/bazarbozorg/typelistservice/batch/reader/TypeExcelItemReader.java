package com.bazarbozorg.typelistservice.batch.reader;

import com.bazarbozorg.typelistservice.client.model.TypeItemDTO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.IteratorItemReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Excel implementation for reading TypeItem data from files.
 */
@Component
public class TypeExcelItemReader implements TypeItemReader {

    private static final Logger log = LoggerFactory.getLogger(TypeExcelItemReader.class);
    
    @Override
    public ItemReader<TypeItemDTO> createReader(File file) {
        log.info("Creating Excel reader for TypeItem data from: {}", file.getName());
        
        try {
            List<TypeItemDTO> items = new ArrayList<>();
            FileSystemResource resource = new FileSystemResource(file);
            
            try (InputStream is = resource.getInputStream();
                 Workbook workbook = WorkbookFactory.create(is)) {
                
                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    throw new IllegalArgumentException("Excel file has no sheets");
                }
                
                // Get header row to determine column positions
                Row headerRow = sheet.getRow(0);
                Map<String, Integer> columnMap = new HashMap<>();
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    String columnName = headerRow.getCell(i).getStringCellValue().toLowerCase();
                    columnMap.put(columnName, i);
                }
                
                // Process data rows
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    
                    TypeItemDTO typeItem = new TypeItemDTO();
                    
                    // Set basic properties
                    if (columnMap.containsKey("code")) {
                        int idx = columnMap.get("code");
                        if (row.getCell(idx) != null) {
                            typeItem.setCode(row.getCell(idx).getStringCellValue());
                        }
                    }
                    
                    if (columnMap.containsKey("name")) {
                        int idx = columnMap.get("name");
                        if (row.getCell(idx) != null) {
                            typeItem.setName(row.getCell(idx).getStringCellValue());
                        }
                    }
                    
                    if (columnMap.containsKey("description")) {
                        int idx = columnMap.get("description");
                        if (row.getCell(idx) != null) {
                            typeItem.setDescription(row.getCell(idx).getStringCellValue());
                        }
                    }
                    
                    if (columnMap.containsKey("parenttypelistid")) {
                        int idx = columnMap.get("parenttypelistid");
                        if (row.getCell(idx) != null) {
                            String idStr = row.getCell(idx).getStringCellValue();
                            try {
                                typeItem.setParentTypeListId(UUID.fromString(idStr));
                            } catch (IllegalArgumentException e) {
                                log.warn("Invalid UUID format for parentTypeListId: {}", idStr);
                            }
                        }
                    }
                    
                    // Add the TypeItem if it has at least code, name, and parent ID
                    if (typeItem.getCode() != null && typeItem.getName() != null && 
                        typeItem.getParentTypeListId() != null) {
                        items.add(typeItem);
                    }
                }
            }
            
            log.info("Extracted {} TypeItem items from Excel file", items.size());
            return new IteratorItemReader<>(items);
            
        } catch (Exception e) {
            log.error("Error reading TypeItem data from Excel file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read TypeItem data from Excel", e);
        }
    }
    
    @Override
    public boolean supports(String fileExtension) {
        return "xlsx".equalsIgnoreCase(fileExtension) || "xls".equalsIgnoreCase(fileExtension);
    }
}