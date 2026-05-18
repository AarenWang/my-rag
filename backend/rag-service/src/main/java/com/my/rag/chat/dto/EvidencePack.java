package com.my.rag.chat.dto;

import java.util.List;

public record EvidencePack(String question, List<Evidence> evidences) {}
