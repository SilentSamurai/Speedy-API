package com.github.silent.samurai.speedy.handlers;

import com.github.silent.samurai.speedy.context.SpeedyContext;
import com.github.silent.samurai.speedy.enums.PermissionType;
import com.github.silent.samurai.speedy.exceptions.BadRequestException;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.query.BinaryCondition;
import com.github.silent.samurai.speedy.interfaces.query.BooleanCondition;
import com.github.silent.samurai.speedy.interfaces.query.QueryField;
import com.github.silent.samurai.speedy.interfaces.query.SpeedyQuery;
import com.github.silent.samurai.speedy.interfaces.request.SpeedyBody;
import com.github.silent.samurai.speedy.models.SpeedyText;
import com.github.silent.samurai.speedy.policy.PolicyEngine;
import com.github.silent.samurai.speedy.policy.SpeedyAuthContext;
import com.github.silent.samurai.speedy.policy.model.PolicyDocument;
import com.github.silent.samurai.speedy.policy.model.PolicyEffect;
import com.github.silent.samurai.speedy.policy.model.SpeedyPolicy;
import com.github.silent.samurai.speedy.policy.model.ResourceSelector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryFieldPolicyHandlerTest {

    @Test
    void associatedFilterUsesTheAssociatedEntityPolicy() {
        EntityMetadata category = metadata("Category");
        FieldMetadata categoryName = mock(FieldMetadata.class);
        when(categoryName.getEntityMetadata()).thenReturn(category);
        when(categoryName.getOutputPropertyName()).thenReturn("name");

        SpeedyContext context = contextFor(categoryName, policy("Product.name"));

        assertThrows(BadRequestException.class, () -> new QueryFieldPolicyHandler().process(context));
    }

    @Test
    void associatedFilterIsAllowedWhenItsEntityFieldIsGranted() {
        EntityMetadata category = metadata("Category");
        FieldMetadata categoryName = mock(FieldMetadata.class);
        when(categoryName.getEntityMetadata()).thenReturn(category);
        when(categoryName.getOutputPropertyName()).thenReturn("name");

        SpeedyContext context = contextFor(categoryName, policy("Category.name"));

        assertDoesNotThrow(() -> new QueryFieldPolicyHandler().process(context));
    }

    private static EntityMetadata metadata(String name) {
        EntityMetadata metadata = mock(EntityMetadata.class);
        when(metadata.getName()).thenReturn(name);
        return metadata;
    }

    private static SpeedyContext contextFor(FieldMetadata field, PolicyEngine policyEngine) {
        QueryField queryField = mock(QueryField.class);
        when(queryField.getMetadataForParsing()).thenReturn(field);
        BinaryCondition condition = mock(BinaryCondition.class);
        when(condition.getField()).thenReturn(queryField);

        BooleanCondition where = mock(BooleanCondition.class);
        when(where.getConditions()).thenReturn(List.of(condition));
        SpeedyQuery query = mock(SpeedyQuery.class);
        when(query.getWhere()).thenReturn(where);
        when(query.getOrderByList()).thenReturn(List.of());

        return new SpeedyContext()
                .put(PolicyEngine.class, policyEngine)
                .put(SpeedyBody.class, query);
    }

    private static PolicyEngine policy(String resource) {
        SpeedyPolicy allow = new SpeedyPolicy("allow", PolicyEffect.ALLOW, Set.of(PermissionType.READ),
                List.of(ResourceSelector.parse(resource)), List.of());
        return new PolicyEngine(new SpeedyAuthContext(
                new PolicyDocument(PolicyEffect.DENY, List.of(allow)),
                Map.of("principal.id", new SpeedyText("principal"))));
    }
}
