package com.github.silent.samurai.speedy.serialization;

import com.github.silent.samurai.speedy.enums.ValueType;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.KeyFieldMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponseWriter;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.models.*;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigInteger;
import java.time.LocalDateTime;

/// Format-agnostic {@link IResponseSerializerV2}: owns the envelope composition for every
/// response type (paging metadata, batch succeeded/failed, error fields, the metamodel
/// document) and the entity traversal (via {@link ResponseWalker}), emitting structural
/// tokens to a per-call {@link SpeedyResponseWriter}. The only format-specific piece is the
/// writer; a new output format implements that sink and reuses all the logic here.
public class WalkingResponseSerializer implements IResponseSerializerV2 {

    private final String contentType;
    private final SpeedyResponseWriter writer;

    public WalkingResponseSerializer(String contentType, SpeedyResponseWriter writer) {
        this.contentType = contentType;
        this.writer = writer;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void writeEntityList(SpeedyEntityResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        SpeedyResponseWriter w = writer;
        ResponseWalker walker = new ResponseWalker(response.getFieldPredicate());

        w.startObject();
        w.field("payload");
        walker.writeCollection(response.getPayload(), response.getEntityMetadata(), response.getExpands(), w);
        w.field("pageIndex");
        if (response.getPageIndex() != null) {
            w.writeLeaf(ValueType.INT, new SpeedyInt((long) response.getPageIndex()));
        } else {
            w.writeNull();
        }
        w.field("pageSize");
        w.writeLeaf(ValueType.INT, new SpeedyInt((long) response.getPayload().size()));
        if (response.getTotalCount() != null) {
            w.field("totalCount");
            w.writeLeaf(ValueType.INT, new SpeedyInt(response.getTotalCount().longValue()));
            w.field("totalPages");
            w.writeLeaf(ValueType.INT, new SpeedyInt((long) calculateTotalPages(response)));
        }
        w.endObject();

        w.finish(httpResponse, response.getStatus(), response.getHeaders(), contentType);
    }

    @Override
    public void writeCount(SpeedyCountResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        SpeedyResponseWriter w = writer;
        w.startObject();
        w.field("count");
        BigInteger count = response.getCount();
        w.writeLeaf(ValueType.INT, new SpeedyInt(count == null ? null : count.longValue()));
        w.endObject();
        w.finish(httpResponse, response.getStatus(), response.getHeaders(), contentType);
    }

    @Override
    public void writeBatch(SpeedyBatchResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        SpeedyResponseWriter w = writer;
        w.startObject();

        w.field("succeeded");
        w.startArray();
        for (SpeedyEntity entity : response.getSucceeded()) {
            writeEntityKeys(entity, w);
        }
        w.endArray();

        w.field("failed");
        w.startArray();
        for (SpeedyPartialFailure failure : response.getFailed()) {
            w.startObject();
            w.field("index");
            w.writeLeaf(ValueType.INT, new SpeedyInt((long) failure.getIndex()));
            w.field("status");
            w.writeLeaf(ValueType.INT, new SpeedyInt((long) failure.getStatus()));
            w.field("message");
            w.writeLeaf(ValueType.TEXT, new SpeedyText(failure.getMessage()));
            w.field("timestamp");
            w.writeLeaf(ValueType.TEXT, new SpeedyText(failure.getTimestamp()));
            w.field("inputPk");
            if (failure.getInputPk() != null) {
                writeEntityKeys(failure.getInputPk(), w);
            } else {
                w.writeNull();
            }
            w.endObject();
        }
        w.endArray();

        w.field("pageIndex");
        w.writeLeaf(ValueType.INT, new SpeedyInt((long) response.getPageIndex()));
        w.endObject();

        w.finish(httpResponse, response.getStatus(), response.getHeaders(), contentType);
    }

    @Override
    public void writeError(SpeedyErrorResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        SpeedyResponseWriter w = writer;
        w.startObject();
        w.field("status");
        w.writeLeaf(ValueType.INT, new SpeedyInt((long) response.getStatus()));
        w.field("message");
        w.writeLeaf(ValueType.TEXT, new SpeedyText(response.getMessage()));
        w.field("timestamp");
        w.writeLeaf(ValueType.TEXT, new SpeedyText(LocalDateTime.now().toString()));
        w.endObject();
        w.finish(httpResponse, response.getStatus(), response.getHeaders(), contentType);
    }

    @Override
    public void writeMetadata(SpeedyMetadataResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        SpeedyResponseWriter w = writer;
        writeMetaModel(response.getMetaModel(), w);
        w.finish(httpResponse, response.getStatus(), response.getHeaders(), contentType);
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
        w.writeLeaf(ValueType.TEXT, new SpeedyText(entityMetadata.getName()));
        w.field("hasCompositeKey");
        w.writeLeaf(ValueType.BOOL, new SpeedyBoolean(entityMetadata.hasCompositeKey()));
        w.field("sensitive");
        w.writeLeaf(ValueType.BOOL, new SpeedyBoolean(entityMetadata.isSensitive()));

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
        w.writeLeaf(ValueType.TEXT, new SpeedyText(fieldMetadata.getOutputPropertyName()));
        w.field("isAssociation");
        w.writeLeaf(ValueType.BOOL, new SpeedyBoolean(fieldMetadata.isAssociation()));

        if (fieldMetadata instanceof KeyFieldMetadata keyFieldMetadata) {
            w.field("isKeyField");
            w.writeLeaf(ValueType.BOOL, new SpeedyBoolean(keyFieldMetadata.isKeyField()));
            w.field("isKeyGenerated");
            w.writeLeaf(ValueType.BOOL, new SpeedyBoolean(keyFieldMetadata.shouldGenerateKey()));
        }

        if (fieldMetadata.isAssociation()) {
            w.field("associatedWith");
            w.writeLeaf(ValueType.TEXT, new SpeedyText(fieldMetadata.getAssociationMetadata().getName()));
            w.field("associatedField");
            w.writeLeaf(ValueType.TEXT, new SpeedyText(fieldMetadata.getAssociatedFieldMetadata().getOutputPropertyName()));
        }

        w.field("fieldType");
        w.writeLeaf(ValueType.TEXT, new SpeedyText(fieldMetadata.getValueType().name()));
        w.field("isNullable");
        w.writeLeaf(ValueType.BOOL, new SpeedyBoolean(fieldMetadata.isNullable()));
        w.field("isCollection");
        w.writeLeaf(ValueType.BOOL, new SpeedyBoolean(fieldMetadata.isCollection()));
        w.field("isSerializable");
        w.writeLeaf(ValueType.BOOL, new SpeedyBoolean(fieldMetadata.isSerializable()));
        w.field("isDeserializable");
        w.writeLeaf(ValueType.BOOL, new SpeedyBoolean(fieldMetadata.isDeserializable()));
        w.field("isUnique");
        w.writeLeaf(ValueType.BOOL, new SpeedyBoolean(fieldMetadata.isUnique()));
        w.field("sensitive");
        w.writeLeaf(ValueType.BOOL, new SpeedyBoolean(fieldMetadata.isSensitive()));
        w.endObject();
    }
}
