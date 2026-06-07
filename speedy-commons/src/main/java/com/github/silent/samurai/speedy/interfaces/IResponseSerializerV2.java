package com.github.silent.samurai.speedy.interfaces;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.models.SpeedyBatchResponse;
import com.github.silent.samurai.speedy.models.SpeedyCountResponse;
import com.github.silent.samurai.speedy.models.SpeedyEntityResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface IResponseSerializerV2 {

    String getContentType();

    void writeEntityList(SpeedyEntityResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException;

    void writeCount(SpeedyCountResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException;

    void writeBatch(SpeedyBatchResponse response, HttpServletResponse httpResponse) throws SpeedyHttpException;

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
        }
    }

}
