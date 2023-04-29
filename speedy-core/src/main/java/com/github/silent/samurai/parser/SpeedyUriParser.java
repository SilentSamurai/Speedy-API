package com.github.silent.samurai.parser;

import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.exceptions.SpeedyHttpException;
import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.interfaces.SpeedyConstant;
import com.github.silent.samurai.models.Operator;
import com.github.silent.samurai.models.conditions.*;
import com.github.silent.samurai.speedy.AntlrParser;
import com.github.silent.samurai.speedy.models.AntlrRequest;
import com.github.silent.samurai.speedy.models.Filter;
import com.github.silent.samurai.speedy.models.Query;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;


public class SpeedyUriParser {

    private final MetaModelProcessor metaModelProcessor;
    private final String requestURI;
    private final Map<String, Condition> conditions = new HashMap<>();
    private final MultiValueMap<String, String> rawQuery = new LinkedMultiValueMap<>();
    private String resource;
    private String secondaryResource;
    private EntityMetadata resourceMetadata;
    private String fragment;
    private boolean onlyIdentifiersPresent = false;
    private final MultiValueMap<String, String> keywords = new LinkedMultiValueMap<>();
    private List<String> filterConditionChain;

    public SpeedyUriParser(MetaModelProcessor metaModelProcessor, String requestURI) {
        this.metaModelProcessor = metaModelProcessor;
        this.requestURI = requestURI;
    }

    public boolean hasKeyword(String fieldName) {
        return keywords.containsKey(fieldName);
    }

    public boolean hasQuery(String queryName) {
        return rawQuery.containsKey(queryName);
    }

    public boolean hasInternalId(String internalId) {
        return conditions.containsKey(internalId);
    }

    public <T extends Condition> T getConditionByInternalId(String internalId) throws NotFoundException {
        if (!hasInternalId(internalId)) {
            throw new NotFoundException("Internal Id not found");
        }
        return (T) this.conditions.get(internalId);
    }

    public List<? extends Condition> getConditionsByField(String fieldName) throws SpeedyHttpException {
        if (!hasKeyword(fieldName)) {
            throw new NotFoundException("keyword not found");
        }
        List<String> internalIds = this.keywords.get(fieldName);
        return internalIds.stream().map(conditions::get).collect(Collectors.toUnmodifiableList());
    }

    public <T extends Condition> T getFirstConditionByField(String fieldName) throws SpeedyHttpException {
        if (!hasKeyword(fieldName)) {
            throw new NotFoundException("keyword not found");
        }
        List<String> internalIds = this.keywords.get(fieldName);
        if (internalIds.isEmpty()) throw new NotFoundException("keyword not found");
        return (T) this.conditions.get(internalIds.get(0));
    }

    public <T> List<T> getFilterValuesByField(String fieldName, Class<T> type) throws SpeedyHttpException {
        if (!hasKeyword(fieldName)) {
            throw new NotFoundException("keyword not found");
        }
        List<String> internalIds = this.keywords.get(fieldName);
        List<T> values = new LinkedList<>();
        for (String internalId : internalIds) {
            BinaryCondition condition = (BinaryCondition) conditions.get(internalId);
            List<String> conditionValues = ConditionFactory.getConditionValue(condition);
            for (String valueString : conditionValues) {
                T value = CommonUtil.quotedStringToPrimitive(valueString, type);
                values.add(value);
            }
        }
        return Collections.unmodifiableList(values);
    }

    public <T> T getFirstFilterValue(String name, Class<T> tClass) throws Exception {
        BinarySVCondition condition = getFirstConditionByField(name);
        String value = condition.getValue();
        return CommonUtil.quotedStringToPrimitive(value, tClass);
    }

    public List<String> getConditionChain() {
        return filterConditionChain;
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
        resource = antlrRequest.getResource();
        filterConditionChain = antlrRequest.getFilterOrder();
        this.resourceMetadata = this.metaModelProcessor.findEntityMetadata(antlrRequest.getResource());
        processFilters(antlrRequest);
        processQuery(antlrRequest);
        boolean isAllEqualCondition = conditions.values()
                .stream()
                .allMatch(EqCondition.class::isInstance);
        if (MetadataUtil.hasOnlyPrimaryKeyFields(resourceMetadata, keywords.keySet()) && isAllEqualCondition) {
            onlyIdentifiersPresent = true;
        }
    }

    private void processFilters(AntlrRequest antlrRequest) throws BadRequestException {
        if (!antlrRequest.getArguments().isEmpty()) {
            String value = antlrRequest.getArguments().get(0);
            Condition idCondition = ConditionFactory.createCondition("id", Operator.EQ, value);
            conditions.put("id", idCondition);
            filterConditionChain.add("id");
            keywords.add("id", "id");
        }
        if (!antlrRequest.getFilters().isEmpty()) {
            for (Filter filter : antlrRequest.getFilters().values()) {
                BinaryCondition condition = ConditionFactory.createCondition(filter);
                conditions.put(filter.getInternalId(), condition);
                keywords.add(filter.getField(), filter.getInternalId());
            }
        }
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
