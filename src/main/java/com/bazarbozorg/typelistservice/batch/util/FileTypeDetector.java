package com.bazarbozorg.typelistservice.batch.util;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for detecting file types and formats.
 * Supports detection of CSV, TSV, JSON, XML, Excel and other common file types.
 * Also determines delimiters, headers, and other format-specific details.
 */
@Component
public class FileTypeDetector {

    private static final Logger log = LoggerFactory.getLogger(FileTypeDetector.class);

    // Constants for file format detection
    private static final int MAX_LINES_TO_SCAN = 5;
    private static final int MIN_DELIMITER_COUNT = 3;
    private static final int MIN_ROWS_TO_ANALYZE = 3;
    private static final int MAX_CHARS_TO_SCAN = 8192; // 8KB sample size
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("^\\s*\\{.*}\\s*$", Pattern.DOTALL);
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("^\\s*\\[.*]\\s*$", Pattern.DOTALL);
    private static final Pattern XML_PATTERN = Pattern.compile("^\\s*<\\?xml.*?>|^\\s*<[^>]+>", Pattern.DOTALL);

    // Common delimiters to check in CSV-like files
    private static final char[] POTENTIAL_DELIMITERS = {',', '\t', ';', '|', ':'};

    // Excel file signatures (magic numbers)
    private static final byte[] XLS_SIGNATURE = new byte[]{(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0};
    private static final byte[] XLSX_SIGNATURE = new byte[]{0x50, 0x4B, 0x03, 0x04};

    public static char detectDelimiter(String line) {
        return new FileTypeDetector().detectDelimiter(new String[]{line});
    }

    /**
     * Main method to detect file type and format
     *
     * @param filePath Path to the file
     * @return FileInfo object containing detected format information
     * @throws IOException If file cannot be read
     */
    public FileInfo detectFileType(String filePath) throws IOException {
        log.debug("Detecting file type for: {}", filePath);

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found or is not a regular file: " + filePath);
        }

        // First, check file extension
        String extension = FilenameUtils.getExtension(filePath).toLowerCase();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFilePath(filePath);
        fileInfo.setFileName(file.getName());
        fileInfo.setExtension(extension);

        // Handle known binary formats based on extension or signature
        if (isExcelFile(file, extension)) {
            fileInfo.setFileType(FileType.EXCEL);
            fileInfo.setExcelType(extension.equalsIgnoreCase("xlsx") ? ExcelType.XLSX : ExcelType.XLS);
            return fileInfo;
        }

        // For text-based formats, we need to analyze content
        try {
            analyzeTextFileContent(file, fileInfo);
        } catch (Exception e) {
            log.warn("Error analyzing file content: {}", e.getMessage());
            // Fallback to extension-based detection if content analysis fails
            fileInfo.setFileType(detectTypeFromExtension(extension));
        }

        log.info("Detected file type: {} for file: {}", fileInfo.getFileType(), filePath);
        return fileInfo;
    }

    /**
     * Analyzes the content of a text file to determine its format
     */
    private void analyzeTextFileContent(File file, FileInfo fileInfo) throws IOException {
        Path path = file.toPath();

        // Read a sample of the file for analysis
        byte[] fileBytes = Files.readAllBytes(path);
        if (fileBytes.length == 0) {
            fileInfo.setFileType(FileType.EMPTY);
            return;
        }

        // Limit sample size for large files
        int sampleSize = Math.min(fileBytes.length, MAX_CHARS_TO_SCAN);
        String sample = new String(fileBytes, 0, sampleSize, StandardCharsets.UTF_8);
        String[] lines = sample.split("\\r?\\n", MAX_LINES_TO_SCAN + 1);

        // Check for JSON
        if (isJsonContent(lines)) {
            fileInfo.setFileType(FileType.JSON);
            return;
        }

        // Check for XML
        if (isXmlContent(lines)) {
            fileInfo.setFileType(FileType.XML);
            return;
        }

        // Check for CSV-like formats
        if (lines.length > 1) {
            char delimiter = detectDelimiter(lines);
            if (delimiter != '\0') {
                fileInfo.setFileType(delimiter == '\t' ? FileType.TSV : FileType.CSV);
                fileInfo.setDelimiter(delimiter);

                // Detect if file has headers
                fileInfo.setHasHeaders(detectHeaders(lines, delimiter));

                // Extract header info if available
                if (fileInfo.isHasHeaders() && lines.length > 0) {
                    fileInfo.setHeaders(parseHeaders(lines[0], delimiter));
                }

                return;
            }
        }

        // Check for fixed-width format
        if (isFixedWidthFormat(lines)) {
            fileInfo.setFileType(FileType.FIXED_WIDTH);
            return;
        }

        // Default to plain text if no specific format detected
        fileInfo.setFileType(FileType.TEXT);
    }

