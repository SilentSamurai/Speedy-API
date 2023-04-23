package com.github.silent.samurai.request.get;

import com.github.silent.samurai.AntlrParser;
import com.github.silent.samurai.Request;
import com.github.silent.samurai.exceptions.NotFoundException;
import com.github.silent.samurai.helpers.MetadataUtil;
import com.github.silent.samurai.interfaces.EntityMetadata;
import com.github.silent.samurai.interfaces.IResponseSerializer;
import com.github.silent.samurai.utils.CommonUtil;
import org.springframework.util.MultiValueMap;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public class GetRequestParser {

    private final GetRequestContext context;

    public GetRequestParser(GetRequestContext context) {
        this.context = context;
    }

    private void parseURI(String requestURI) throws UnsupportedEncodingException {
        AntlrParser antlrParser = new AntlrParser(requestURI);
        Request request = antlrParser.parse();
        context.setRequest(request);
    }

    private boolean updateSerializationType() throws NotFoundException {
        context.setSerializationType(IResponseSerializer.MULTIPLE_ENTITY);
        if (context.getResourceMetadata() == null) {
            throw new NotFoundException("Entity Not Found " + context.getRequest().getResource());
        }
        EntityMetadata resource = context.getResourceMetadata();
        if (MetadataUtil.hasOnlyPrimaryKeyFields(resource, context.getKeywords().keySet())) {
            context.setPrimaryKey(true);
            context.setSerializationType(IResponseSerializer.SINGLE_ENTITY);
            return true;
        }
        return false;
    }

    private void processFilters() throws NotFoundException {
        List<String> arguments = context.getRequest().getArguments();
        Map<String, String> keywords = context.getRequest().getKeywords();
        if (!arguments.isEmpty()) {
            arguments.forEach(arg -> context.getArguments().add(arg));
            keywords.put("id", arguments.get(0));
        }
        if (!keywords.isEmpty()) {
            keywords.forEach((key, val) -> {
                String strVal = CommonUtil.getGson().fromJson(val, String.class);
                context.getKeywords().put(key, strVal);
            });
        }
        this.updateSerializationType();
    }

    private void processQuery() {
        MultiValueMap<String, String> query = context.getRequest().getQuery();
        if (!query.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : query.entrySet()) {
                String key = entry.getKey();
                List<String> valueList = entry.getValue();
                for (String val : valueList) {
                    String strVal = CommonUtil.getGson().fromJson(val, String.class);
                    context.getQueryParams().add(key, strVal);
                }
            }
        }
    }

    private void processResource() throws NotFoundException {
        String resource = context.getRequest().getResource();
        EntityMetadata entityMetadata = context.getMetaModelProcessor().findEntityMetadata(resource);
        context.setResourceMetadata(entityMetadata);
    }

    public void process() throws UnsupportedEncodingException, NotFoundException {
        parseURI(context.getRequestURI());
        processResource();
        processFilters();
        processQuery();
    }

}
