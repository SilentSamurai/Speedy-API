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
import java.time.LocalDateTime;

/// Format-agnostic {@link IResponseSerializerV2}: owns the envelope composition for every
/// response type (paging metadata, batch succeeded/failed, error fields, the metamodel
/// document) and the entity traversal (via {@link SpeedyToStructure}), emitting structural
/// tokens to a per-call {@link SpeedyResponseWriter}. The only format-specific piece is the
/// writer; a new output format implements that sink and reuses all the logic here.
public class DefaultResponseSerializer implements IResponseSerializerV2 {

    private final String contentType;
    private final SpeedyResponseWriter writer;

    public DefaultResponseSerializer(String contentType, SpeedyResponseWriter writer) {
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

        w.finish(httpResponse, response.getStatus(), response.getHeaders(), contentType);
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
            w.writeInt(failure.getIndex());
            w.field("status");
            w.writeInt(failure.getStatus());
            w.field("message");
            w.writeText(failure.getMessage());
            w.field("timestamp");
            w.writeText(failure.getTimestamp());
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
        w.writeInt(response.getPageIndex());
        w.endObject();

        w.finish(httpResponse, response.getStatus(), response.getHeaders(), contentType);
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
