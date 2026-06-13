package com.github.silent.samurai.speedy.parser;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.*;
import com.github.silent.samurai.speedy.conversion.registry.JavaTypeRegistry;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import com.github.silent.samurai.speedy.utils.Speedy;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Builder
public class SpeedyUriContext {
    private final MetaModel metaModel;
    private final String requestURI;
    /// The Java-type registry used to parse string literals from URL query parameters
    /// into typed SpeedyValue instances via {@link #buildExpression}.
    ///
    /// @see JavaTypeRegistry#parseString(String, Class)
    private final JavaTypeRegistry javaTypeRegistry;
    private final MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
    @Builder.Default
    private final int maxPageSize = Integer.MAX_VALUE;
    @Builder.Default
    private final int defaultPageSize = 20;
    @Builder.Default
    private final int maxQueryStringLength = Integer.MAX_VALUE;
    @Builder.Default
    private final int maxFilterCount = Integer.MAX_VALUE;
    private SpeedyQueryImpl speedyQuery;
    private String actionSuffix;

    Expression buildExpression(FieldMetadata metadata, String symbol) throws SpeedyHttpException {
        if (symbol.startsWith("$")) {
            String field = symbol.substring(1);
            QueryField queryField = this.speedyQuery.getConditionFactory().createQueryField(field);
            this.speedyQuery.getConditionFactory().validateQueryFieldNotSensitive(queryField);
            return new Identifier(queryField);
        } else {
            String literal = symbol.replaceAll("['\" ]", "");
            Object parsed = javaTypeRegistry.parseString(literal, metadata.getValueType().javaTypeClass());
            return new Literal(javaTypeRegistry.toSpeedy(parsed, metadata.getValueType()));
        }
    }

    public SpeedyQuery parse() throws SpeedyHttpException {
        try {
            return this.process();
        } catch (BadRequestException e) {
            throw e;
        } catch (NotFoundException e) {
            throw new BadRequestException(e.getMessage(), e);
        } catch (Exception e) {
            throw new BadRequestException("Invalid URL", e);
        }
    }

    public SpeedyQuery process() throws Exception {

        String sanitizedURI = URLDecoder.decode(requestURI, StandardCharsets.UTF_8);
        if (sanitizedURI.length() > maxQueryStringLength) {
            throw new BadRequestException(
                    "Query string length " + sanitizedURI.length() + " exceeds maximum " + maxQueryStringLength);
        }
        if (sanitizedURI.contains(SpeedyConstant.URI)) {
            int indexOf = sanitizedURI.indexOf(SpeedyConstant.URI);
            sanitizedURI = sanitizedURI.substring(indexOf + SpeedyConstant.URI.length());
        }

        UriComponents uriComponents = UriComponentsBuilder.fromUriString(sanitizedURI)
                .build();

        EntityMetadata entityMetadata = this.extractEntity(uriComponents);
        this.actionSuffix = this.extractActionSuffix(uriComponents);

        this.speedyQuery = new SpeedyQueryImpl(entityMetadata);
        this.speedyQuery.setMaxPageSize(maxPageSize);
        this.speedyQuery.addPageSize(Math.min(defaultPageSize, maxPageSize));

        captureUrlParams(uriComponents);

        processFilters();

        addToOrderList("$orderBy", false);
        addToOrderList("$orderByDesc", true);

        capturePageInfo(uriComponents);
        captureSelectParams(uriComponents);

        return this.speedyQuery;
    }

    private void capturePageInfo(UriComponents uriComponents) throws SpeedyHttpException {
        if (uriComponents.getQueryParams().containsKey("$pageSize")) {
            String $pageSize = uriComponents.getQueryParams().getFirst("$pageSize");
            try {
                Integer pageSize = javaTypeRegistry.parseString($pageSize.replaceAll("['\" ]", ""), Integer.class);
                Objects.requireNonNull(pageSize);
                if (pageSize > maxPageSize) {
                    throw new BadRequestException(
                            "Requested page size " + pageSize + " exceeds maximum allowed page size " + maxPageSize
                    );
                }
                speedyQuery.addPageSize(pageSize);
            } catch (BadRequestException e) {
                throw e;
            } catch (NumberFormatException e) {
                throw new BadRequestException("Invalid value for $pageSize: '" + $pageSize + "'. Must be an integer.", e);
            }
        }

        if (uriComponents.getQueryParams().containsKey("$pageNo")) {
            String $pageNo = uriComponents.getQueryParams().getFirst("$pageNo");
            try {
                Integer pageNo = javaTypeRegistry.parseString($pageNo.replaceAll("['\" ]", ""), Integer.class);
                Objects.requireNonNull(pageNo);
                speedyQuery.addPageNo(pageNo);
            } catch (NumberFormatException e) {
                throw new BadRequestException("Invalid value for $pageNo: '" + $pageNo + "'. Must be an integer.", e);
            }
        }

        if (uriComponents.getQueryParams().containsKey("$format")) {
            String $format = uriComponents.getQueryParams().getFirst("$format");
            if ($format != null) {
                speedyQuery.addFormat(javaTypeRegistry.parseString($format.replaceAll("['\" ]", ""), String.class));
            }
        }

        if (uriComponents.getQueryParams().containsKey("$expand")) {
            String $expand = uriComponents.getQueryParams().getFirst("$expand");
            if ($expand != null) {
                String[] expands = $expand.replaceAll("['\" ]", "").split(",");
                for (String exp : expands) {
                    if (!exp.isEmpty()) {
                        speedyQuery.addExpand(exp);
                    }
                }
            }
        }
    }

