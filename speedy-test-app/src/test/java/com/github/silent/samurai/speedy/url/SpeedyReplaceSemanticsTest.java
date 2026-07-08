package com.github.silent.samurai.speedy.url;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.entity.Supplier;
import com.github.silent.samurai.speedy.enums.SpeedyEndpoint;
import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.repositories.SupplierRepository;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Verifies that PUT (full replace) and PATCH (partial update) now have distinct semantics
/// (issue #125), driven as raw HTTP verbs against the {@code $update} endpoint with the PK in
/// the body. Uses {@link Supplier} — required {@code name}/{@code phoneNo}/{@code altPhoneNo},
/// nullable {@code address}/{@code email} — and reads the persisted row back through the
/// repository. Runs against H2 locally and Postgres + MySQL in CI.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class SpeedyReplaceSemanticsTest {

    private static final String UPDATE_URL = SpeedyConstants.URI + "/Supplier/" + SpeedyEndpoint.UPDATE.suffix();
    private static final AtomicLong COUNTER = new AtomicLong();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SupplierRepository supplierRepository;

    /// PATCH omitting a nullable field must leave it untouched (partial-update, unchanged behaviour).
    @Test
    void patch_omittingNullableField_leavesItUnchanged() throws Exception {
        Supplier supplier = saveSupplier("patch-supplier", "123 Old Street", "old@example.com");

        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("id", supplier.getId());
        body.put("name", "patch-supplier-renamed"); // address & email omitted

        mvc.perform(MockMvcRequestBuilders.patch(UPDATE_URL)
                        .content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk());

        Supplier reloaded = reload(supplier.getId());
        assertEquals("patch-supplier-renamed", reloaded.getName());
        assertEquals("123 Old Street", reloaded.getAddress(), "PATCH must not touch omitted fields");
        assertEquals("old@example.com", reloaded.getEmail(), "PATCH must not touch omitted fields");
    }

    /// PUT omitting a nullable field must reset it to null (full replace).
    @Test
    void put_omittingNullableField_resetsItToNull() throws Exception {
        Supplier supplier = saveSupplier("put-supplier", "456 Some Ave", "present@example.com");

        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("id", supplier.getId());
        body.put("name", "put-supplier-replaced");
        body.put("phoneNo", supplier.getPhoneNo());
        body.put("altPhoneNo", supplier.getAltPhoneNo());
        // address & email deliberately omitted -> full replace resets them

        mvc.perform(MockMvcRequestBuilders.put(UPDATE_URL)
                        .content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk());

        Supplier reloaded = reload(supplier.getId());
        assertEquals("put-supplier-replaced", reloaded.getName());
        assertNull(reloaded.getAddress(), "PUT must reset an omitted nullable field to null");
        assertNull(reloaded.getEmail(), "PUT must reset an omitted nullable field to null");
        // required fields supplied in the payload are preserved
        assertEquals(supplier.getPhoneNo(), reloaded.getPhoneNo());
    }

    /// PUT omitting a required field must be rejected with 400.
    @Test
    void put_omittingRequiredField_returns400() throws Exception {
        Supplier supplier = saveSupplier("put-missing-required", "789 Blvd", "req@example.com");

        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("id", supplier.getId());
        body.put("name", "still-here"); // phoneNo & altPhoneNo (required) omitted

        mvc.perform(MockMvcRequestBuilders.put(UPDATE_URL)
                        .content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isBadRequest());
    }

    /// PUT against a well-formed but non-existent PK must return 404.
    @Test
    void put_onNonExistentPk_returns404() throws Exception {
        String phone = uniquePhone();

        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("id", UUID.randomUUID().toString());
        body.put("name", "ghost");
        body.put("phoneNo", phone);
        body.put("altPhoneNo", phone);

        mvc.perform(MockMvcRequestBuilders.put(UPDATE_URL)
                        .content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isNotFound());
    }

    /// PUT is idempotent: applying the same full representation twice yields the same state.
    @Test
    void put_isIdempotent() throws Exception {
        Supplier supplier = saveSupplier("idempotent-supplier", "1 Repeat Rd", "dup@example.com");

        ObjectNode body = CommonUtil.json().createObjectNode();
        body.put("id", supplier.getId());
        body.put("name", "idempotent-replaced");
        body.put("phoneNo", supplier.getPhoneNo());
        body.put("altPhoneNo", supplier.getAltPhoneNo());
        body.put("email", "kept@example.com"); // address omitted -> nulled on every apply

        performPut(body);
        Supplier afterFirst = reload(supplier.getId());

        performPut(body);
        Supplier afterSecond = reload(supplier.getId());

        assertEquals(afterFirst.getName(), afterSecond.getName());
        assertEquals(afterFirst.getEmail(), afterSecond.getEmail());
        assertEquals(afterFirst.getAddress(), afterSecond.getAddress());
        assertEquals("idempotent-replaced", afterSecond.getName());
        assertEquals("kept@example.com", afterSecond.getEmail());
        assertNull(afterSecond.getAddress(), "omitted nullable field stays null across idempotent PUTs");
    }

    /* -------------------------------------------------------------------- */
    /* Helpers                                                              */
    /* -------------------------------------------------------------------- */

    private void performPut(ObjectNode body) throws Exception {
        mvc.perform(MockMvcRequestBuilders.put(UPDATE_URL)
                        .content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().isOk());
    }

    private Supplier saveSupplier(String name, String address, String email) {
        String phone = uniquePhone();
        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setAddress(address);
        supplier.setEmail(email);
        supplier.setPhoneNo(phone);
        supplier.setAltPhoneNo("a" + phone);
        supplierRepository.save(supplier);
        assertNotNull(supplier.getId());
        return supplier;
    }

    private Supplier reload(String id) {
        Optional<Supplier> found = supplierRepository.findById(id);
        assertTrue(found.isPresent(), "supplier should still exist after write");
        return found.get();
    }

    /// Unique phone value within the 15-char column limit and distinct from seed data.
    private String uniquePhone() {
        return "p" + COUNTER.incrementAndGet() + "x" + (System.nanoTime() % 100000);
    }
}
