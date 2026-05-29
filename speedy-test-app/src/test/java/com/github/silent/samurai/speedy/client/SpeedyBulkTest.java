package com.github.silent.samurai.speedy.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.silent.samurai.speedy.TestApplication;
import com.github.silent.samurai.speedy.client.test.SpeedyTest;
import com.github.silent.samurai.speedy.client.test.SpeedyTestResult;
import com.github.silent.samurai.speedy.entity.Supplier;
import com.github.silent.samurai.speedy.utils.CommonUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class SpeedyBulkTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private SpeedyTest client;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        client = SpeedyTest.mockMvc(mockMvc);
        mapper = CommonUtil.json();
    }

    @Test
    void bulkDelete_removesMultipleSuppliers() throws Exception {
        String phoneBase = "+98-" + Long.toString(System.nanoTime()).substring(9);
        String phone1 = phoneBase + "1";
        String phone2 = phoneBase + "2";

        SpeedyTestResult r1 = client.create("Supplier")
                .field("name", "BulkDelete 1 " + System.nanoTime())
                .field("phoneNo", phone1)
                .field("altPhoneNo", phone1)
                .execute()
                .expectOk();

        SpeedyTestResult r2 = client.create("Supplier")
                .field("name", "BulkDelete 2 " + System.nanoTime())
                .field("phoneNo", phone2)
                .field("altPhoneNo", phone2)
                .execute()
                .expectOk();

        String id1 = r1.jsonPath("$.payload[0].id");
        String id2 = r2.jsonPath("$.payload[0].id");

        ObjectNode pk1 = mapper.createObjectNode();
        pk1.put("id", id1);
        ObjectNode pk2 = mapper.createObjectNode();
        pk2.put("id", id2);

        client.deleteMany("Supplier").items(List.of(pk1, pk2)).execute()
                .expectOk()
                .expectJsonPath("$.payload[*]", hasSize(2));

        EntityManager em = entityManagerFactory.createEntityManager();
        List<Supplier> suppliers = em.createQuery(
                        "SELECT s FROM Supplier s WHERE s.id IN (:id1, :id2)", Supplier.class)
                .setParameter("id1", id1)
                .setParameter("id2", id2)
                .getResultList();

        assertTrue(suppliers.isEmpty(), "Both suppliers should be deleted");
        em.close();
    }

    @Test
    void bulkCreate_createsMultipleSuppliers() throws Exception {
        String phoneBase = "+97-" + Long.toString(System.nanoTime()).substring(9);
        String name1 = "BulkCreate 1 " + System.nanoTime();
        String name2 = "BulkCreate 2 " + System.nanoTime();
        String phone1 = phoneBase + "1";
        String phone2 = phoneBase + "2";

        ObjectNode entity1 = mapper.createObjectNode();
        entity1.put("name", name1);
        entity1.put("phoneNo", phone1);
        entity1.put("altPhoneNo", phone1);

        ObjectNode entity2 = mapper.createObjectNode();
        entity2.put("name", name2);
        entity2.put("phoneNo", phone2);
        entity2.put("altPhoneNo", phone2);

        client.createMany("Supplier").items(List.of(entity1, entity2)).execute()
                .expectOk()
                .expectJsonPath("$.payload[*]", hasSize(2))
                .expectJsonPath("$.payload[*].id", not(empty()));

        EntityManager em = entityManagerFactory.createEntityManager();
        List<Supplier> created = em.createQuery(
                        "SELECT s FROM Supplier s WHERE s.name IN (:n1, :n2) ORDER BY s.name ASC", Supplier.class)
                .setParameter("n1", name1)
                .setParameter("n2", name2)
                .getResultList();

        assertEquals(2, created.size(), "Both suppliers should be created");
        assertEquals(name1, created.get(0).getName());
        assertEquals(name2, created.get(1).getName());
        em.close();
    }

    @Test
    void emptyCreateArray_shouldSucceed() throws Exception {
        client.createMany("Supplier", List.of())
                .expectOk();
    }
}
