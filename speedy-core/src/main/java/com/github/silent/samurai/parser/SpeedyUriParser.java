package com.github.silent.samurai.parser;

import com.github.silent.samurai.AntlrParser;
import com.github.silent.samurai.exceptions.BadRequestException;
import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.interfaces.SpeedyConstant;
import com.github.silent.samurai.models.Operator;
import com.github.silent.samurai.models.conditions.BinarySVCondition;
import com.github.silent.samurai.models.conditions.Condition;
import com.github.silent.samurai.models.conditions.ConditionFactory;
import com.github.silent.samurai.models.conditions.EqCondition;
import com.github.silent.samurai.speedy.model.AntlrRequest;
import com.github.silent.samurai.speedy.model.Filter;
import com.github.silent.samurai.speedy.model.FilterValue;
import com.github.silent.samurai.utils.CommonUtil;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class SpeedyUriParser {

    private final MetaModelProcessor metaModelProcessor;
    private final String requestURI;
    private final MultiValueMap<String, Condition> conditions = new LinkedMultiValueMap<>();
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
        return conditions.containsKey(name);
    }

    public boolean hasQuery(String name) {
        return rawQuery.containsKey(name);
    }

    public <T extends Condition> T getCondition(String name) throws NotFoundException {
        return (T) this.conditions.get(name)
                .stream().findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public <T> T getConditionValue(String name, Class<T> tClass) throws NotFoundException {
        BinarySVCondition condition = getCondition(name);
        String value = condition.getValue();
        return CommonUtil.quotedStringToPrimitive(value, tClass);
    }

    public List<Condition> getAllConditions() {
        return conditions.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    public Set<String> getQueries() {
        return rawQuery.keySet();
    }

    public <T> List<T> getQuery(String name, Class<T> type) throws NotFoundException {
        if (!hasQuery(name)) {
            throw new NotFoundException("query not found");
        }
        List<String> values = rawQuery.get(name);
        return values.stream()
                .map(str -> CommonUtil.quotedStringToPrimitive(str, type))
                .collect(Collectors.toList());
    }

    public <T> T getQueryOrDefault(String name, Class<T> type, T defaultValue) {
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
        boolean isAllEqualCondition = getAllConditions().stream().allMatch(EqCondition.class::isInstance);
        if (MetadataUtil.hasOnlyPrimaryKeyFields(resourceMetadata, conditions.keySet()) && isAllEqualCondition) {
            onlyIdentifiersPresent = true;
        }
    }

    private void processFilters(AntlrRequest antlrRequest) throws BadRequestException {
        if (!antlrRequest.getArguments().isEmpty()) {
            String value = antlrRequest.getArguments().get(0);
            Condition idCondition = ConditionFactory.createCondition("id", Operator.EQ, value);
            conditions.add("id", idCondition);
        }
        if (!antlrRequest.getKeywords().isEmpty()) {
            for (Filter filter : antlrRequest.getKeywords().values()) {
                Condition condition = ConditionFactory.createCondition(filter);
                conditions.add(filter.getIdentifier(), condition);
            }
        }
    }

    private void processQuery(AntlrRequest antlrRequest) {
        if (!antlrRequest.getQuery().isEmpty()) {
            for (Map.Entry<String, List<FilterValue>> entry : antlrRequest.getQuery().entrySet()) {
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
