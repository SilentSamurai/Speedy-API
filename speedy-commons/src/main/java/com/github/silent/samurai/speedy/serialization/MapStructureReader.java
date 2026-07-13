package com.github.silent.samurai.speedy.serialization;

import com.github.silent.samurai.speedy.exceptions.SpeedyHttpException;
import com.github.silent.samurai.speedy.interfaces.SpeedyValue;
import com.github.silent.samurai.speedy.interfaces.metadata.EntityMetadata;
import com.github.silent.samurai.speedy.interfaces.metadata.FieldMetadata;
import com.github.silent.samurai.speedy.interfaces.request.StructureReader;
import com.github.silent.samurai.speedy.models.SpeedyBoolean;
import com.github.silent.samurai.speedy.models.SpeedyDouble;
import com.github.silent.samurai.speedy.models.SpeedyInt;
import com.github.silent.samurai.speedy.models.SpeedyNull;
import com.github.silent.samurai.speedy.models.SpeedyText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Format-agnostic {@link StructureReader} over a plain Java value tree — {@link Map} is an
/// object, {@link List} is an array, anything else is a scalar (String / Number / Boolean /
/// {@link SpeedyValue} / {@code null}). Lets a walker built for wire formats ({@link
/// StructureToQuery}) also run against an already-in-memory spec, e.g. a policy
/// {@code QueryCondition}'s raw {@code Map<String, Object>}.
///
/// The tree is flattened once into a token list; the cursor obeys the same contract as the
/// streaming JSON reader (after any value is consumed the cursor rests on that value's last
/// token, so {@link #nextKey}/{@link #nextElement} simply step past it).
public class MapStructureReader implements StructureReader {

    private enum TkType {OBJ_START, OBJ_END, ARR_START, ARR_END, FIELD, SCALAR}

    private record Tk(TkType type, String name, Object value) {
    }

    private final List<Tk> tokens = new ArrayList<>();
    private int pos = -1;

    public MapStructureReader(Object root) {
        tokenize(root);
    }

    private void tokenize(Object node) {
        if (node instanceof Map<?, ?> map) {
            tokens.add(new Tk(TkType.OBJ_START, null, null));
            for (Map.Entry<?, ?> e : map.entrySet()) {
                tokens.add(new Tk(TkType.FIELD, String.valueOf(e.getKey()), null));
                tokenize(e.getValue());
            }
            tokens.add(new Tk(TkType.OBJ_END, null, null));
        } else if (node instanceof List<?> list) {
            tokens.add(new Tk(TkType.ARR_START, null, null));
            for (Object e : list) {
                tokenize(e);
            }
            tokens.add(new Tk(TkType.ARR_END, null, null));
        } else {
            tokens.add(new Tk(TkType.SCALAR, null, node));
        }
    }

    @Override
    public Kind begin() {
        pos = 0;
        return kindAt(pos);
    }

    @Override
    public Kind currentKind() {
        return kindAt(pos);
    }

    @Override
    public FieldMetadata nextField(EntityMetadata entityMetadata) throws SpeedyHttpException {
        String name;
        while ((name = nextKey()) != null) {
            if (entityMetadata.has(name)) {
                return entityMetadata.field(name);
            }
            skipValue();
        }
        return null;
    }

    @Override
    public String nextKey() {
        pos++;
        Tk t = tokens.get(pos);
        if (t.type() == TkType.OBJ_END) {
            return null;
        }
        String name = t.name(); // t is a FIELD
        pos++; // step onto the field's value token
        return name;
    }

    @Override
    public Kind nextElement() {
        pos++;
        if (tokens.get(pos).type() == TkType.ARR_END) {
            return null;
        }
        return kindAt(pos);
    }

    /// Decodes the current scalar. An already-{@link SpeedyValue} token passes through
    /// unchanged; otherwise the raw Java type (not {@code field}'s declared type) picks the
    /// wrapper, falling back to text for anything unrecognized — a condition spec's literals are
    /// hand-written Java values, not schema-validated wire input.
    @Override
    public SpeedyValue readField(FieldMetadata field) {
        Object v = tokens.get(pos).value();
        if (v == null) {
            return SpeedyNull.SPEEDY_NULL;
        }
        if (v instanceof SpeedyValue speedyValue) {
            return speedyValue;
        }
        if (v instanceof Boolean b) {
            return new SpeedyBoolean(b);
        }
        if (v instanceof Long l) {
            return new SpeedyInt(l);
        }
        if (v instanceof Integer i) {
            return new SpeedyInt(i.longValue());
        }
        if (v instanceof Double d) {
            return new SpeedyDouble(d);
        }
        if (v instanceof Float f) {
            return new SpeedyDouble(f.doubleValue());
        }
        return new SpeedyText(String.valueOf(v));
    }

    @Override
    public String textValue() {
        Tk t = tokens.get(pos);
        return (t.type() == TkType.SCALAR && t.value() instanceof String s) ? s : null;
    }

    @Override
    public int intValue() {
        Object v = tokens.get(pos).value();
        return v instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(v));
    }

    @Override
    public boolean boolValue() {
        Object v = tokens.get(pos).value();
        return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v));
    }

    @Override
    public boolean isBoolValue() {
        Tk t = tokens.get(pos);
        return t.type() == TkType.SCALAR && t.value() instanceof Boolean;
    }

    @Override
    public void skipValue() {
        TkType type = tokens.get(pos).type();
        if (type == TkType.OBJ_START || type == TkType.ARR_START) {
            int depth = 1;
            while (depth > 0) {
                pos++;
                TkType next = tokens.get(pos).type();
                if (next == TkType.OBJ_START || next == TkType.ARR_START) {
                    depth++;
                } else if (next == TkType.OBJ_END || next == TkType.ARR_END) {
                    depth--;
                }
            }
        }
        // scalar: no-op — the cursor stays on the scalar token, like the streaming JSON reader
    }

    @Override
    public void close() {
        // nothing to release
    }

    private Kind kindAt(int i) {
        Tk t = tokens.get(i);
        return switch (t.type()) {
            case OBJ_START -> Kind.OBJECT;
            case ARR_START -> Kind.ARRAY;
            case SCALAR -> t.value() == null ? Kind.NULL : Kind.VALUE;
            default -> throw new IllegalStateException("not positioned on a value token: " + t.type());
        };
    }
}
