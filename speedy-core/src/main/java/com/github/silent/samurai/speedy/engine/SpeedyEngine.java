package com.github.silent.samurai.speedy.engine;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.request.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.response.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.response.SpeedyResponse;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.models.SpeedyHeaders;
import com.github.silent.samurai.speedy.parser.SpeedyUriContext;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/// Pipeline interface for processing a single Speedy API request.
///
/// ## The pipeline — DO NOT REVERT
///
/// {@code SpeedyFactory.processReqV2} drives these steps in order:
/// ```
/// 1. newContext        -> SpeedyContext          get the request into ctx
///    prepare(ctx)       (void)                    setup: store QueryProcessor in ctx
/// 2. parseUri          -> SpeedyUriContext       parse the URI (also puts TransactionMode in ctx)
/// 3. parseHeaders      -> SpeedyHeaders          parse method + headers + raw body bytes
///    resolveOperation  -> SpeedyRequestType      classify op from URI + HTTP method
/// 4. selectSerializer  -> IResponseSerializerV2  } negotiate output + input format
///    selectBodyParser  -> IRequestBodyParser     }
/// 5. switch(type)      -> SpeedyResponse         single dispatch in the factory: write ops
///                                                parse their body (parseXBody) then run the
///                                                operation; read ops just run the operation
/// 6. serializer.write(resp, response)            write the response
/// ```
///
/// ## Every parse/select step returns its produced type — DO NOT void them
///
/// Each step stores its result in the shared {@link SpeedyContext} for downstream handlers AND
/// returns it so the factory's orchestration reads cleanly without inspecting the context bag.
/// The method name matches what it returns: {@link #parseUri} returns the parsed URI context,
/// {@link #parseHeaders} returns the parsed headers, {@link #resolveOperation} returns the
/// request type. {@link #prepare} is the one intentional {@code void} — it is setup, not a parse.
/// DO NOT change these back to void or re-fuse them into a single {@code parseRequest}.
///
/// ## resolveOperation runs after parseUri AND parseHeaders
///
/// Operation resolution needs both the parsed URI and the HTTP method, so it is its own step
/// after the two parsers. DO NOT fold it back into {@link #parseHeaders}.
///
/// ## A single dispatch switch belongs in SpeedyFactory, NOT here
///
/// This interface exposes named operations ({@link #get}, {@link #query}, etc.) and the
/// per-type body parsers ({@link #parseQueryBody}, {@link #parseCreateBody}, etc.) but does
/// NOT decide which to call. That decision is one {@code switch(type)} in
/// {@code SpeedyFactory.processReqV2}: each write-op arm parses its body then runs the
/// operation; read ops (GET_LIST, METADATA) just run the operation. There is exactly ONE such
/// switch — DO NOT add a second one, and DO NOT push it down into a handler (that is the very
/// thing the old {@code BodyParserHandler} did wrong). DO NOT collapse these into a single
/// {@code execute(SpeedyContext)} method either.
public interface SpeedyEngine {

    SpeedyContext newContext(HttpServletRequest request, HttpServletResponse response) throws SpeedyHttpException;

    void prepare(SpeedyContext ctx) throws SpeedyHttpException;

    /// Parses the request URI into a {@link SpeedyUriContext} (also puts {@code TransactionMode}
    /// in ctx). Stored in ctx AND returned.
    SpeedyUriContext parseUri(SpeedyContext ctx) throws SpeedyHttpException;

    /// Parses the HTTP method, request headers, and raw body bytes. Returns the parsed
    /// {@link SpeedyHeaders} (HTTP method and raw bytes are also stored in ctx).
    SpeedyHeaders parseHeaders(SpeedyContext ctx) throws SpeedyHttpException;

    /// Classifies the request into a {@link SpeedyRequestType} from the parsed URI and HTTP
    /// method. Must run after {@link #parseUri} and {@link #parseHeaders}. Stored in ctx AND
    /// returned so the factory drives the dispatch switch without an extra {@code ctx.get} call.
    SpeedyRequestType resolveOperation(SpeedyContext ctx) throws SpeedyHttpException;

    /// Selects the body parser for the request's Content-Type. Stored in ctx AND returned.
    IRequestBodyParser selectBodyParser(SpeedyContext ctx) throws SpeedyHttpException;

    // --- Per-type body parsers ---
    // Each parses the raw body with the parser chosen by selectBodyParser into the SpeedyBody
    // subtype for one request type, stores it in ctx for the matching operation handler, and
    // returns it. The factory's single request-type switch routes each write op to its parser.
    // GET_LIST / METADATA carry no body, so they have no parser here.

    SpeedyBody parseQueryBody(SpeedyContext ctx) throws SpeedyHttpException;

    SpeedyBody parseCreateBody(SpeedyContext ctx) throws SpeedyHttpException;

    SpeedyBody parseUpdateBody(SpeedyContext ctx) throws SpeedyHttpException;

    SpeedyBody parseDeleteBody(SpeedyContext ctx) throws SpeedyHttpException;

    // --- Operation methods ---
    // SpeedyFactory owns the switch that calls these; see SpeedyFactory.processReqV2.
    // Each returns the SpeedyResponse it produced (also stored in ctx).

    SpeedyResponse get(SpeedyContext ctx) throws SpeedyHttpException;

    SpeedyResponse query(SpeedyContext ctx) throws SpeedyHttpException;

    SpeedyResponse create(SpeedyContext ctx) throws SpeedyHttpException;

    SpeedyResponse update(SpeedyContext ctx) throws SpeedyHttpException;

    SpeedyResponse delete(SpeedyContext ctx) throws SpeedyHttpException;

    SpeedyResponse metadata(SpeedyContext ctx) throws SpeedyHttpException;

    /// Selects the response serializer from the Accept header. Stored in ctx AND returned
    /// so the factory calls {@code serializer.write(resp, response)} directly.
    IResponseSerializerV2 selectSerializer(SpeedyContext ctx) throws SpeedyHttpException;

}
