# Exception Handling

### Overview

Speedy provides a built-in `DefaultExceptionMapper` that translates exceptions to HTTP status codes. You can **override
default mappings** or **add custom mappings** using `@SpeedyControllerAdvice` and `@SpeedyExceptionHandler` annotations.

### Built-in Resolution Order

When an exception is thrown during request processing, Speedy resolves the HTTP status in this order:

1. **Custom advice handlers** — `@SpeedyExceptionHandler` methods (highest priority)
2. **`SpeedyHttpException`** hierarchy — maps `BadRequestException` (400), `NotFoundException` (404),
   `InternalServerError` (500)
3. **Persistence exceptions** — Hibernate `ConstraintViolationException` / `DataException` → 400
4. **Jackson `JsonProcessingException`** → 400
5. **`IllegalArgumentException`** → 400
6. **Fallback** → 500

### Message Behavior

- **4xx responses**: The handler message or exception message is **exposed** to the client.
- **5xx responses**: The message is **masked** to `"Internal Server Error"`.

### Custom Exception Handling

#### Step 1: Define a Controller Advice

Create a class annotated with `@SpeedyControllerAdvice`. Add methods annotated with `@SpeedyExceptionHandler`:

```java
import com.github.silent.samurai.speedy.annotations.SpeedyControllerAdvice;
import com.github.silent.samurai.speedy.annotations.SpeedyExceptionHandler;
import org.springframework.stereotype.Component;

@Component
@SpeedyControllerAdvice
public class MyExceptionAdvice {

    @SpeedyExceptionHandler(value = IllegalArgumentException.class, status = 400)
    public String handleIllegalArgument(IllegalArgumentException e) {
        return "Invalid argument: " + e.getMessage();
    }

    @SpeedyExceptionHandler(value = RuntimeException.class, status = 500)
    public String handleRuntime() {
        return "Unexpected error occurred";
    }
}
```

Handler methods can optionally accept the thrown exception as a parameter. If the parameter is present, use it to craft
a dynamic message.

#### Step 2: Register the Advice

Register the advice bean in your `ISpeedyConfiguration.register()` method:

```java
@Configuration
public class SpeedyConfig implements ISpeedyConfiguration {

    @Autowired
    MyExceptionAdvice myExceptionAdvice;

    // ...

    @Override
    public void register(ISpeedyRegistry registry) {
        registry.registerControllerAdvice(myExceptionAdvice);
        registry.registerEventHandler(entityEvents);
        registry.registerValidator(speedyValidation);
    }
}
```

### Resolution Rules

Custom exception handlers resolve matches using these rules:

| Rule                     | Description                                                                                                               |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------|
| **Exact match**          | `@SpeedyExceptionHandler(value = NotFoundException.class)` matches exactly `NotFoundException`                            |
| **Superclass hierarchy** | If no exact match, Speedy walks up the superclass chain. A handler for `RuntimeException` catches all runtime subclasses. |
| **Cause chain**          | If the top-level exception has no match, Speedy walks the `getCause()` chain. A handler for the root cause will be used.  |

**Example:**

```java
@SpeedyControllerAdvice
public class GlobalAdvice {

    @SpeedyExceptionHandler(value = RuntimeException.class, status = 500)
    public String handleRuntime(RuntimeException e) {
        return "Server error: " + e.getMessage();
    }

    @SpeedyExceptionHandler(value = IllegalArgumentException.class, status = 400)
    public String handleIllegalArg(IllegalArgumentException e) {
        return "Bad request: " + e.getMessage();
    }

    @SpeedyExceptionHandler(value = NotFoundException.class, status = 404)
    public String handleNotFound(NotFoundException e) {
        return "Entity not found: " + e.getMessage();
    }
}
```

```java
// Matches IllegalArgumentException → 400
throw new IllegalArgumentException("field email is invalid");

// CustomException extends RuntimeException → matches via superclass → 500
throw new CustomException("something went wrong");

// Wrapped exception → walks cause chain to IllegalArgumentException → 400
Exception e = new Exception("wrapper", new IllegalArgumentException("root cause"));
```

### Response Format

All exceptions produce a JSON error response:

```json
{
    "status": 400,
    "message": "Bad request: field email is invalid",
    "timestamp": "2026-05-28T13:45:00"
}
```

Messages for 5xx responses are always masked:

```json
{
    "status": 500,
    "message": "Internal Server Error",
    "timestamp": "2026-05-28T13:45:00"
}
```