    /**
     * Checks if content matches JSON format patterns
     */
    private boolean isJsonContent(String[] lines) {
        if (lines.length == 0) return false;

        // Join lines for pattern matching
        String content = String.join("", lines).trim();

        // Check if content starts with { or [
        return (content.startsWith("{") && JSON_OBJECT_PATTERN.matcher(content).find()) ||
                (content.startsWith("[") && JSON_ARRAY_PATTERN.matcher(content).find());
    }

    /**
     * Checks if content matches XML format patterns
     */
    private boolean isXmlContent(String[] lines) {
        if (lines.length == 0) return false;

        // Join first few lines for pattern matching
        String content = String.join("", lines).trim();

        // Look for XML declaration or opening tag
        return XML_PATTERN.matcher(content).find();
    }

    /**
     * Detects the delimiter character used in CSV-like files
     */
    private char detectDelimiter(String[] lines) {
        if (lines.length < MIN_ROWS_TO_ANALYZE) {
            return '\0'; // Not enough data
        }

        Map<Character, Integer> delimiterScores = new HashMap<>();

        // Initialize scores for potential delimiters
        for (char delimiter : POTENTIAL_DELIMITERS) {
            delimiterScores.put(delimiter, 0);
        }

        // Analyze lines to find the most consistent delimiter
        for (int i = 0; i < Math.min(lines.length, MAX_LINES_TO_SCAN); i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            for (char delimiter : POTENTIAL_DELIMITERS) {
                int count = StringUtils.countMatches(line, String.valueOf(delimiter));

                // Only consider if delimiter appears multiple times
                if (count >= MIN_DELIMITER_COUNT) {
                    delimiterScores.put(delimiter, delimiterScores.get(delimiter) + 1);
                }
            }
        }

        // Find the delimiter with the highest score
        return delimiterScores.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse('\0');
    }

    /**
     * Detects if the file has headers by comparing the data type patterns
     */
    private boolean detectHeaders(String[] lines, char delimiter) {
        if (lines.length < 2) return false;

        String[] possibleHeaders = lines[0].split(String.valueOf(delimiter), -1);
        String[] firstDataRow = lines[1].split(String.valueOf(delimiter), -1);

        // Check if header and data rows have the same number of columns
        if (possibleHeaders.length != firstDataRow.length) {
            return false;
        }

        // Check if headers are text and data rows follow a different pattern
        int textHeaderCount = 0;
        int patternDifferenceCount = 0;

        for (int i = 0; i < possibleHeaders.length; i++) {
            String header = possibleHeaders[i].trim();
            String data = firstDataRow[i].trim();

            // Check if header appears to be a text label
            boolean headerIsLabel = !header.isEmpty() && !isNumeric(header);
            if (headerIsLabel) textHeaderCount++;

            // Check if pattern differs between header and data
            boolean patternDiffers = headerIsLabel && (isNumeric(data) || data.isEmpty());
            if (patternDiffers) patternDifferenceCount++;
        }

        // Headers likely if there are several text headers and pattern differences
        return textHeaderCount >= (possibleHeaders.length / 2.0) &&
                patternDifferenceCount > 0;
    }

