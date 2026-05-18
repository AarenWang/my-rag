package com.my.rag.retrieval.repository;

import com.my.rag.retrieval.dto.RetrievedChunk;
import java.util.List;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RetrievalMapper {

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
                (1 - (e.embedding &lt;=&gt; #{questionVector}::vector)) AS score
            FROM rag_chunk_embedding e
            JOIN rag_document_chunk c ON c.id = e.chunk_id
            JOIN rag_document d ON d.id = c.document_id
            WHERE d.status = 'READY'
              AND e.embedding_model = #{embeddingModel}
              AND (1 - (e.embedding &lt;=&gt; #{questionVector}::vector)) &gt;= #{scoreThreshold}
              <if test="documentIds != null and documentIds.size() > 0">
              AND d.id IN
                <foreach collection="documentIds" item="documentId" open="(" separator="," close=")">
                  #{documentId}
                </foreach>
              </if>
            ORDER BY e.embedding &lt;=&gt; #{questionVector}::vector
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
    List<RetrievedChunk> search(
            @Param("questionVector") String questionVector,
            @Param("embeddingModel") String embeddingModel,
            @Param("documentIds") List<Long> documentIds,
            @Param("topK") int topK,
            @Param("scoreThreshold") double scoreThreshold);
}
