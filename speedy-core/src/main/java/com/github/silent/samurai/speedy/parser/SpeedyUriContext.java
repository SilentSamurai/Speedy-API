package com.github.silent.samurai.speedy.parser;

import com.github.silent.samurai.speedy.AntlrParser;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.mappings.String2JavaType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstant;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.models.AntlrRequest;
import com.github.silent.samurai.speedy.models.Filter;
import com.github.silent.samurai.speedy.models.ResourceRequest;
import com.github.silent.samurai.speedy.models.UrlQuery;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;


public class SpeedyUriContext {

    private final MetaModel metaModel;
    private final String requestURI;
    private final MultiValueMap<String, String> rawQuery = new LinkedMultiValueMap<>();
    private String fragment;
    private final MultiValueMap<String, String> keywords = new LinkedMultiValueMap<>();
    private SpeedyQueryImpl speedyQuery;
    private List<String> filterConditionChain;
    private ResourceRequest resourceRequest;


    public SpeedyUriContext(MetaModel metaModel, String requestURI) {
        this.metaModel = metaModel;
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
            T t = String2JavaType.quotedStringToPrimitive(str, type);
            list.add(t);
        }
        return list;
    }

    public <T> T getQueryOrDefault(String name, Class<T> type, T defaultValue) throws BadRequestException {
        if (hasQuery(name)) {
            String queryValue = rawQuery.get(name).get(0);
            return String2JavaType.quotedStringToPrimitive(queryValue, type);
        }
        return defaultValue;
    }

    public SpeedyQuery parse() throws SpeedyHttpException {
        try {
            this.process();
        } catch (Exception e) {
            throw new BadRequestException("Invalid URL", e);
        }
        return speedyQuery;
    }

    private void process() throws Exception {
        String sanitizedURI = requestURI;
        if (requestURI.contains(SpeedyConstant.URI)) {
            int indexOf = requestURI.indexOf(SpeedyConstant.URI);
            sanitizedURI = requestURI.substring(indexOf + SpeedyConstant.URI.length());
        }
        AntlrRequest antlrRequest = new AntlrParser(sanitizedURI).parse();
        this.resourceRequest = antlrRequest.getRequestList().get(0);

        String primaryResource = resourceRequest.getResource();
        EntityMetadata resourceMetadata = this.metaModel.findEntityMetadata(primaryResource);
        this.speedyQuery = new SpeedyQueryImpl(resourceMetadata);

        this.filterConditionChain = resourceRequest.getFilterOrder();

        processFilters(resourceRequest);
        processUrlParams(antlrRequest);
        addToOrderList("orderBy", false);
        addToOrderList("orderByDesc", true);

        if (hasQuery("pageSize")) {
            int pageSize = this.getQueryOrDefault("pageSize", Integer.class, 0);
            speedyQuery.addPageSize(pageSize);
        }

        if (hasQuery("pageNo")) {
            int pageNo = this.getQueryOrDefault("pageNo", Integer.class, 0);
            speedyQuery.addPageNo(pageNo);
        }

//        boolean isAllEqualCondition = speedyQuery.getWhere().getConditions()
//                .stream()
//                .allMatch(EqCondition.class::isInstance);
//        if (MetadataUtil.hasOnlyPrimaryKeyFields(resourceMetadata, keywords.keySet()) && isAllEqualCondition) {
//            onlyIdentifiersPresent = true;
//        }

    }

    private void processUrlParams(AntlrRequest antlrRequest) {
        if (!antlrRequest.getQueries().isEmpty()) {
            for (Map.Entry<String, List<UrlQuery>> entry : antlrRequest.getQueries().entrySet()) {
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

    private void addToOrderList(String queryName,
                                boolean isDesc) throws Exception {
        if (this.hasQuery(queryName)) {
            Map<Boolean, List<String>> collect = this.getQuery(queryName, String.class)
                    .stream()
                    .collect(Collectors.partitioningBy(qry -> qry.contains(",")));
            List<String> withComma = collect.get(true);
            List<String> withoutComma = collect.get(false);
            withComma.stream()
                    .flatMap(qry -> Arrays.stream(qry.split(",")))
                    .forEach(withoutComma::add);
            for (String fieldName : withoutComma) {
                if (isDesc) {
                    speedyQuery.orderByDesc(fieldName);
                } else {
                    speedyQuery.orderByAsc(fieldName);
                }
            }
        }
    }

    private void processFilters(ResourceRequest resourceRequest) throws SpeedyHttpException {
        if (!resourceRequest.getFilters().isEmpty()) {
            ConditionFactory conditionFactory = speedyQuery.getConditionFactory();
            for (Filter filter : resourceRequest.getFilters().values()) {
                if (filter.isMultiple()) {
                    BinaryCondition binaryCondition = conditionFactory.createBinaryConditionQuotedString(filter.getField(), filter.getOperator(), filter.getValues());
                    speedyQuery.getWhere().addSubCondition(binaryCondition);
                } else {
                    String value = filter.getValues().get(0);
                    if (filter.isAssociationPresent()) {
                        BinaryCondition binaryCondition = conditionFactory.createAssociatedConditionQuotedString(filter.getField(), filter.getAssociationId(), filter.getOperator(), value);
                        speedyQuery.getWhere().addSubCondition(binaryCondition);
                    } else {
                        BinaryCondition binaryCondition = conditionFactory.createBinaryConditionQuotedString(filter.getField(), filter.getOperator(), value);
                        speedyQuery.getWhere().addSubCondition(binaryCondition);
                    }
                }
                // internal
                String kwd = filter.getField();
                if (filter.isAssociationPresent()) {
                    kwd += "." + filter.getAssociationId();
                }
                keywords.add(kwd, filter.getInternalId());
            }
        }
    }

}
