package com.github.silent.samurai;

import com.github.silent.samurai.metamodel.JpaMetaModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManagerFactory;

import static org.junit.Assert.assertNotNull;

@SpringBootTest(classes = TestApplication.class)
class SpeedyFactoryTest {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    SpeedyFactory speedyFactory;

    @Autowired
    JpaMetaModel jpaMetamodel;

    @BeforeEach
    void setUp() {
    }

    @Test
    void requestResource() {
        assertNotNull(jpaMetamodel.getEntityMetadata("Customer"));

    }
}