package com.github.silent.samurai.request.get;

import com.github.silent.samurai.AntlrParser;
import com.github.silent.samurai.Request;
import com.github.silent.samurai.controllers.SpeedyApiController;
import com.github.silent.samurai.exceptions.ResourceNotFoundException;
import com.github.silent.samurai.helpers.EntityMetadataHelper;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.serializers.ApiAutomateJsonSerializer;
import com.github.silent.samurai.utils.CommonUtil;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class GETRequestParser {

    private final MetaModelProcessor metaModelProcessor;

    public GETRequestParser(MetaModelProcessor metaModelProcessor) {
        this.metaModelProcessor = metaModelProcessor;
    }

    private void parseURI(String requestURI, GETRequestContext GETRequestContext) {
        AntlrParser antlrParser = new AntlrParser(requestURI);
        Request request = antlrParser.parse();
        GETRequestContext.setRequest(request);
    }

    private boolean updateSerializationType(GETRequestContext GETRequestContext) {
        GETRequestContext.setSerializationType(ApiAutomateJsonSerializer.MULTIPLE_ENTITY);
        if (GETRequestContext.getResourceMetadata() == null) {
            throw new ResourceNotFoundException("Entity Not Found " + GETRequestContext.getRequest().getResource());
        }
        EntityMetadata resource = GETRequestContext.getResourceMetadata();
        if (EntityMetadataHelper.instance.hasOnlyPrimaryKeyFields(resource, GETRequestContext.getKeywords().keySet())) {
            GETRequestContext.setPrimaryKey(true);
            GETRequestContext.setSerializationType(ApiAutomateJsonSerializer.SINGLE_ENTITY);
            return true;
        }
        return false;
    }

    private void processFilters(GETRequestContext GETRequestContext) {
        List<String> arguments = GETRequestContext.getRequest().getArguments();
        Map<String, String> keywords = GETRequestContext.getRequest().getKeywords();
        if (!arguments.isEmpty()) {
            arguments.forEach(arg -> GETRequestContext.getArguments().add(arg));
            keywords.put("id", arguments.get(0));
        }
        if (!keywords.isEmpty()) {
            keywords.forEach((key, val) -> {
                String strVal = CommonUtil.getGson().fromJson(val, String.class);
                GETRequestContext.getKeywords().put(key, strVal);
            });
        }
        this.updateSerializationType(GETRequestContext);
    }

    private void processQuery(GETRequestContext GETRequestContext) {
        MultiValueMap<String, String> query = GETRequestContext.getRequest().getQuery();
        if (!query.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : query.entrySet()) {
                String key = entry.getKey();
                List<String> valueList = entry.getValue();
                for (String val : valueList) {
                    String strVal = CommonUtil.getGson().fromJson(val, String.class);
                    GETRequestContext.getQueryParams().add(key, strVal);
                }
            }
        }
    }

    private void processResource(GETRequestContext GETRequestContext) {
        String resource = GETRequestContext.getRequest().getResource();
        EntityMetadata entityMetadata = metaModelProcessor.findEntityMetadata(resource);
        GETRequestContext.setResourceMetadata(entityMetadata);
    }

    public GETRequestContext process(HttpServletRequest request) throws UnsupportedEncodingException {
        String requestURI = URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8.name());
        requestURI = requestURI.replaceAll(SpeedyApiController.URI, "");

        GETRequestContext GETRequestContext = new GETRequestContext();
        parseURI(requestURI, GETRequestContext);

        processResource(GETRequestContext);
        processFilters(GETRequestContext);
        processQuery(GETRequestContext);


        return GETRequestContext;
    }

}
