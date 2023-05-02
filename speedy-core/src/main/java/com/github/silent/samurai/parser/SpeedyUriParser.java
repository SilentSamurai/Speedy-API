package com.github.silent.samurai.parser;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.exceptions.SpeedyHttpException;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.AntlrParser;
import com.github.silent.samurai.speedy.models.AntlrRequest;
import com.github.silent.samurai.speedy.models.Query;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class SpeedyUriParser {

    private final MetaModelProcessor metaModelProcessor;
    private final String requestURI;
    private final MultiValueMap<String, String> rawQuery = new LinkedMultiValueMap<>();
    private String fragment;
    private ResourceSelector primaryResource;
    private ResourceSelector secondaryResource;

    public ResourceSelector getPrimaryResource() {
        return primaryResource;
    }

    public ResourceSelector getSecondaryResource() {
        return secondaryResource;
    }

    public boolean isAssociationFilterRequired() {
        return secondaryResource != null;
    }

    public SpeedyUriParser(MetaModelProcessor metaModelProcessor, String requestURI) {
        this.metaModelProcessor = metaModelProcessor;
        this.requestURI = requestURI;
    }

    public boolean hasQuery(String queryName) {
        return rawQuery.containsKey(queryName);
    }

    public Set<String> getQueryIdentifiers() {
        return rawQuery.keySet();
    }

    public <T> List<T> getQuery(String name, Class<T> type) throws Exception {
        if (!hasQuery(name)) {
            throw new NotFoundException("query not found");
        }
        List<String> values = rawQuery.get(name);
        List<T> list = new ArrayList<>();
        for (String str : values) {
            T t = CommonUtil.quotedStringToPrimitive(str, type);
            list.add(t);
        }
        return list;
    }

    public <T> T getQueryOrDefault(String name, Class<T> type, T defaultValue) throws BadRequestException {
        if (hasQuery(name)) {
            String queryValue = rawQuery.get(name).get(0);
            return CommonUtil.quotedStringToPrimitive(queryValue, type);
        }
        return defaultValue;
    }

    public void parse() throws SpeedyHttpException {
        try {
            this.process();
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    private void process() throws Exception {
        String sanitizedURI = requestURI;
        if (requestURI.contains(SpeedyConstant.URI)) {
            int indexOf = requestURI.indexOf(SpeedyConstant.URI);
            sanitizedURI = requestURI.substring(indexOf + SpeedyConstant.URI.length());
        }
        AntlrRequest antlrRequest = new AntlrParser(sanitizedURI).parse();
        this.primaryResource = new ResourceSelector(this.metaModelProcessor);
        this.primaryResource.process(antlrRequest.getRequestList().get(0));
        if (antlrRequest.getRequestList().size() > 1) {
            this.secondaryResource = new ResourceSelector(this.metaModelProcessor);
            this.secondaryResource.process(antlrRequest.getRequestList().get(1));
        }
        processQuery(antlrRequest);
    }

    private void processQuery(AntlrRequest antlrRequest) {
        if (!antlrRequest.getQueries().isEmpty()) {
            for (Map.Entry<String, List<Query>> entry : antlrRequest.getQueries().entrySet()) {
                String key = entry.getKey();
                List<String> valueList = entry.getValue().stream()
                        .flatMap(fv -> fv.getValues().stream())
                        .collect(Collectors.toList());
                if (valueList.isEmpty()) {
                    valueList.add("true");
                }
                valueList.forEach(val -> rawQuery.add(key, val));
            }
        }
    }
}
