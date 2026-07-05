package com.github.silent.samurai.speedy.io;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// A MockMvc request driver bound to one {@link IoFormat}, so a parameterized test body stays
/// format-agnostic: it builds Jackson trees and reads Jackson trees, while this class owns the
/// {@code Content-Type}/{@code Accept} negotiation, the 200 expectations, and the negotiated
/// content-type assertion on every response.
public final class IoMvc {

    private final MockMvc mvc;
    private final IoFormat fmt;

    public IoMvc(MockMvc mvc, IoFormat fmt) {
        this.mvc = mvc;
        this.fmt = fmt;
    }

    /// POSTs {@code body} (a request tree) to {@code uri}, expecting {@code 200 OK}.
    public MvcResult post(String uri, JsonNode body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(uri)
                        .content(fmt.write(body))
                        .contentType(fmt.media)
                        .accept(fmt.media))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    /// POSTs {@code body} to {@code uri} expecting {@code expectedStatus} — for error-path
    /// tests — and returns the raw result so the caller can assert the error envelope. The
    /// error body is still rendered in the negotiated format (errors after content negotiation
    /// honor {@code Accept}), so {@link #tree} applies here too.
    public MvcResult post(String uri, JsonNode body, int expectedStatus) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(uri)
                        .content(fmt.write(body))
                        .contentType(fmt.media)
                        .accept(fmt.media))
                .andDo(print())
                .andExpect(status().is(expectedStatus))
                .andReturn();
    }

    /// GETs {@code uri}, expecting {@code 200 OK}.
    public MvcResult get(String uri) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(uri)
                        .accept(fmt.media))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    /// PATCHes {@code body} (a request tree) to {@code uri}, expecting {@code 200 OK}.
    public MvcResult patch(String uri, JsonNode body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.patch(uri)
                        .content(fmt.write(body))
                        .contentType(fmt.media)
                        .accept(fmt.media))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    /// DELETEs {@code body} (a request tree) at {@code uri}, expecting {@code 200 OK}.
    public MvcResult delete(String uri, JsonNode body) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.delete(uri)
                        .content(fmt.write(body))
                        .contentType(fmt.media)
                        .accept(fmt.media))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    /// Parses a response into a {@link JsonNode}, first asserting it was rendered in the
    /// negotiated format (guarding against a silent fall-back to another format).
    public JsonNode tree(MvcResult result) {
        fmt.assertNegotiated(result);
        return fmt.tree(result);
    }
}
