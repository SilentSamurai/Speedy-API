# Validation Rules in Speedy

Speedy offers a lightweight, annotation-driven validation layer that is automatically
executed for every **CREATE** / **UPDATE** / **DELETE** request.  
Validation rules can be declared with either **Jakarta Bean Validation** annotations
or **Speedy-specific** annotations – both variants are mapped to the same internal
rules so you can choose the style that best suits your codebase.

> ⚠️ Server-side validation is performed **before** persistence; invalid payloads
> result in HTTP `400 Bad Request` with a descriptive error message.

---

## Quick Example

Jakarta

```java
@Column(name = "salary", nullable = false)
@Positive
private BigDecimal salary;
```

Speedy 

```java
@Column(name = "salary", nullable = false)
@SpeedyPositive
private BigDecimal salary;
```

If the client submits `salary <= 0` the request fails with:

```
HTTP 400  –  salary must be > 0
```

---

## Built-in Rules

| Category | Speedy Annotation | Jakarta Equivalent | Description |
|----------|------------------|--------------------|-------------|
| Numeric Bounds | `@SpeedyMin(long)` | `@Min` | Value must be `>= min` |
|                | `@SpeedyMax(long)` | `@Max` | Value must be `<= max` |
| Numeric Sign   | `@SpeedyPositive` | `@Positive` | Value `> 0` |
|                | `@SpeedyPositiveOrZero` | `@PositiveOrZero` | Value `>= 0` |
|                | `@SpeedyNegative` | `@Negative` | Value `< 0` |
|                | `@SpeedyNegativeOrZero` | `@NegativeOrZero` | Value `<= 0` |
| Decimal Bounds | `@SpeedyDecimalMin(value, inclusive)` | `@DecimalMin` | Decimal `>= / > value` |
|                | `@SpeedyDecimalMax(value, inclusive)` | `@DecimalMax` | Decimal `<= / < value` |
| Precision      | `@SpeedyDigits(integer, fraction)` | `@Digits` | Max digits before / after decimal |
| String Length  | `@SpeedyLength(min, max)` | `@Size` | Length between `min..max` |
| Pattern        | `@SpeedyRegex("regex")` | `@Pattern` | Must match regex |
| E-mail         | `@SpeedyEmail` | `@Email` | Must be valid email |
| URL            | `@SpeedyUrl`   | — | Must be valid URL |
| Date & Time    | `@SpeedyDateWithFormat(ISO_DATE)` | — | Value must match ISO format (DATE or DATE_TIME) |
|                | `@SpeedyFuture` | — | Date/time strictly in the future |
|                | `@SpeedyPast` | — | Date/time strictly in the past |
|                | `@SpeedyDateRange(min,max)` | — | Date within inclusive range |
| Blank Check    | `@SpeedyNotBlank` | `@NotBlank` | Not null / empty / whitespace |

> **Tip:** You can freely mix Speedy and Jakarta annotations – duplicates are
> ignored, only one instance of each rule is added.

---

## How Validation Works

1. **Meta-model processing** – At application startup Speedy scans JPA entities,
   discovers validation annotations and converts them to internal `FieldRule`s.
2. **Request handling** – When the endpoint receives a JSON payload it is
   converted to `SpeedyEntity` and validated *before* hitting the repository.
3. **Error aggregation** – All rule violations for a payload are aggregated and
   returned in a single response to reduce round-trips.

---

## Custom Validation

If you need more complex logic you can register a class implementing
`ISpeedyCustomValidation` and annotate its methods with `@SpeedyValidator`.
Custom validators run **in addition** to the built-in field rules.

```java
@Component
public class PersonValidation implements ISpeedyCustomValidation {

    @SpeedyValidator(entity = "AnnotatedPerson", requests = CREATE)
    public boolean checkAgeRange(AnnotatedPerson person) {
        return person.getAge() >= 18 && person.getAge() <= 60;
    }
}
```

See `docs/put-operation.md` for more information on custom validators.

---

## FAQ

**Q:** *Do I need to annotate with both Speedy and Jakarta?*  
**A:** No. Pick one. Both annotations resolve to the same rule – using both is
redundant but harmless.

**Q:** *Are validation rules also applied on DELETE?*  
**A:** Only for key fields (e.g. immutable primary keys). For DELETE operations
Speedy validates that key values are present and not empty.

**Q:** *Can I disable validation globally?*  
**A:** Not currently. Validation is a core safety feature of Speedy API.

---

Happy validating! 🎉
