package com.github.silent.samurai.speedy.serialization;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.response.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.metadata.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.MetaModel;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponseWriter;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.*;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/// Format-agnostic {@link IResponseSerializerV2}: owns the envelope composition for every
/// response type (paging metadata, batch succeeded/failed, error fields, the metamodel
/// document) and the entity traversal (via {@link SpeedyToStructure}), emitting structural
/// tokens to a per-call {@link SpeedyResponseWriter}. The only format-specific piece is the
/// writer; a new output format implements that sink and reuses all the logic here.
public class DefaultResponseSerializer implements IResponseSerializerV2 {

    /// The negotiated media type (e.g. {@code application/json}) — the format identity,
    /// returned by {@link #getContentType()}.
    private final String contentType;
    /// The same type with an explicit {@code ;charset=UTF-8}, used only for the wire
    /// {@code Content-Type} header (see {@link #withUtf8Charset}).
    private final String wireContentType;
    private final SpeedyResponseWriter writer;

    public DefaultResponseSerializer(String contentType, SpeedyResponseWriter writer) {
        this.contentType = contentType;
        this.wireContentType = withUtf8Charset(contentType);
        this.writer = writer;
    }

    /// Speedy always writes UTF-8 bytes, so the response {@code Content-Type} header declares the
    /// charset explicitly instead of relying on servlet-container defaults (which only some
    /// media types, e.g. {@code application/json}, receive for free). Idempotent: a content
    /// type that already names a charset is returned unchanged. This is a wire-encoding detail
    /// only — {@link #getContentType()} still reports the bare negotiated media type.
    private static String withUtf8Charset(String contentType) {
        if (contentType == null || contentType.toLowerCase().contains("charset=")) {
            return contentType;
        }
        return contentType + ";charset=" + StandardCharsets.UTF_8.name();
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void writeEntityList(SpeedyEntityResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        SpeedyResponseWriter w = writer;
        SpeedyToStructure walker = new SpeedyToStructure(response.getFieldPredicate());

        w.startObject();
        w.field("payload");
        walker.writeCollection(response.getPayload(), response.getEntityMetadata(), response.getExpands(), w);
        w.field("pageIndex");
        if (response.getPageIndex() != null) {
            w.writeInt(response.getPageIndex());
        } else {
            w.writeNull();
        }
        w.field("pageSize");
        w.writeInt(response.getPayload().size());
        if (response.getTotalCount() != null) {
            w.field("totalCount");
            w.writeInt(response.getTotalCount().longValue());
            w.field("totalPages");
            w.writeInt(calculateTotalPages(response));
        }
        w.endObject();

        w.finish(httpResponse, response.getStatus(), response.getHeaders(), wireContentType);
    }

    @Override
    public void writeCount(SpeedyCountResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        SpeedyResponseWriter w = writer;
        w.startObject();
        w.field("count");
        BigInteger count = response.getCount();
        if (count == null) {
            w.writeNull();
        } else {
            w.writeInt(count.longValue());
        }
        w.endObject();
        w.finish(httpResponse, response.getStatus(), response.getHeaders(), wireContentType);
    }

    @Override
    public void writeError(SpeedyErrorResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        SpeedyResponseWriter w = writer;
        w.reset(); // discard any partial document if a prior write failed mid-stream
        w.startObject();
        w.field("status");
        w.writeInt(response.getStatus());
        w.field("message");
        w.writeText(response.getMessage());
        w.field("timestamp");
        w.writeText(LocalDateTime.now().toString());
        w.endObject();
        w.finish(httpResponse, response.getStatus(), response.getHeaders(), wireContentType);
    }

    @Override
    public void writeMetadata(SpeedyMetadataResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        SpeedyResponseWriter w = writer;
        writeMetaModel(response.getMetaModel(), w);
        w.finish(httpResponse, response.getStatus(), response.getHeaders(), wireContentType);
    }

    private void writeEntityKeys(SpeedyEntity entity, SpeedyResponseWriter w) throws SpeedyHttpException {
        w.startObject();
        for (KeyFieldMetadata keyField : entity.getMetadata().getKeyFields()) {
            w.field(keyField);
            SpeedyValue value = entity.get(keyField);
            if (value == null || value.isNull()) {
                w.writeNull();
            } else {
                w.writeLeaf(value.getValueType(), value);
            }
        }
        w.endObject();
    }

    private int calculateTotalPages(SpeedyEntityResponse response) {
        int requestedPageSize = response.getRequestedPageSize();
        if (requestedPageSize <= 0) {
            return 1;
        }
        long totalCount = response.getTotalCount().longValue();
        return (int) Math.ceil((double) totalCount / requestedPageSize);
    }

    private void writeMetaModel(MetaModel metaModel, SpeedyResponseWriter w) throws SpeedyHttpException {
        w.startArray();
        for (EntityMetadata entityMetadata : metaModel.getAllEntityMetadata()) {
            writeEntityMetaModel(entityMetadata, w);
        }
        w.endArray();
    }

    private void writeEntityMetaModel(EntityMetadata entityMetadata, SpeedyResponseWriter w) throws SpeedyHttpException {
        w.startObject();
        w.field("name");
        w.writeText(entityMetadata.getName());
        w.field("hasCompositeKey");
        w.writeBool(entityMetadata.hasCompositeKey());
        w.field("sensitive");
        w.writeBool(entityMetadata.isSensitive());

        w.field("fields");
        w.startArray();
        for (FieldMetadata fieldMetadata : entityMetadata.getAllFields()) {
            writeFieldMetadata(fieldMetadata, w);
        }
        w.endArray();

        w.field("keyFields");
        w.startArray();
        for (FieldMetadata fieldMetadata : entityMetadata.getKeyFields()) {
            writeFieldMetadata(fieldMetadata, w);
        }
        w.endArray();

        w.endObject();
    }

    private void writeFieldMetadata(FieldMetadata fieldMetadata, SpeedyResponseWriter w) throws SpeedyHttpException {
        w.startObject();
        w.field("outputProperty");
        w.writeText(fieldMetadata.getOutputPropertyName());
        w.field("isAssociation");
        w.writeBool(fieldMetadata.isAssociation());

        if (fieldMetadata instanceof KeyFieldMetadata keyFieldMetadata) {
            w.field("isKeyField");
            w.writeBool(keyFieldMetadata.isKeyField());
            w.field("isKeyGenerated");
            w.writeBool(keyFieldMetadata.shouldGenerateKey());
        }

        if (fieldMetadata.isAssociation()) {
            w.field("associatedWith");
            w.writeText(fieldMetadata.getAssociationMetadata().getName());
            w.field("associatedField");
            w.writeText(fieldMetadata.getAssociatedFieldMetadata().getOutputPropertyName());
        }

        w.field("fieldType");
        w.writeText(fieldMetadata.getValueType().name());
        w.field("isNullable");
        w.writeBool(fieldMetadata.isNullable());
        w.field("isCollection");
        w.writeBool(fieldMetadata.isCollection());
        w.field("isSerializable");
        w.writeBool(fieldMetadata.isSerializable());
        w.field("isDeserializable");
        w.writeBool(fieldMetadata.isDeserializable());
        w.field("isUnique");
        w.writeBool(fieldMetadata.isUnique());
        w.field("sensitive");
        w.writeBool(fieldMetadata.isSensitive());
        w.endObject();
    }
}
