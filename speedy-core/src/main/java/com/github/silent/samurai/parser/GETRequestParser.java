package com.github.silent.samurai.parser;

import com.github.silent.samurai.AntlrParser;
import com.github.silent.samurai.Request;
import com.github.silent.samurai.controllers.SpeedyApiController;
import com.github.silent.samurai.exceptions.ResourceNotFoundException;
import com.github.silent.samurai.helpers.EntityMetadataHelper;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.metamodel.RequestInfo;
import com.github.silent.samurai.serializers.ApiAutomateJsonSerializer;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.common.base.Splitter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

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

    private void parseURI(String requestURI, RequestInfo requestInfo) {
        AntlrParser antlrParser = new AntlrParser(requestURI);
        Request request = antlrParser.parse();
        requestInfo.setRequest(request);
    }

    private boolean isPrimaryKeyFilter(RequestInfo requestInfo) {
        requestInfo.setSerializationType(ApiAutomateJsonSerializer.MULTIPLE_ENTITY);
        if (requestInfo.getResourceMetadata() == null) {
            throw new ResourceNotFoundException("Entity Not Found " + requestInfo.getRequest().getResource());
        }
        EntityMetadata resource = requestInfo.getResourceMetadata();
        if (EntityMetadataHelper.instance.isOnlyPrimaryKeyFields(resource, requestInfo.getKeywords().keySet())) {
            requestInfo.setPrimaryKey(true);
            requestInfo.setSerializationType(ApiAutomateJsonSerializer.SINGLE_ENTITY);
            return true;
        }
        return false;
    }

    private void processFilters(RequestInfo requestInfo) {
        List<String> arguments = requestInfo.getRequest().getArguments();
        Map<String, String> keywords = requestInfo.getRequest().getKeywords();
        if (!arguments.isEmpty()) {
            arguments.forEach(arg -> requestInfo.getArguments().add(arg));
        }
        if (!keywords.isEmpty()) {
            keywords.forEach((key, val) -> {
                String strVal = CommonUtil.getGson().fromJson(val, String.class);
                requestInfo.getKeywords().put(key, strVal);
            });
        }
        this.isPrimaryKeyFilter(requestInfo);
    }

    private void processQuery(RequestInfo requestInfo) {
        MultiValueMap<String, String> query = requestInfo.getRequest().getQuery();
        MultiValueMap<String, String> queryParams = requestInfo.getQueryParams();
        if (!query.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : query.entrySet()) {
                String key = entry.getKey();
                List<String> valueList = entry.getValue();
                for (String val : valueList) {
                    String strVal = CommonUtil.getGson().fromJson(val, String.class);
                    requestInfo.getQueryParams().add(key, strVal);
                }
            }
        }
    }

    private void processResource(RequestInfo requestInfo) {
        String resource = requestInfo.getRequest().getResource();
        EntityMetadata entityMetadata = metaModelProcessor.findEntityMetadata(resource);
        requestInfo.setResourceMetadata(entityMetadata);
    }

    public RequestInfo parse(HttpServletRequest request) throws UnsupportedEncodingException {
        String requestURI = URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8.name());
        requestURI = requestURI.replaceAll(SpeedyApiController.URI, "");

        RequestInfo requestInfo = new RequestInfo();
        parseURI(requestURI, requestInfo);

        processResource(requestInfo);
        processFilters(requestInfo);
        processQuery(requestInfo);


        return requestInfo;
    }

}
