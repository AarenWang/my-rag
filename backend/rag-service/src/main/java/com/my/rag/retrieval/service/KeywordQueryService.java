package com.my.rag.retrieval.service;

import com.my.rag.config.RagProperties;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KeywordQueryService {

    private static final Pattern CONFIG_TOKEN_PATTERN = Pattern.compile("\\b[A-Z][A-Z0-9_]{2,}\\b");
    private static final Pattern CODE_TOKEN_PATTERN = Pattern.compile("\\b[A-Za-z][A-Za-z0-9_.$-]{2,}\\b");
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("第\\s*[一二三四五六七八九十百千万0-9]+\\s*[章节篇]");
    private static final Pattern NUMBER_CONDITION_PATTERN = Pattern.compile(
            "\\d+(?:\\.\\d+)?\\s*(?:天|小时|分钟|元|维|token|tokens|章|节)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HAN_RUN_PATTERN = Pattern.compile("[\\p{IsHan}]{2,}");

    private static final Set<String> ENGLISH_STOP_WORDS = Set.of(
            "what", "where", "when", "which", "who", "why", "how", "is", "are", "the", "and", "or",
            "can", "could", "should", "would", "does", "did", "do", "with", "about", "from");
    private static final List<String> CHINESE_STOP_WORDS = List.of(
            "是什么意思", "有什么", "有哪些", "在哪里", "是什么", "为什么", "请问", "一下", "相关",
            "内容", "如何", "怎么", "哪里", "哪个", "是否", "能否", "可以", "应该", "需要",
            "的是", "什么", "以及", "或者", "如果", "那么", "这个", "那个", "一个", "一些",
            "的", "了", "和", "与", "或", "在", "是", "有", "吗", "呢", "啊");
    private static final List<String> INTENT_TERMS = List.of(
            "区别", "对比", "配置", "设置", "价格", "费用", "多少", "哪里", "章节", "退货",
            "签收", "引用", "来源", "说明", "解释");

    private final RagProperties ragProperties;

    public KeywordQueryService(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    public List<String> generate(String question) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }

        String normalized = normalize(question);
        List<String> strongTokens = extractStrongTokens(normalized);
        List<String> chapterTokens = extractMatches(CHAPTER_PATTERN, normalized);
        List<String> numberTokens = extractMatches(NUMBER_CONDITION_PATTERN, normalized);
        List<String> chineseTerms = extractChineseTerms(normalized);
        List<String> englishTerms = extractEnglishTerms(normalized);
        List<String> intentTerms = extractIntentTerms(normalized);

        LinkedHashSet<String> queries = new LinkedHashSet<>();

        String exactQuery = join(strongTokens, chapterTokens, numberTokens, intentTerms);
        if (hasSpecificSignal(strongTokens, chapterTokens, numberTokens) && StringUtils.hasText(exactQuery)) {
            queries.add(exactQuery);
        }

        String entityQuery = join(chineseTerms, englishTerms, chapterTokens, numberTokens);
        if (StringUtils.hasText(entityQuery)) {
            queries.add(entityQuery);
        }

        String broadQuery = compactWithoutStopWords(normalized);
        if (StringUtils.hasText(broadQuery)) {
            queries.add(broadQuery);
        }

        if (queries.isEmpty()) {
            queries.add(normalized);
        }

        int maxQueries = Math.max(1, ragProperties.getRetrieval().getKeywordMaxQueries());
        return queries.stream()
                .map(this::normalizeSpaces)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(maxQueries)
                .toList();
    }

    private String normalize(String question) {
        String normalized = Normalizer.normalize(question, Normalizer.Form.NFKC);
        return normalizeSpaces(normalized.replaceAll("[，。！？；：、（）【】《》“”‘’]", " "));
    }

    private List<String> extractStrongTokens(String text) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        tokens.addAll(extractMatches(CONFIG_TOKEN_PATTERN, text));
        tokens.addAll(extractEnglishTerms(text));
        return List.copyOf(tokens);
    }

    private List<String> extractEnglishTerms(String text) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        Matcher matcher = CODE_TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            if (ENGLISH_STOP_WORDS.contains(token.toLowerCase())) {
                continue;
            }
            tokens.add(token);
        }
        return List.copyOf(tokens);
    }

    private List<String> extractChineseTerms(String text) {
        String withoutStopWords = replaceStopWords(text);
        Matcher matcher = HAN_RUN_PATTERN.matcher(withoutStopWords);
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        while (matcher.find()) {
            String term = matcher.group();
            if (term.length() < 2) {
                continue;
            }
            terms.add(term);
            if (term.length() > 6) {
                terms.add(term.substring(0, 6));
                terms.add(term.substring(term.length() - 6));
            }
        }
        return terms.stream().limit(8).toList();
    }

    private List<String> extractIntentTerms(String text) {
        List<String> terms = new ArrayList<>();
        for (String intentTerm : INTENT_TERMS) {
            if (text.contains(intentTerm)) {
                terms.add(intentTerm);
            }
        }
        return terms;
    }

    private List<String> extractMatches(Pattern pattern, String text) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            tokens.add(normalizeSpaces(matcher.group()));
        }
        return List.copyOf(tokens);
    }

    @SafeVarargs
    private final String join(List<String>... tokenGroups) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (List<String> tokenGroup : tokenGroups) {
            tokens.addAll(tokenGroup);
        }
        return normalizeSpaces(String.join(" ", tokens));
    }

    private boolean hasSpecificSignal(List<String> strongTokens, List<String> chapterTokens, List<String> numberTokens) {
        return !strongTokens.isEmpty() || !chapterTokens.isEmpty() || !numberTokens.isEmpty();
    }

    private String compactWithoutStopWords(String text) {
        return normalizeSpaces(replaceStopWords(text).replaceAll("[^\\p{IsHan}A-Za-z0-9_.$-]+", " "));
    }

    private String replaceStopWords(String text) {
        String result = text;
        for (String stopWord : CHINESE_STOP_WORDS) {
            result = result.replace(stopWord, " ");
        }
        return result;
    }

    private String normalizeSpaces(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }
}
