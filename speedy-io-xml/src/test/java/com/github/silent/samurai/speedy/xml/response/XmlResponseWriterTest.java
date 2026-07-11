package com.github.silent.samurai.speedy.xml.response;

import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Covers the {@link XmlResponseWriter} context-stack invariant: pushing a nested
/// object/array always opens its parent first, so a lazily-tagged element can never end
/// up written as a sibling of the element it's meant to nest inside.
@ExtendWith(MockitoExtension.class)
class XmlResponseWriterTest {

    private FieldMetadata fieldOf(String owningEntityName, String outputName) {
        EntityMetadata entityMetadata = mock(EntityMetadata.class);
        when(entityMetadata.getName()).thenReturn(owningEntityName);
        FieldMetadata field = mock(FieldMetadata.class);
        when(field.getEntityMetadata()).thenReturn(entityMetadata);
        when(field.getOutputPropertyName()).thenReturn(outputName);
        return field;
    }

    private String finishAndCapture(XmlResponseWriter writer) throws Exception {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }

            @Override
            public void write(int b) throws IOException {
                captured.write(b);
            }
        });
        writer.finish(response, 200, Map.of(), "application/xml");
        return captured.toString(StandardCharsets.UTF_8);
    }

    @Test
    void associationAsFirstFieldNestsInsideParent() throws Exception {
        XmlResponseWriter writer = new XmlResponseWriter();

        writer.startArray();
        writer.startObject();
        writer.field(fieldOf("Product", "category"));
        writer.startObject();
        writer.field("id");
        writer.writeText("9");
        writer.endObject();
        writer.field("name");
        writer.writeText("Widget");
        writer.endObject();
        writer.endArray();

        String xml = finishAndCapture(writer);
        assertTrue(xml.contains("<Product><category><id>9</id></category><name>Widget</name></Product>"), xml);
    }

    @Test
    void emptyNestedObjectStillNestsInsideParent() throws Exception {
        XmlResponseWriter writer = new XmlResponseWriter();

        writer.startArray();
        writer.startObject();
        writer.field(fieldOf("Product", "category"));
        writer.startObject(); // pushed but closed again without ever writing a field
        writer.endObject();
        writer.field("name");
        writer.writeText("Widget");
        writer.endObject();
        writer.endArray();

        String xml = finishAndCapture(writer);
        assertTrue(xml.contains("<Product><category/><name>Widget</name></Product>"), xml);
    }
}
