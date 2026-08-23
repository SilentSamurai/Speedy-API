package com.github.silent.samurai.speedy.docs;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Rewrites `api-docs.json` from the spec the application actually serves.
///
/// That file is the `inputSpec` the openapi-generator plugin builds the client from, so it is a
/// snapshot, not a generated artifact — nothing recreates it, and nothing fails when it drifts from
/// the entities. It has drifted before. This is the one command that refreshes it:
///
/// ```
/// mvn test -pl speedy-test-app -Dtest=ApiDocsSnapshotWriter -Dspeedy.write-api-docs=true
/// ```
///
/// Guarded by that property so it never runs as part of the suite: it writes into the source tree,
/// which a test run must not do on its own. Rebuild afterwards so the generated client picks the
/// change up, and review the diff — a surprise in it is the point of keeping the file.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@EnabledIfSystemProperty(named = "speedy.write-api-docs", matches = "true")
class ApiDocsSnapshotWriter {

    @Autowired
    private MockMvc mvc;

    @Test
    void writeApiDocsSnapshot() throws Exception {
        String served = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Indented, so the next drift is reviewable as a diff rather than as one 250 KB line.
        // Arrays are indented like objects and there is no space before a colon: Jackson's defaults
        // differ from the committed file on both counts, and either one turns an unrelated edit into
        // a whole-file rewrite that hides what actually changed.
        DefaultIndenter indenter = new DefaultIndenter();
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
                .withObjectIndenter(indenter)
                .withSeparators(Separators.createDefaultInstance()
                        .withObjectFieldValueSpacing(Separators.Spacing.AFTER));
        printer.indentArraysWith(indenter);

        ObjectNode spec = (ObjectNode) CommonUtil.json().readTree(served);
        Path target = Path.of(System.getProperty("user.dir"), "api-docs.json");
        carryOverServers(spec, target);
        sortByKey(spec, "paths");
        sortByKey((ObjectNode) spec.get("components"), "schemas");

        String formatted = CommonUtil.json().writer(printer).writeValueAsString(spec);
        Files.writeString(target, formatted + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    /// `servers` describes where the application is deployed, not the shape of its API, and MockMvc
    /// has no port to report — it would rewrite the URL to one nothing listens on. The snapshot keeps
    /// whatever it already had.
    private static void carryOverServers(ObjectNode spec, Path existing) throws Exception {
        if (!Files.exists(existing)) {
            return;
        }
        JsonNode previous = CommonUtil.json().readTree(Files.readString(existing, StandardCharsets.UTF_8));
        if (previous.hasNonNull("servers")) {
            spec.set("servers", previous.get("servers"));
        }
    }

    /// springdoc emits paths and schemas in hash-map order, which differs between runs. Left alone,
    /// every regeneration reorders the whole file and buries the change that prompted it. JSON object
    /// members are unordered, so sorting them costs nothing and the generator reads the same spec.
    private static void sortByKey(ObjectNode parent, String field) {
        if (parent == null || !parent.hasNonNull(field)) {
            return;
        }
        ObjectNode unsorted = (ObjectNode) parent.get(field);
        List<String> names = new ArrayList<>();
        unsorted.fieldNames().forEachRemaining(names::add);
        Collections.sort(names);
        ObjectNode sorted = CommonUtil.json().createObjectNode();
        names.forEach(name -> sorted.set(name, unsorted.get(name)));
        parent.set(field, sorted);
    }
}
