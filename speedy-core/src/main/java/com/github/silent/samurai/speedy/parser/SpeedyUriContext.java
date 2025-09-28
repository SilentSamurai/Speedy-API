package com.github.silent.samurai.speedy.parser;

import com.github.silent.samurai.speedy.enums.ConditionOperator;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.exceptions.NotFoundException;
import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.*;
import com.github.silent.samurai.speedy.interfaces.query.*;
import com.github.silent.samurai.speedy.mappings.TypeConverterRegistry;
import com.github.silent.samurai.speedy.models.SpeedyCollection;
import com.github.silent.samurai.speedy.models.SpeedyQueryImpl;
import com.github.silent.samurai.speedy.utils.Speedy;
import com.github.silent.samurai.speedy.utils.SpeedyValueFactory;
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
public class SpeedyUriContext {
    private final MetaModel metaModel;
    private final String requestURI;
    private final MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>();
    private SpeedyQueryImpl speedyQuery;

    public SpeedyUriContext(MetaModel metaModel, String requestURI) {
        this.metaModel = metaModel;
        this.requestURI = requestURI;
    }

    Expression buildExpression(FieldMetadata metadata, String symbol) throws SpeedyHttpException {
        if (symbol.startsWith("$")) {
            String field = symbol.substring(1);
            QueryField queryField = this.speedyQuery.getConditionFactory().createQueryField(field);
            return new Identifier(queryField);
        } else {
            return new Literal(SpeedyValueFactory.basicFromString(metadata, symbol));
        }
    }

    public SpeedyQuery parse() throws SpeedyHttpException {
        try {
            return this.process();
        } catch (Exception e) {
            throw new BadRequestException("Invalid URL", e);
        }
    }

    public SpeedyQuery process() throws Exception {

        String sanitizedURI = URLDecoder.decode(requestURI, StandardCharsets.UTF_8);
        if (sanitizedURI.contains(SpeedyConstant.URI)) {
            int indexOf = sanitizedURI.indexOf(SpeedyConstant.URI);
            sanitizedURI = sanitizedURI.substring(indexOf + SpeedyConstant.URI.length());
        }

        UriComponents uriComponents = UriComponentsBuilder.fromUriString(sanitizedURI)
                .build();

        EntityMetadata entityMetadata = this.extractEntity(uriComponents);

        this.speedyQuery = new SpeedyQueryImpl(entityMetadata);

        captureUrlParams(uriComponents);

        processFilters();

        addToOrderList("$orderBy", false);
        addToOrderList("$orderByDesc", true);

        extractPageInfo(uriComponents);


        return this.speedyQuery;
    }

    private void extractPageInfo(UriComponents uriComponents) throws SpeedyHttpException {
        if (uriComponents.getQueryParams().containsKey("$pageSize")) {
            String $pageSize = uriComponents.getQueryParams().getFirst("$pageSize");
            try {
                Integer pageSize = TypeConverterRegistry.fromString($pageSize.replaceAll("['\" ]", ""), Integer.class);
                Objects.requireNonNull(pageSize);
                speedyQuery.addPageSize(pageSize);
            } catch (NumberFormatException e) {
                log.error("Invalid value for $pageSize. Must be an integer.");
            }
        }

        if (uriComponents.getQueryParams().containsKey("$pageNo")) {
            String $pageNo = uriComponents.getQueryParams().getFirst("$pageNo");
            try {
                Integer pageNo = TypeConverterRegistry.fromString($pageNo.replaceAll("['\" ]", ""), Integer.class);
                Objects.requireNonNull(pageNo);
                speedyQuery.addPageSize(pageNo);
            } catch (NumberFormatException e) {
                log.error("Invalid value for $pageNo. Must be an integer.");
            }
        }

        if (uriComponents.getQueryParams().containsKey("$format")) {
            String $format = uriComponents.getQueryParams().getFirst("$format");
            if ($format != null) {
                speedyQuery.addFormat(TypeConverterRegistry.fromString($format.replaceAll("['\" ]", ""), String.class));
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

    private void captureUrlParams(UriComponents uriComponents) throws SpeedyHttpException {
        if (!uriComponents.getQueryParams().isEmpty()) {
            MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                String key = entry.getKey().strip().trim();
                if (!key.startsWith("$") && speedyQuery.getConditionFactory().createQueryField(key) == null) {
                    continue;
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
                // TODO check this condition
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
                        SpeedyValue speedyValue = SpeedyValueFactory.basicFromString(queryField.getMetadataForParsing(), value);
                        speedyValueList.add(speedyValue);
                    }
                    SpeedyCollection fieldValue = SpeedyValueFactory.fromCollection(speedyValueList);
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
                            return TypeConverterRegistry.fromString(item.replaceAll("['\" ]", ""), String.class);
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
