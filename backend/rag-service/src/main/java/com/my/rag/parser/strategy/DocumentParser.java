package com.my.rag.parser.strategy;

import com.my.rag.document.entity.RagDocument;
import com.my.rag.parser.dto.ParsedDocument;

public interface DocumentParser {

    boolean supports(String fileType);

    ParsedDocument parse(RagDocument document);
}

