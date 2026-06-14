package com.github.silent.samurai.speedy.engine;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.IRequestBodyParser;
import com.github.silent.samurai.speedy.interfaces.IResponseSerializerV2;
import com.github.silent.samurai.speedy.interfaces.SpeedyBody;
import com.github.silent.samurai.speedy.interfaces.SpeedyResponse;
import com.github.silent.samurai.speedy.enums.SpeedyRequestType;
import com.github.silent.samurai.speedy.context.SpeedyContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/// Pipeline interface for processing a single Speedy API request.
///
/// ## Design contract — DO NOT REVERT
///
/// Each method either advances the shared {@link SpeedyContext} (pipeline state) or
/// returns a typed result that the caller ({@code SpeedyFactory}) holds explicitly.
/// For methods that produce a concrete output, both happen: the result is stored in
/// {@code ctx} for downstream handlers AND returned so the factory's orchestration code
/// is readable without inspecting the context bag.
///
/// ## Dispatch switch belongs in SpeedyFactory, NOT here
///
/// This interface exposes named operations ({@link #get}, {@link #query}, etc.) but does
/// NOT decide which one to call. That switch lives in {@code SpeedyFactory.processReqV2}
/// because choosing the operation is orchestration, not engine logic.
/// DO NOT collapse these back into a single {@code execute(SpeedyContext)} method.
///
/// ## Return types are intentional — DO NOT void them
///
/// Methods return typed values even though the values are also in {@code ctx}. The return
/// types make the data flow visible at the factory level and keep this interface testable
/// without inspecting context internals. DO NOT change operation or negotiation methods
/// back to void.
///
public interface SpeedyEngine {

    SpeedyContext newContext(HttpServletRequest request, HttpServletResponse response) throws SpeedyHttpException;

    void prepare(SpeedyContext ctx) throws SpeedyHttpException;

    /// Parses the HTTP method, URI, and raw body bytes into {@code ctx}.
    /// Returns the {@link SpeedyRequestType} (also stored in ctx) so the factory can drive
    /// the dispatch switch without an extra {@code ctx.get} call.
    SpeedyRequestType parseRequest(SpeedyContext ctx) throws SpeedyHttpException;

    /// Selects the body parser for the request's Content-Type. Stored in ctx AND returned.
    IRequestBodyParser selectBodyParser(SpeedyContext ctx) throws SpeedyHttpException;

    /// Parses the raw body using the parser chosen by {@link #selectBodyParser}.
    /// Stored in ctx AND returned.
    SpeedyBody parseBody(SpeedyContext ctx) throws SpeedyHttpException;

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
