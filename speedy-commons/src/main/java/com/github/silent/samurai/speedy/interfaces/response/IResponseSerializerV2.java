package com.github.silent.samurai.speedy.interfaces.response;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.models.SpeedyBatchResponse;
import com.github.silent.samurai.speedy.models.SpeedyCountResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import com.github.silent.samurai.speedy.models.SpeedyErrorResponse;
import com.github.silent.samurai.speedy.models.SpeedyMetadataResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface IResponseSerializerV2 {

    String getContentType();

    void writeEntityList(SpeedyEntityResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException;

    void writeCount(SpeedyCountResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException;

    void writeBatch(SpeedyBatchResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException;

    /// Writes a server-level error document (status + message). Not tied to any entity.
    void writeError(SpeedyErrorResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException;

    /// Writes the metamodel description document (the `$metadata` endpoint). Not tied to any entity.
    void writeMetadata(SpeedyMetadataResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException;

    default void write(SpeedyResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException {
        switch (response.getType()) {
            case ENTITY_LIST:
                writeEntityList((SpeedyEntityResponse) response, httpResponse);
                break;
            case COUNT:
                writeCount((SpeedyCountResponse) response, httpResponse);
                break;
            case BATCH_RESULT:
                writeBatch((SpeedyBatchResponse) response, httpResponse);
                break;
            case ERROR:
                writeError((SpeedyErrorResponse) response, httpResponse);
                break;
            case METADATA:
                writeMetadata((SpeedyMetadataResponse) response, httpResponse);
                break;
        }
    }

}
