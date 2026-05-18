package com.my.rag.parser.strategy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class EpubMetadataDebug {

    public static void main(String[] args) throws Exception {
        String epubPath = "C:\\Users\\wangr\\Downloads\\黄仁勋：英伟达之芯 (【美】斯蒂芬·威特) .epub";
        Path path = Paths.get(epubPath);
        
        if (!path.toFile().exists()) {
            System.err.println("Test file not found at: " + epubPath);
            return;
        }

        System.out.println("=".repeat(80));
        System.out.println("EPUB Metadata Debug");
        System.out.println("=".repeat(80));
        System.out.println("EPUB file: " + epubPath);
        System.out.println();

        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            System.out.println("Step 1: Finding metadata files...");
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                
                if (name.endsWith("content.opf") || name.endsWith("toc.ncx")) {
                    System.out.println("  Found: " + name);
                    
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        System.out.println();
                        System.out.println("=== " + name + " ===");
                        System.out.println(content);
                        System.out.println("=== END OF " + name + " ===");
                        System.out.println();
                    } catch (Exception e) {
                        System.err.println("  Failed to read " + name + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to parse EPUB");
            e.printStackTrace();
            throw e;
        }

        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("Done!");
        System.out.println("=".repeat(80));
    }
}
