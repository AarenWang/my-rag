package com.my.rag.retrieval.repository;

import com.my.rag.retrieval.dto.RetrievedChunk;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KeywordRetrievalMapper {

    @Select("""
            <script>
            SELECT
                d.id AS document_id,
                d.title AS document_title,
                c.chapter_title AS chapter_title,
                c.id AS chunk_id,
                c.chunk_index AS chunk_index,
                c.start_paragraph AS start_paragraph,
                c.end_paragraph AS end_paragraph,
                c.content AS content,
                ts_rank_cd(s.search_vector, plainto_tsquery(CAST(#{textSearchConfig} AS regconfig), #{question})) AS score
            FROM rag_chunk_search_index s
            JOIN rag_document_chunk c ON c.id = s.chunk_id
            JOIN rag_document d ON d.id = c.document_id
            WHERE d.status = 'READY'
              AND s.search_vector @@ plainto_tsquery(CAST(#{textSearchConfig} AS regconfig), #{question})
              <if test="documentIds != null and documentIds.size() > 0">
              AND d.id IN
                <foreach collection="documentIds" item="documentId" open="(" separator="," close=")">
                  #{documentId}
                </foreach>
              </if>
            ORDER BY score DESC
            LIMIT #{topK}
            </script>
            """)
    @ConstructorArgs({
            @Arg(column = "document_id", javaType = Long.class),
            @Arg(column = "document_title", javaType = String.class),
            @Arg(column = "chapter_title", javaType = String.class),
            @Arg(column = "chunk_id", javaType = Long.class),
            @Arg(column = "chunk_index", javaType = Integer.class),
            @Arg(column = "start_paragraph", javaType = Integer.class),
            @Arg(column = "end_paragraph", javaType = Integer.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "score", javaType = Double.class)
    })
    List<RetrievedChunk> searchPlain(
            @Param("question") String question,
            @Param("textSearchConfig") String textSearchConfig,
            @Param("documentIds") List<Long> documentIds,
            @Param("topK") int topK);

    @Select("""
            <script>
            SELECT
                d.id AS document_id,
                d.title AS document_title,
                c.chapter_title AS chapter_title,
                c.id AS chunk_id,
                c.chunk_index AS chunk_index,
                c.start_paragraph AS start_paragraph,
                c.end_paragraph AS end_paragraph,
                c.content AS content,
                ts_rank_cd(s.search_vector, websearch_to_tsquery(CAST(#{textSearchConfig} AS regconfig), #{question})) AS score
            FROM rag_chunk_search_index s
            JOIN rag_document_chunk c ON c.id = s.chunk_id
            JOIN rag_document d ON d.id = c.document_id
            WHERE d.status = 'READY'
              AND s.search_vector @@ websearch_to_tsquery(CAST(#{textSearchConfig} AS regconfig), #{question})
              <if test="documentIds != null and documentIds.size() > 0">
              AND d.id IN
                <foreach collection="documentIds" item="documentId" open="(" separator="," close=")">
                  #{documentId}
                </foreach>
              </if>
            ORDER BY score DESC
            LIMIT #{topK}
            </script>
            """)
    @ConstructorArgs({
            @Arg(column = "document_id", javaType = Long.class),
            @Arg(column = "document_title", javaType = String.class),
            @Arg(column = "chapter_title", javaType = String.class),
            @Arg(column = "chunk_id", javaType = Long.class),
            @Arg(column = "chunk_index", javaType = Integer.class),
            @Arg(column = "start_paragraph", javaType = Integer.class),
            @Arg(column = "end_paragraph", javaType = Integer.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "score", javaType = Double.class)
    })
    List<RetrievedChunk> searchWebsearch(
            @Param("question") String question,
            @Param("textSearchConfig") String textSearchConfig,
            @Param("documentIds") List<Long> documentIds,
            @Param("topK") int topK);

    @Select("""
            <script>
            SELECT
                d.id AS document_id,
                d.title AS document_title,
                c.chapter_title AS chapter_title,
                c.id AS chunk_id,
                c.chunk_index AS chunk_index,
                c.start_paragraph AS start_paragraph,
                c.end_paragraph AS end_paragraph,
                c.content AS content,
                ts_rank_cd(s.search_vector, phraseto_tsquery(CAST(#{textSearchConfig} AS regconfig), #{question})) AS score
            FROM rag_chunk_search_index s
            JOIN rag_document_chunk c ON c.id = s.chunk_id
            JOIN rag_document d ON d.id = c.document_id
            WHERE d.status = 'READY'
              AND s.search_vector @@ phraseto_tsquery(CAST(#{textSearchConfig} AS regconfig), #{question})
              <if test="documentIds != null and documentIds.size() > 0">
              AND d.id IN
                <foreach collection="documentIds" item="documentId" open="(" separator="," close=")">
                  #{documentId}
                </foreach>
              </if>
            ORDER BY score DESC
            LIMIT #{topK}
            </script>
            """)
    @ConstructorArgs({
            @Arg(column = "document_id", javaType = Long.class),
            @Arg(column = "document_title", javaType = String.class),
            @Arg(column = "chapter_title", javaType = String.class),
            @Arg(column = "chunk_id", javaType = Long.class),
            @Arg(column = "chunk_index", javaType = Integer.class),
            @Arg(column = "start_paragraph", javaType = Integer.class),
            @Arg(column = "end_paragraph", javaType = Integer.class),
            @Arg(column = "content", javaType = String.class),
            @Arg(column = "score", javaType = Double.class)
    })
    List<RetrievedChunk> searchPhrase(
            @Param("question") String question,
            @Param("textSearchConfig") String textSearchConfig,
            @Param("documentIds") List<Long> documentIds,
            @Param("topK") int topK);
}
