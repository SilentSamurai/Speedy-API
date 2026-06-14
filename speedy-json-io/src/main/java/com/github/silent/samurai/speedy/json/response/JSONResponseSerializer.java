package com.github.silent.samurai.speedy.json.response;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.json.registry.JsonRegistry;
import com.github.silent.samurai.speedy.models.SpeedyBatchResponse;
import com.github.silent.samurai.speedy.models.SpeedyCountResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.models.SpeedyErrorResponse;
import com.github.silent.samurai.speedy.models.SpeedyMetadataResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

/// JSON {@link IResponseSerializerV2}. Entity-agnostic and reusable: it holds only the
/// content-type-bound dependencies and delegates each response type to a single-purpose
/// writer. Per-call data (including the target entity) travels inside each response.
public class JSONResponseSerializer implements IResponseSerializerV2 {

    private final JsonEntityListWriter entityListWriter;
    private final JsonCountWriter countWriter;
    private final JsonBatchWriter batchWriter;
    private final JsonErrorWriter errorWriter;
    private final JsonMetaModelSerializer metadataWriter;

    public JSONResponseSerializer(JsonRegistry jsonRegistry) {
        this.entityListWriter = new JsonEntityListWriter(jsonRegistry);
        this.countWriter = new JsonCountWriter();
        this.batchWriter = new JsonBatchWriter(jsonRegistry);
        this.errorWriter = new JsonErrorWriter();
        this.metadataWriter = new JsonMetaModelSerializer();
    }

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public void writeEntityList(SpeedyEntityResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        entityListWriter.write(response, httpResponse);
    }

    @Override
    public void writeCount(SpeedyCountResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        countWriter.write(response, httpResponse);
    }

    @Override
    public void writeBatch(SpeedyBatchResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        batchWriter.write(response, httpResponse);
    }

    @Override
    public void writeError(SpeedyErrorResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        errorWriter.write(response, httpResponse);
    }

    @Override
    public void writeMetadata(SpeedyMetadataResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        metadataWriter.write(response, httpResponse);
    }
}
