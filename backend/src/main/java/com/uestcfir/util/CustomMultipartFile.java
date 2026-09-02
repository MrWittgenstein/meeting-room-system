package com.uestcfir.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CustomMultipartFile implements MultipartFile {
    
    private final byte[] fileData;
    private final String fileName;
    private final String contentType;
    
    public CustomMultipartFile(byte[] fileData, String fileName, String contentType) {
        this.fileData = fileData;
        this.fileName = fileName;
        this.contentType = contentType;
    }
    
    @Override
    public String getName() {
        return "file";
    }
    
    @Override
    public String getOriginalFilename() {
        return fileName;
    }
    
    @Override
    public String getContentType() {
        return contentType;
    }
    
    @Override
    public boolean isEmpty() {
        return fileData == null || fileData.length == 0;
    }
    
    @Override
    public long getSize() {
        return fileData != null ? fileData.length : 0;
    }
    
    @Override
    public byte[] getBytes() throws IOException {
        return fileData;
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(fileData);
    }
    
    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        java.nio.file.Files.write(dest.toPath(), fileData);
    }
}