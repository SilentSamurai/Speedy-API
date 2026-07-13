package com.github.silent.samurai.speedy.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static com.github.silent.samurai.speedy.policy.PolicyBuilder.denyByDefault;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Exercises field-level ABAC where a wildcard grant covers the whole entity but a single
/// sensitive field is carved out with an explicit deny, on {@link
/// com.github.silent.samurai.speedy.entity.Employee}: the caller can read every field except the
/// one denied field, which is dropped from the response rather than the request being rejected.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PolicyTestConfiguration.class)
class SensitiveFieldPolicyTest {

    @Autowired
    private MockMvc mvc;

    private static String operationUrl(String entity, SpeedyEndpoint endpoint) {
        return SpeedyConstants.URI + "/" + entity + "/" + endpoint.suffix();
    }

    private static JsonNode responseBody(MvcResult result) throws Exception {
        return CommonUtil.json().readTree(result.getResponse().getContentAsString());
    }

    private static RequestPostProcessor withPolicy(SpeedyAuthContext authContext) {
        return request -> {
            request.setAttribute(PolicyTestConfiguration.AUTH_CONTEXT_ATTRIBUTE, authContext);
            return request;
        };
    }

    /// A wildcard read allow grants every field on the entity; an explicit deny on one sensitive
    /// field overrides the wildcard for that field alone. The other fields (including the always-
    /// visible key field) still come back — only the denied field is omitted.
    @Test
    void wildcardReadAllowGrantsEveryFieldExceptTheExplicitlyDeniedSensitiveField() throws Exception {
        String employeeId = createScratchEmployee("Jane Doe", 95000.0);

        SpeedyAuthContext authContext = denyByDefault()
                .allow("read-all-employee-fields", PermissionType.READ, "Employee.*")
                .deny("hide-salary", PermissionType.READ, "Employee.salary")
                .build();

        MvcResult result = mvc.perform(get(SpeedyConstants.URI + "/Employee")
                        .with(withPolicy(authContext))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.length()").value(1))
                .andExpect(jsonPath("$.payload[0].id").value(employeeId))
                .andExpect(jsonPath("$.payload[0].name").value("Jane Doe"))
                .andReturn();

        JsonNode payload = responseBody(result).path("payload");
        JsonNode employee = payload.get(0);
        assertTrue(employee.has("id"));
        assertTrue(employee.has("name"));
        assertFalse(employee.has("salary"));
    }

    /// Creates a throwaway Employee with an explicit creation policy, so this test doesn't need to
    /// depend on shared seed data.
    private String createScratchEmployee(String name, double salary) throws Exception {
        SpeedyAuthContext authContext = denyByDefault()
                .allow("scratch-create", PermissionType.CREATE, "Employee.*")
                .build();

        MvcResult result = mvc.perform(post(operationUrl("Employee", SpeedyEndpoint.CREATE))
                        .with(withPolicy(authContext))
                        .content("[{\"name\":\"" + name + "\",\"salary\":" + salary + "}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return responseBody(result).path("payload").get(0).path("id").asText();
    }

}
