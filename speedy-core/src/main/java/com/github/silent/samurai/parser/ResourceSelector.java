package com.github.silent.samurai.parser;

import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.exceptions.SpeedyHttpException;
import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.MetaModelProcessor;
import com.github.silent.samurai.models.conditions.BinaryCondition;
import com.github.silent.samurai.models.conditions.BinarySVCondition;
import com.github.silent.samurai.models.conditions.Condition;
import com.github.silent.samurai.models.conditions.EqCondition;
import com.github.silent.samurai.speedy.models.Filter;
import com.github.silent.samurai.speedy.models.ResourceRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

public class ResourceSelector {

    private final MetaModelProcessor metaModelProcessor;
    private String resource;
    private EntityMetadata resourceMetadata;
    private boolean onlyIdentifiersPresent = false;
    private final MultiValueMap<String, String> keywords = new LinkedMultiValueMap<>();
    private List<String> filterConditionChain;
    private final Map<String, Condition> conditions = new HashMap<>();

    public ResourceSelector(MetaModelProcessor metaModelProcessor) {
        this.metaModelProcessor = metaModelProcessor;
    }

    public boolean hasKeyword(String fieldName) {
        return keywords.containsKey(fieldName);
    }

    public boolean hasInternalId(String conditionId) {
        return conditions.containsKey(conditionId);
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
            List<Object> conditionValues = ConditionFactory.getConditionValue(condition);
            for (Object instance : conditionValues) {
                values.add(type.cast(instance));
            }
        }
        return Collections.unmodifiableList(values);
    }

    public <T> T getFirstFilterValue(String name, Class<T> tClass) throws Exception {
        BinarySVCondition condition = getFirstConditionByField(name);
        Object instance = condition.getInstance();
        return tClass.cast(instance);
    }

    public List<String> getConditionChain() {
        return filterConditionChain;
    }


    public String getResource() {
        return resource;
    }

    public EntityMetadata getResourceMetadata() {
        return resourceMetadata;
    }

    public boolean isOnlyIdentifiersPresent() {
        return onlyIdentifiersPresent;
    }

    public void process(ResourceRequest resourceRequest) throws SpeedyHttpException {
        resource = resourceRequest.getResource();
        filterConditionChain = resourceRequest.getFilterOrder();
        this.resourceMetadata = this.metaModelProcessor.findEntityMetadata(resourceRequest.getResource());
        processFilters(resourceRequest);
        boolean isAllEqualCondition = conditions.values()
                .stream()
                .allMatch(EqCondition.class::isInstance);
        if (MetadataUtil.hasOnlyPrimaryKeyFields(resourceMetadata, keywords.keySet()) && isAllEqualCondition) {
            onlyIdentifiersPresent = true;
        }
    }


    private void processFilters(ResourceRequest resourceRequest) throws SpeedyHttpException {
        if (!resourceRequest.getFilters().isEmpty()) {
            for (Filter filter : resourceRequest.getFilters().values()) {
                BinaryCondition condition = ConditionFactory.createCondition(filter, getResourceMetadata());
                conditions.put(filter.getInternalId(), condition);
                String kwd = filter.getField();
                if (filter.isAssociationPresent()) {
                    kwd += "." + filter.getAssociationId();
                }
                keywords.add(kwd, filter.getInternalId());
            }
        }
    }


}