    private void captureSelectParams(UriComponents uriComponents) throws BadRequestException {
        if (uriComponents.getQueryParams().containsKey("$select")) {
            String $select = uriComponents.getQueryParams().getFirst("$select");
            if ($select != null) {
                String[] selects = $select.replaceAll("['\" ]", "").split(",");
                for (String sel : selects) {
                    if (!sel.isEmpty()) {
                        if ("$count".equals(sel)) {
                            speedyQuery.setCountRequest(true);
                        } else {
                            speedyQuery.addSelect(sel);
                        }
                    }
                }
            }
            if (speedyQuery.isCountRequest() && !speedyQuery.getSelect().isEmpty()) {
                throw new BadRequestException(
                        "$select cannot mix '$count' with field names. Use '$count' alone to request a count.");
            }
        }
    }

    private EntityMetadata extractEntity(UriComponents uriComponents) throws BadRequestException, NotFoundException {
        List<String> pathSegments = uriComponents.getPathSegments();
        if (!pathSegments.isEmpty()) {
            String resourceName = pathSegments.get(0);
            return metaModel.findEntityMetadata(resourceName);
        } else {
            throw new BadRequestException("Invalid URL: Missing resource name in path");
        }
    }

    private String extractActionSuffix(UriComponents uriComponents) {
        List<String> pathSegments = uriComponents.getPathSegments();
        return pathSegments.isEmpty() ? "" : pathSegments.get(pathSegments.size() - 1);
    }

    public String getActionSuffix() {
        return actionSuffix;
    }

    public SpeedyQueryImpl getParsedQuery() {
        return speedyQuery;
    }

    private void captureUrlParams(UriComponents uriComponents) throws SpeedyHttpException {
        if (!uriComponents.getQueryParams().isEmpty()) {
            MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
            int filterCount = 0;
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                String key = entry.getKey().strip().trim();
                if (!key.startsWith("$")) {
                    filterCount++;
                    if (filterCount > maxFilterCount) {
                        throw new BadRequestException(
                                "Filter count exceeds maximum " + maxFilterCount);
                    }
                    try {
                        speedyQuery.getConditionFactory().createQueryField(key);
                    } catch (NotFoundException e) {
                        throw new BadRequestException("Unknown query field: '" + key + "' on entity '" + speedyQuery.getFrom().getName() + "'", e);
                    }
                }
                List<String> valueList = entry.getValue()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(Predicate.not(String::isBlank))
                        .map(String::trim)
                        .map(String::strip)
                        .collect(Collectors.toList());
                if (valueList.isEmpty()) {
                    valueList.add("true");
                }
                queryParameters.put(key, valueList);
            }
        }
    }

    private void processFilters() throws SpeedyHttpException {
        if (!queryParameters.isEmpty()) {
            ConditionFactory conditionFactory = speedyQuery.getConditionFactory();
            for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
                String field = entry.getKey();
                if (field.startsWith("$")) continue;

                QueryField queryField = conditionFactory.createQueryField(field);
                List<String> valueList = entry.getValue();

                if (valueList.isEmpty()) {
                    SpeedyValue speedyValue = Speedy.from(true);
                    BinaryCondition binaryCondition = conditionFactory.createBiCondition(queryField, ConditionOperator.EQ, new Literal(speedyValue));
                    speedyQuery.getWhere().addSubCondition(binaryCondition);
                } else if (valueList.size() == 1) {
                    Expression expression = buildExpression(queryField.getMetadataForParsing(), valueList.get(0));
                    BinaryCondition binaryCondition = conditionFactory.createBiCondition(queryField, ConditionOperator.EQ, expression);
                    speedyQuery.getWhere().addSubCondition(binaryCondition);

                } else {

                    List<SpeedyValue> speedyValueList = new LinkedList<>();
                    for (String value : valueList) {
                        Object parsed = javaTypeRegistry.parseString(value.replaceAll("['\" ]", ""), queryField.getMetadataForParsing().getValueType().javaTypeClass());
                        speedyValueList.add(javaTypeRegistry.toSpeedy(parsed, queryField.getMetadataForParsing().getValueType()));
                    }
                    SpeedyCollection fieldValue = new SpeedyCollection(speedyValueList);
                    BinaryCondition binaryCondition = conditionFactory.createBiCondition(queryField, ConditionOperator.EQ, new Literal(fieldValue));
                    speedyQuery.getWhere().addSubCondition(binaryCondition);

                }
            }
        }
    }

    private void addToOrderList(String queryName, boolean isDesc) throws Exception {
        MultiValueMap<String, String> queryParams = queryParameters;
        if (queryParams.containsKey(queryName)) {
            List<String> values = queryParams.remove(queryName);
            List<String> fields = values.stream()
                    .map(item -> {
                        try {
                            return javaTypeRegistry.parseString(item.replaceAll("['\" ]", ""), String.class);
                        } catch (SpeedyHttpException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .flatMap(qry -> Arrays.stream(qry.split(",")))
                    .toList();
            for (String field : fields) {
                if (isDesc) {
                    speedyQuery.orderByDesc(field);
                } else {
                    speedyQuery.orderByAsc(field);
                }
            }
        }
    }

}
