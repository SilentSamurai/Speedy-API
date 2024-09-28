package com.github.silent.samurai.speedy.api.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.RequestEntity.BodyBuilder;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.Map.Entry;

@Getter
public class ApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiClient.class);

    private boolean debugging = false;

    private HttpHeaders defaultHeaders = new HttpHeaders();
    private MultiValueMap<String, String> defaultCookies = new LinkedMultiValueMap<String, String>();

    private String basePath = "http://localhost:8080";

    private RestTemplate restTemplate;

    private Map<String, String> authentications;

    public ApiClient() {
        this.restTemplate = buildRestTemplate();
        init();
    }

    public ApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        init();
    }

    protected void init() {
        // Set default User-Agent.
        setUserAgent("Speedy-Java-SDK");

        // Setup authentications (key: authentication name, value: authentication).
        authentications = new HashMap<String, String>();
        // Prevent the authentications from being modified.
        authentications = Collections.unmodifiableMap(authentications);
    }


    public void setUserAgent(String userAgent) {
        addDefaultHeader("User-Agent", userAgent);
    }

    public void addDefaultHeader(String name, String value) {
        if (defaultHeaders.containsKey(name)) {
            defaultHeaders.remove(name);
        }
        defaultHeaders.add(name, value);
    }

    public void addAuthorization(String authValue) {
        addDefaultHeader("Authorization", authValue);
    }

    public void setDebugging(boolean debugging) {
        List<ClientHttpRequestInterceptor> currentInterceptors = this.restTemplate.getInterceptors();
        if (debugging) {
            if (currentInterceptors == null) {
                currentInterceptors = new ArrayList<ClientHttpRequestInterceptor>();
            }
            ClientHttpRequestInterceptor interceptor = new ApiClientHttpRequestInterceptor();
            currentInterceptors.add(interceptor);
            this.restTemplate.setInterceptors(currentInterceptors);
        } else {
            if (currentInterceptors != null && !currentInterceptors.isEmpty()) {
                Iterator<ClientHttpRequestInterceptor> iter = currentInterceptors.iterator();
                while (iter.hasNext()) {
                    ClientHttpRequestInterceptor interceptor = iter.next();
                    if (interceptor instanceof ApiClientHttpRequestInterceptor) {
                        iter.remove();
                    }
                }
                this.restTemplate.setInterceptors(currentInterceptors);
            }
        }
        this.debugging = debugging;
    }


    /**
     * Check if the given MIME is a JSON MIME.
     * JSON MIME examples:
     * application/json
     * application/json; charset=UTF8
     * APPLICATION/JSON
     *
     * @param mediaType the input MediaType
     * @return boolean true if the MediaType represents JSON, false otherwise
     */
    public boolean isJsonMime(MediaType mediaType) {
        return mediaType != null && (MediaType.APPLICATION_JSON.isCompatibleWith(mediaType) || mediaType.getSubtype().matches("^.*\\+json[;]?\\s*$"));
    }

    /**
     * Select the Accept header's value from the given accepts array:
     * if JSON exists in the given array, use it;
     * otherwise use all of them (joining into a string)
     *
     * @param accepts The accepts array to select from
     * @return List The list of MediaTypes to use for the Accept header
     */
    public List<MediaType> selectHeaderAccept(String[] accepts) {
        if (accepts.length == 0) {
            return null;
        }
        for (String accept : accepts) {
            MediaType mediaType = MediaType.parseMediaType(accept);
            if (isJsonMime(mediaType)) {
                return Collections.singletonList(mediaType);
            }
        }
        return MediaType.parseMediaTypes(StringUtils.arrayToCommaDelimitedString(accepts));
    }

    /**
     * Select the Content-Type header's value from the given array:
     * if JSON exists in the given array, use it;
     * otherwise use the first one of the array.
     *
     * @param contentTypes The Content-Type array to select from
     * @return MediaType The Content-Type header to use. If the given array is empty, JSON will be used.
     */
    public MediaType selectHeaderContentType(String[] contentTypes) {
        if (contentTypes.length == 0) {
            return MediaType.APPLICATION_JSON;
        }
        for (String contentType : contentTypes) {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            if (isJsonMime(mediaType)) {
                return mediaType;
            }
        }
        return MediaType.parseMediaType(contentTypes[0]);
    }

    /**
     * Select the body to use for the request
     *
     * @param obj         the body object
     * @param formParams  the form parameters
     * @param contentType the content type of the request
     * @return Object the selected body
     */
    protected Object selectBody(Object obj, MultiValueMap<String, Object> formParams, MediaType contentType) {
        boolean isForm = MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType) || MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType);
        return isForm ? formParams : obj;
    }

    /**
     * Invoke API by sending HTTP request with the given options.
     *
     * @param <T>          the return type to use
     * @param path         The sub-path of the HTTP URL
     * @param method       The request method
     * @param pathParams   The path parameters
     * @param queryParams  The query parameters
     * @param body         The request body object
     * @param headerParams The header parameters
     * @param cookieParams The cookie parameters
     * @param formParams   The form parameters
     * @param accept       The request's Accept header
     * @param contentType  The request's Content-Type header
     * @param authNames    The authentications to apply
     * @param returnType   The return type into which to deserialize the response
     * @return ResponseEntity&lt;T&gt; The response of the chosen type
     */
    public <T> ResponseEntity<T> invokeAPI(String path,
                                           HttpMethod method, Map<String, Object> pathParams,
                                           MultiValueMap<String, String> queryParams,
                                           JsonNode body, HttpHeaders headerParams,
                                           MultiValueMap<String, String> cookieParams,
                                           MultiValueMap<String, Object> formParams, List<MediaType> accept,
                                           MediaType contentType, String[] authNames,
                                           ParameterizedTypeReference<T> returnType) throws RestClientException {

//        updateParamsForAuth(authNames, queryParams, headerParams, cookieParams);

        Map<String, Object> uriParams = new HashMap<>();
        uriParams.putAll(pathParams);

//        String finalUri = path;

//        if (queryParams != null && !queryParams.isEmpty()) {
//            //Include queryParams in uriParams taking into account the paramName
//            String queryUri = generateQueryUri(queryParams, uriParams);
//            //Append to finalUri the templatized query string like "?param1={param1Value}&.......
//            finalUri += "?" + queryUri;
//        }

        final String uriPath = UriComponentsBuilder
                .fromHttpUrl(basePath)
                .path(path)
                .queryParams(queryParams)
                .buildAndExpand(uriParams).toUriString();

        URI uri;
        try {
//            String encoded = URLEncoder.encode(uriPath, StandardCharsets.UTF_8);
            uri = new URI(uriPath);
        } catch (URISyntaxException ex) {
            throw new RestClientException("Could not build URL: " + uriPath, ex);
        }

        final BodyBuilder requestBuilder = RequestEntity.method(method, uri);
        if (accept != null) {
            requestBuilder.accept(accept.toArray(new MediaType[accept.size()]));
        }
        if (contentType != null) {
            requestBuilder.contentType(contentType);
        }

        addHeadersToRequest(headerParams, requestBuilder);
        addHeadersToRequest(defaultHeaders, requestBuilder);
        addCookiesToRequest(cookieParams, requestBuilder);
        addCookiesToRequest(defaultCookies, requestBuilder);

        RequestEntity<Object> requestEntity = requestBuilder.body(selectBody(body, formParams, contentType));

        try {
            ResponseEntity<T> responseEntity = restTemplate.exchange(requestEntity, returnType);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                return responseEntity;
            } else {
                // The error handler built into the RestTemplate should handle 400 and 500 series errors.
                throw new RestClientException("API returned " + responseEntity.getStatusCode() + " and it wasn't handled by the RestTemplate error handler");
            }

        } catch (HttpServerErrorException e) {
            // log
            LOGGER.error("speedy api request failed {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            // log
            LOGGER.error("speedy api request failed ", e);
            throw e;
        }

    }

    /**
     * Add headers to the request that is being built
     *
     * @param headers        The headers to add
     * @param requestBuilder The current request
     */
    protected void addHeadersToRequest(HttpHeaders headers, BodyBuilder requestBuilder) {
        for (Entry<String, List<String>> entry : headers.entrySet()) {
            List<String> values = entry.getValue();
            for (String value : values) {
                if (value != null) {
                    requestBuilder.header(entry.getKey(), value);
                }
            }
        }
    }

    /**
     * Add cookies to the request that is being built
     *
     * @param cookies        The cookies to add
     * @param requestBuilder The current request
     */
    protected void addCookiesToRequest(MultiValueMap<String, String> cookies, BodyBuilder requestBuilder) {
        if (!cookies.isEmpty()) {
            requestBuilder.header("Cookie", buildCookieHeader(cookies));
        }
    }

    /**
     * Build cookie header. Keeps a single value per cookie (as per <a href="https://tools.ietf.org/html/rfc6265#section-5.3">
     * RFC6265 section 5.3</a>).
     *
     * @param cookies map all cookies
     * @return header string for cookies.
     */
    private String buildCookieHeader(MultiValueMap<String, String> cookies) {
        final StringBuilder cookieValue = new StringBuilder();
        String delimiter = "";
        for (final Entry<String, List<String>> entry : cookies.entrySet()) {
            final String value = entry.getValue().get(entry.getValue().size() - 1);
            cookieValue.append(String.format("%s%s=%s", delimiter, entry.getKey(), value));
            delimiter = "; ";
        }
        return cookieValue.toString();
    }

    /**
     * Build the RestTemplate used to make HTTP requests.
     *
     * @return RestTemplate
     */
    protected RestTemplate buildRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // This allows us to read the response more than once - Necessary for debugging.
        restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(restTemplate.getRequestFactory()));

        // disable default URL encoding
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        restTemplate.setUriTemplateHandler(uriBuilderFactory);
        return restTemplate;
    }

    private class ApiClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
        private final Log log = LogFactory.getLog(ApiClientHttpRequestInterceptor.class);

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            logRequest(request, body);
            ClientHttpResponse response = execution.execute(request, body);
            logResponse(response);
            return response;
        }

        private void logRequest(HttpRequest request, byte[] body) throws UnsupportedEncodingException {
            log.info("URI: " + request.getURI());
            log.info("HTTP Method: " + request.getMethod());
            log.info("HTTP Headers: " + headersToString(request.getHeaders()));
            log.info("Request Body: " + new String(body, StandardCharsets.UTF_8));
        }

        private void logResponse(ClientHttpResponse response) throws IOException {
            log.info("HTTP Status Code: " + response.getRawStatusCode());
            log.info("Status Text: " + response.getStatusText());
            log.info("HTTP Headers: " + headersToString(response.getHeaders()));
            log.info("Response Body: " + bodyToString(response.getBody()));
        }

        private String headersToString(HttpHeaders headers) {
            if (headers == null || headers.isEmpty()) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (Entry<String, List<String>> entry : headers.entrySet()) {
                builder.append(entry.getKey()).append("=[");
                for (String value : entry.getValue()) {
                    builder.append(value).append(",");
                }
                builder.setLength(builder.length() - 1); // Get rid of trailing comma
                builder.append("],");
            }
            builder.setLength(builder.length() - 1); // Get rid of trailing comma
            return builder.toString();
        }

        private String bodyToString(InputStream body) throws IOException {
            StringBuilder builder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8));
            String line = bufferedReader.readLine();
            while (line != null) {
                builder.append(line).append(System.lineSeparator());
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            return builder.toString();
        }
    }
}
