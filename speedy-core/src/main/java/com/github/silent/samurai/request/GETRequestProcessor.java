package com.github.silent.samurai.request;

import com.github.silent.samurai.controllers.SpeedyApiController;
import com.github.silent.samurai.exceptions.ResourceNotFoundException;
import com.github.silent.samurai.helpers.EntityMetadataHelper;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.metamodel.RequestInfo;
import com.github.silent.samurai.serializers.ApiAutomateJsonSerializer;
import com.github.silent.samurai.utils.CommonUtil;
import com.google.common.base.Splitter;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GETRequestProcessor {

    private final MetaModelProcessor metaModelProcessor;

    public GETRequestProcessor(MetaModelProcessor metaModelProcessor) {
        this.metaModelProcessor = metaModelProcessor;
    }

    public boolean checkIfPrimaryKeyFilters(RequestInfo requestInfo) {
        boolean isAllAttrPresent = false;
        EntityMetadata entityMetadata = metaModelProcessor.findEntityMetadata(requestInfo.resourceType);
        if (entityMetadata == null) {
            throw new ResourceNotFoundException("Entity Not Found " + requestInfo.resourceType);
        }
        if (EntityMetadataHelper.instance.isOnlyPrimaryKeyFields(entityMetadata, requestInfo.filters.keySet())) {
            requestInfo.primaryKey = true;
            requestInfo.serializationType = ApiAutomateJsonSerializer.SINGLE_ENTITY;
            return true;
        }
        return false;
    }

    public Map<String, String> parseParams(String pkString) {
        Map<String, String> pkMap = new HashMap<>();
        if (pkString.contains("=")) {
            Map<String, String> paramMap = Splitter.on(",")
                    .trimResults()
                    .withKeyValueSeparator("=")
                    .split(pkString);
            for (Map.Entry<String, String> param : paramMap.entrySet()) {
                String val = param.getValue().replaceAll("/\"'/g", "");
                pkMap.put(param.getKey(), val);
            }
        } else {
            pkMap.put("id", pkString.replaceAll("/\"'/g", ""));
        }
        return pkMap;
    }

    public void processResourceAndFilters(String resource, RequestInfo requestInfo) {
        String filterParams = CommonUtil.findRegexGroup("[A-Za-z0-9_-]+\\((.*)\\)", resource, 1);
        requestInfo.resourceType = resource;
        requestInfo.serializationType = ApiAutomateJsonSerializer.MULTIPLE_ENTITY;
        if (filterParams != null) {
            if (!filterParams.contains("=")) {
                requestInfo.serializationType = ApiAutomateJsonSerializer.SINGLE_ENTITY;
                requestInfo.primaryKey = true;
            }
            requestInfo.filters = parseParams(filterParams);
            requestInfo.resourceType = resource.substring(0, resource.indexOf("("));
            this.checkIfPrimaryKeyFilters(requestInfo);
        }
    }

    public RequestInfo process(HttpServletRequest request) throws UnsupportedEncodingException {
        String requestURI = URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8.name());
        requestURI = requestURI.replaceAll(SpeedyApiController.URI + "/", "");
        RequestInfo requestInfo = new RequestInfo();
        List<String> firstSplit = Splitter.on("?").trimResults().omitEmptyStrings().splitToList(requestURI);
        if (!firstSplit.isEmpty()) {
            List<String> resourceSplits = Splitter.on("/").trimResults().omitEmptyStrings().splitToList(firstSplit.get(0));
            if (!resourceSplits.isEmpty()) {
                processResourceAndFilters(resourceSplits.get(0), requestInfo);
            }
            if (resourceSplits.size() > 1) {
                requestInfo.secondaryResourceType = resourceSplits.get(1);
            }
        }
        requestInfo.queryParams = UriComponentsBuilder.fromUriString(request.getRequestURI()).build().getQueryParams();
        return requestInfo;
    }

}
