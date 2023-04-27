package com.github.silent.samurai.parser;

import com.github.silent.samurai.AntlrParser;
import com.github.silent.samurai.AntlrRequest;
import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.FieldMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.interfaces.SpeedyConstant;
import com.github.silent.samurai.utils.CommonUtil;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class SpeedyUriParser {

    private final MetaModelProcessor metaModelProcessor;
    private final String requestURI;
    private final Map<String, String> rawKeywords = new HashMap<>();
    private final MultiValueMap<String, String> rawQuery = new LinkedMultiValueMap<>();
    private String resource;
    private String secondaryResource;
    private EntityMetadata resourceMetadata;
    private String fragment;
    private boolean onlyIdentifiersPresent = false;

    public SpeedyUriParser(MetaModelProcessor metaModelProcessor, String requestURI) {
        this.metaModelProcessor = metaModelProcessor;
        this.requestURI = requestURI;
    }

    public boolean hasKeyword(String name) {
        return rawKeywords.containsKey(name);
    }

    public boolean hasQuery(String name) {
        return rawQuery.containsKey(name);
    }

    public Set<String> getKeywords() {
        return rawKeywords.keySet();
    }

    public Set<String> getQueries() {
        return rawQuery.keySet();
    }

    public Object getKeyword(String name) throws NotFoundException {
        String value = rawKeywords.get(name);
        FieldMetadata field = resourceMetadata.field(name);
        return CommonUtil.quotedStringToPrimitive(value, field.getFieldType());
    }

    public Object getKeyword(String name, Class<?> type) throws NotFoundException {
        if (!hasKeyword(name)) {
            throw new NotFoundException("keyword not found");
        }
        String value = rawKeywords.get(name);
        return CommonUtil.quotedStringToPrimitive(value, type);
    }

    public List<Object> getQuery(String name, Class<?> type) throws NotFoundException {
        if (!hasQuery(name)) {
            throw new NotFoundException("query not found");
        }
        List<String> values = rawQuery.get(name);
        return values.stream()
                .map(str -> CommonUtil.quotedStringToPrimitive(str, type))
                .collect(Collectors.toList());
    }

    public Object getQueryOrDefault(String name, Class<?> type, Object defaultValue) {
        if (hasQuery(name)) {
            String queryValue = rawQuery.get(name).get(0);
            return CommonUtil.quotedStringToPrimitive(queryValue, type);
        }
        return defaultValue;
    }

    public void parse() throws Exception {
        String sanitizedURI = requestURI;
        if (requestURI.contains(SpeedyConstant.URI)) {
            int indexOf = requestURI.indexOf(SpeedyConstant.URI);
            sanitizedURI = requestURI.substring(indexOf + SpeedyConstant.URI.length());
        }
        AntlrRequest antlrRequest = new AntlrParser(sanitizedURI).parse();
        resource = antlrRequest.getResource();
        this.resourceMetadata = this.metaModelProcessor.findEntityMetadata(antlrRequest.getResource());
        processFilters(antlrRequest);
        processQuery(antlrRequest);
        if (MetadataUtil.hasOnlyPrimaryKeyFields(resourceMetadata, rawKeywords.keySet())) {
            onlyIdentifiersPresent = true;
        }
    }

    private void processFilters(AntlrRequest antlrRequest) {
        if (!antlrRequest.getArguments().isEmpty()) {
            rawKeywords.put("id", antlrRequest.getArguments().get(0));
        }
        if (!antlrRequest.getKeywords().isEmpty()) {
            rawKeywords.putAll(antlrRequest.getKeywords());
        }
    }

    private void processQuery(AntlrRequest antlrRequest) {
        if (!antlrRequest.getQuery().isEmpty()) {
            for (Map.Entry<String, List<String>> entry : antlrRequest.getQuery().entrySet()) {
                String key = entry.getKey();
                List<String> valueList = entry.getValue();
                valueList.forEach(val -> rawQuery.add(key, val));
            }
        }
    }

    public String getResource() {
        return resource;
    }

    public String getSecondaryResource() {
        return secondaryResource;
    }

    public EntityMetadata getResourceMetadata() {
        return resourceMetadata;
    }

    public boolean isOnlyIdentifiersPresent() {
        return onlyIdentifiersPresent;
    }
}