    /**
     * Parses headers from the header line
     */
    private String[] parseHeaders(String headerLine, char delimiter) {
        String[] headers = headerLine.split(String.valueOf(delimiter), -1);

        // Trim whitespace from headers
        for (int i = 0; i < headers.length; i++) {
            headers[i] = headers[i].trim();
        }

        return headers;
    }

    /**
     * Checks if the file has a fixed-width format by analyzing line lengths and patterns
     */
    private boolean isFixedWidthFormat(String[] lines) {
        if (lines.length < MIN_ROWS_TO_ANALYZE) {
            return false;
        }

        // Check if all non-empty lines have the same length
        int firstLineLength = lines[0].length();
        int consistentLengthCount = 0;

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (!line.trim().isEmpty() && line.length() == firstLineLength) {
                consistentLengthCount++;
            }
        }

        // Consider fixed width if most lines have consistent length
        return consistentLengthCount >= (lines.length - 1) * 0.7;
    }

    /**
     * Checks if a string is numeric
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        // Check if string is a number (integer or decimal)
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    /**
     * Detects if the file is an Excel file based on extension or file signature
     */
    private boolean isExcelFile(File file, String extension) throws IOException {
        // Check by extension first
        if ("xls".equalsIgnoreCase(extension) || "xlsx".equalsIgnoreCase(extension)) {
            return true;
        }

        // If extension doesn't match, check by file signature
        byte[] fileBytes = new byte[Math.max(XLS_SIGNATURE.length, XLSX_SIGNATURE.length)];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = fis.read(fileBytes);
            if (bytesRead < 4) return false;

            // Check for XLS signature
            if (startsWithSignature(fileBytes, XLS_SIGNATURE)) {
                return true;
            }

            // Check for XLSX signature
            return startsWithSignature(fileBytes, XLSX_SIGNATURE);
        }
    }

    /**
     * Checks if a byte array starts with the given signature
     */
    private boolean startsWithSignature(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Detects file type based on file extension as a fallback method
     */
    private FileType detectTypeFromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return FileType.UNKNOWN;
        }

        switch (extension.toLowerCase()) {
            case "csv":
                return FileType.CSV;
            case "tsv":
                return FileType.TSV;
            case "json":
                return FileType.JSON;
            case "xml":
                return FileType.XML;
            case "xls":
            case "xlsx":
                return FileType.EXCEL;
            case "txt":
                return FileType.TEXT;
            default:
                return FileType.UNKNOWN;
        }
    }

    /**
     * Enum representing supported file types
     */
    public enum FileType {
        CSV,
        TSV,
        JSON,
        XML,
        EXCEL,
        FIXED_WIDTH,
        TEXT,
        EMPTY,
        UNKNOWN
    }

    /**
     * Enum representing Excel file types
     */
    public enum ExcelType {
        XLS,
        XLSX
    }
//    public static FileType detectFileType(String filePath) throws IOException {
//        return new FileTypeDetector().detectFileType(filePath).getFileType();
//    }

    /**
     * Class that holds information about a detected file
     */
    @Setter
    @Getter
    public static class FileInfo {
        private String filePath;
        private String fileName;
        private String extension;
        private FileType fileType = FileType.UNKNOWN;
        private char delimiter;
        private boolean hasHeaders;
        private String[] headers;
        private ExcelType excelType;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("FileInfo[")
                    .append("fileType=").append(fileType)
                    .append(", fileName=").append(fileName);

            if (fileType == FileType.CSV || fileType == FileType.TSV) {
                builder.append(", delimiter='").append(delimiter).append("'")
                        .append(", hasHeaders=").append(hasHeaders);
                if (hasHeaders && headers != null) {
                    builder.append(", headers=").append(Arrays.toString(headers));
                }
            } else if (fileType == FileType.EXCEL && excelType != null) {
                builder.append(", excelType=").append(excelType);
            }

            builder.append("]");
            return builder.toString();
        }
    }
}