package com.github.silent.samurai.speedy.json.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.SpeedyRequestReader;
import com.github.silent.samurai.speedy.interfaces.StructureReader;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.json.parser.JsonQueryParser;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;

import java.io.IOException;

import static com.github.silent.samurai.speedy.utils.CommonUtil.json;

/// JSON {@link SpeedyRequestReader}: opens a streaming {@link JsonStructureReader} over
/// the raw body for entity-tree parsing, and keeps the tree-based {@code $query} parse
/// ({@link JsonQueryParser}) for filter bodies.
public class JsonRequestReader implements SpeedyRequestReader {

    @Override
    public StructureReader readDocument(byte[] rawBody) throws SpeedyHttpException {
        try {
            return new JsonStructureReader(json().getFactory().createParser(rawBody));
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
    }

    @Override
    public SpeedyQuery parseQuery(byte[] rawBody, MetaModel metaModel, SpeedyQuery baseQuery,
                                  int maxPageSize, int defaultPageSize) throws SpeedyHttpException {
        try {
            JsonNode jsonBody = json().readTree(rawBody);
            JsonQueryParser jsonQueryParser = new JsonQueryParser(metaModel, baseQuery.getFrom(), jsonBody);
            jsonQueryParser.setMaxPageSize(maxPageSize);
            jsonQueryParser.setDefaultPageSize(defaultPageSize);
            SpeedyQuery query = jsonQueryParser.build();
            if (query instanceof SpeedyQueryImpl impl) {
                impl.setType(SpeedyRequestType.QUERY);
            }
            return query;
        } catch (IOException e) {
            throw new BadRequestException("Invalid JSON body", e);
        }
    }
}
