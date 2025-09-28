package com.github.silent.samurai.speedy.serializers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.exceptions.InternalServerError;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.IResponseContext;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.io.SelectiveSpeedy2Json;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/// JSON serializer for Speedy API responses with support for dot notation expansions.
///
/// This serializer handles both entity-based expansions (e.g., `["Category"]`) and
/// dot notation expansions (e.g., `["Inventory.Product", "Inventory.Product.Category"]`).
///
/// ## Features
///
/// - **Dot Notation Support**: Handles nested expansions like `Inventory.Product.Category`
/// - **Backward Compatibility**: Supports traditional entity-based expansions
/// - **Field Filtering**: Uses predicate to filter which fields to serialize
/// - **Pagination**: Includes page information in response
public class JSONSerializerV2 implements IResponseSerializerV2 {

    private final Predicate<FieldMetadata> fieldPredicate;
    private final List<? extends SpeedyValue> payload;
    private final Integer pageIndex;
    private final Set<String> expands;

    /// Creates a JSON serializer with the default field predicate (includes all fields).
    ///
    /// @param payload   the list of entities to serialize
    /// @param pageIndex the current page index
    /// @param expands   the set of expansions (supports dot notation like `["Inventory.Product"]`)
    public JSONSerializerV2(List<? extends SpeedyValue> payload, Integer pageIndex, Set<String> expands) {
        this.payload = payload;
        this.pageIndex = pageIndex;
        this.expands = expands;
        this.fieldPredicate = fieldMetadata -> true;
    }

    /// Creates a JSON serializer with custom field predicate for filtering fields.
    ///
    /// @param fieldPredicate predicate to filter which fields to include in serialization
    /// @param payload        the list of entities to serialize
    /// @param pageIndex      the current page index
    /// @param expands        the set of expansions (supports dot notation like `["Inventory.Product"]`)
    public JSONSerializerV2(Predicate<FieldMetadata> fieldPredicate,
                            List<? extends SpeedyValue> payload,
                            Integer pageIndex, Set<String> expands) {
        this.fieldPredicate = fieldPredicate;
        this.payload = payload;
        this.pageIndex = pageIndex;
        this.expands = expands;
    }

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public void write(IResponseContext context) throws SpeedyHttpException {

        SelectiveSpeedy2Json selectiveSpeedy2Json = new SelectiveSpeedy2Json(
                context.getMetaModel(),
                fieldPredicate
        );

        JsonNode jsonElement = selectiveSpeedy2Json.formCollection(
                payload,
                context.getEntityMetadata(),
                expands
        );

        ObjectMapper json = CommonUtil.json();
        ObjectNode basePayload = json.createObjectNode();
        basePayload.set("payload", jsonElement);
        basePayload.put("pageIndex", pageIndex);
        basePayload.put("pageSize", payload.size());
//        basePayload.put("totalPageCount", totalPageCount);

        HttpServletResponse response = context.getResponse();
        response.setContentType(this.getContentType());
        response.setStatus(HttpServletResponse.SC_OK);

        try {
            json.writeValue(response.getWriter(), basePayload);
        } catch (IOException e) {
            throw new InternalServerError("Internal Server Error", e);
        }
    }

}
