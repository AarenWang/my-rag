package com.my.rag.document.service;

import com.my.rag.document.entity.RagDocument;
import com.my.rag.document.enums.DocumentStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DocumentLifecycleService {

    private static final Map<DocumentStatus, Set<DocumentStatus>> ALLOWED_TRANSITIONS =
            buildAllowedTransitions();

    public boolean canTransition(DocumentStatus currentStatus, DocumentStatus nextStatus) {
        if (currentStatus == null || nextStatus == null) {
            return false;
        }
        if (currentStatus == nextStatus) {
            return true;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus);
    }

    public void assertTransitionAllowed(DocumentStatus currentStatus, DocumentStatus nextStatus) {
        if (!canTransition(currentStatus, nextStatus)) {
            throw new IllegalStateException(
                    "Illegal document status transition: " + currentStatus + " -> " + nextStatus);
        }
    }

    public void moveTo(RagDocument document, DocumentStatus nextStatus) {
        assertTransitionAllowed(document.getStatus(), nextStatus);
        document.setStatus(nextStatus);
        if (nextStatus != DocumentStatus.FAILED) {
            document.setErrorMessage(null);
        }
    }

    public void fail(RagDocument document, String errorMessage) {
        assertTransitionAllowed(document.getStatus(), DocumentStatus.FAILED);
        document.setStatus(DocumentStatus.FAILED);
        document.setErrorMessage(errorMessage);
    }

    private static Map<DocumentStatus, Set<DocumentStatus>> buildAllowedTransitions() {
        Map<DocumentStatus, Set<DocumentStatus>> transitions = new EnumMap<>(DocumentStatus.class);
        transitions.put(DocumentStatus.UPLOADED, EnumSet.of(DocumentStatus.PARSING, DocumentStatus.FAILED));
        transitions.put(DocumentStatus.PARSING, EnumSet.of(DocumentStatus.PARSED, DocumentStatus.FAILED));
        transitions.put(DocumentStatus.PARSED, EnumSet.of(DocumentStatus.CHUNKING, DocumentStatus.FAILED));
        transitions.put(DocumentStatus.CHUNKING, EnumSet.of(DocumentStatus.CHUNKED, DocumentStatus.FAILED));
        transitions.put(DocumentStatus.CHUNKED, EnumSet.of(DocumentStatus.EMBEDDING, DocumentStatus.FAILED));
        transitions.put(DocumentStatus.EMBEDDING, EnumSet.of(DocumentStatus.READY, DocumentStatus.FAILED));
        transitions.put(DocumentStatus.READY, EnumSet.of(DocumentStatus.CHUNKING, DocumentStatus.EMBEDDING, DocumentStatus.FAILED));
        transitions.put(DocumentStatus.FAILED, EnumSet.of(DocumentStatus.PARSING, DocumentStatus.CHUNKING, DocumentStatus.EMBEDDING));
        return transitions;
    }
}

