package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.MetaModel;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.request.RequestContext;

/// # EntityCaptureHandler
/// 
/// This handler is responsible for parsing the request URI and extracting entity metadata
/// from the Speedy query. It acts as a crucial step in the request processing pipeline
/// by identifying which entity the request is targeting.
/// 
/// ## Purpose
/// - Parses the incoming request URI using [SpeedyUriContext]
/// - Extracts entity metadata from the parsed query
/// - Sets the entity metadata in the request context for downstream handlers
/// 
/// ## Processing Flow
/// 1. Retrieves the [MetaModel] and request URI from context
/// 2. Creates a [SpeedyUriContext] parser with the metamodel and URI
/// 3. Parses the URI to extract the [SpeedyQuery]
/// 4. Extracts the target entity metadata from query
/// 5. Sets the entity metadata in the request context
/// 6. Delegates to the next handler in the chain
/// 
/// ## Chain Position
/// This handler typically runs early in the processing chain, after basic request
/// validation but before entity-specific operations like CRUD operations.
/// 
/// ## Example Usage
/// ```java
/// Handler nextHandler = new ValidationHandler();
/// Handler entityHandler = new EntityCaptureHandler(nextHandler);
/// entityHandler.process(requestContext);
/// ```
public class EntityCaptureHandler implements Handler {

    /// The next handler in the processing chain
    final Handler next;

    /// Creates a new EntityCaptureHandler with the specified next handler
    /// 
    /// @param next The next handler to process after entity capture
    public EntityCaptureHandler(Handler next) {
        this.next = next;
    }

    /// Processes the request by capturing entity metadata from the URI
    /// 
    /// This method performs the following steps:
    /// 1. Extracts the metamodel and request URI from context
    /// 2. Parses the URI using [SpeedyUriContext]
    /// 3. Extracts entity metadata from the parsed query
    /// 4. Sets the entity metadata in the context for downstream handlers
    /// 5. Delegates processing to the next handler
    /// 
    /// @param context The request context containing the request information
    /// @throws SpeedyHttpException If there's an error parsing the URI or extracting entity metadata
    @Override
    public void process(RequestContext context) throws SpeedyHttpException {
        // Get the metamodel and request URI from the context
        MetaModel metaModel = context.getMetaModel();
        String requestURI = context.getRequestUri();

        // Parse the URI to extract the Speedy query
        SpeedyUriContext parser = new SpeedyUriContext(metaModel, requestURI);
        SpeedyQuery uriSpeedyQuery = parser.parse();

        // Extract the target entity metadata from the parsed query
        EntityMetadata resourceMetadata = uriSpeedyQuery.getFrom();
        context.setEntityMetadata(resourceMetadata);

        // Continue processing with the next handler
        next.process(context);
    }
}
