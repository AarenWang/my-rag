package com.my.rag.document.service;

@FunctionalInterface
public interface IndexProgressListener {

    void update(String stage, int progressPercent, String message, Integer chunkCount);
}
