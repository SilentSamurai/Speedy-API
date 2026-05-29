# Getting Started with Speedy

Speedy is supported with Spring boot 2.1 and above.

### Maven Dependency

Speedy spring boot auto configurations

```xml
<dependency>
    <groupId>com.github.silentsamurai</groupId>
    <artifactId>spring-boot-starter-speedy-api</artifactId>
    <version>3.1.4</version>
</dependency>
```

include the jpa implementation of the Speedy System

```xml
<dependency>
    <groupId>com.github.silentsamurai</groupId>
    <artifactId>speedy-jpa-impl</artifactId>
    <version>3.1.4</version>
</dependency>
```

### Speedy Config

speedy system won't be activated unless this config (ISpeedyConfiguration) is created.

speedy needs entity manager and meta-model processor

```java
@Configuration
public class SpeedyConfig implements ISpeedyConfiguration {

    private final EntityManagerFactory entityManagerFactory;
    private final SpeedyValidation speedyValidation;
    private final EntityEvents entityEvents;
    private final DataSource dataSource;
    private final Environment environment;

    public SpeedyConfig(EntityManagerFactory entityManagerFactory, 
                        SpeedyValidation speedyValidation, 
                        EntityEvents entityEvents, 
                        DataSource dataSource, 
                        Environment environment) {
        this.entityManagerFactory = entityManagerFactory;
        this.speedyValidation = speedyValidation;
        this.entityEvents = entityEvents;
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Override
    public MetaModelProcessor metaModelProcessor() {
        return new JpaMetaModelProcessor(this, entityManagerFactory);
    }

    @Override
    public void register(ISpeedyRegistry registry) {
        registry.registerEventHandler(entityEvents)
                .registerValidator(speedyValidation);
    }

    @Override
    public DataSource dataSourcePerReq() {
        return dataSource;
    }

    @Override
    public SpeedyDialect getDialect() {
        Set<String> profiles = new HashSet<>(Arrays.asList(environment.getActiveProfiles()));
        if (profiles.contains("prod")) {
            return SpeedyDialect.POSTGRES;
        }
        return SpeedyDialect.H2;
    }
}

```

### Jpa Entity

Configure Jpa Entity so that speedy can retrieve the resource details

**User Entity**

```java
@Setter
@Getter
@Table(name = "users")
@Entity
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    protected UUID id;

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Column(name = "phone_no", nullable = false, length = 15)
    private String phoneNo;

    @Column(name = "email", nullable = false, length = 250)
    private String email;

    @Column(name = "type", nullable = false, length = 512)
    private String type;

    @SpeedyAction(ActionType.READ)
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @SpeedyAction(ActionType.READ)
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @SpeedyAction(ActionType.READ)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "last_login_at")
    private LocalDate lastLoginDate;

    @Column(name = "login_count")
    private Integer loginCount;
}
```

### Authentication & Authorization

Speedy-API is a library — it does **not** enforce authentication or per-user authorization. These responsibilities belong to the consuming application (e.g., Spring Security filters) that run before requests reach `/speedy/v1/**`.

#### Securing Endpoints with Spring Security

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/speedy/v1/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(GET, "/speedy/v1/$metadata").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
```

Replace `oauth2ResourceServer` with any authentication mechanism your application uses (basic auth, JWT, OAuth2, API keys, etc.).

#### `@SpeedyAction` — Static CRUD Gating

The `@SpeedyAction` annotation provides **static** per-entity and per-field CRUD gating enforced in the `SwitchHandler`:

```java
@SpeedyAction(ActionType.READ)  // field-level: only GET requests may read this field
@Column(name = "created_at")
private LocalDateTime createdAt;
```

- **Entity-level**: Place `@SpeedyAction` on the entity class to gate the entire entity.
- **Field-level**: Place it on a field to override or restrict access to that field.
- `ActionType.READ`, `CREATE`, `UPDATE`, `DELETE`, `ALL` control which HTTP verbs are allowed.
- This is a **static** check (same rules for all users). Per-user or role-based access control must be implemented in your own middleware.

#### Per-Field & Per-User Access Control

Because Speedy-API runs behind your security layer, you can:

1. Use Spring Security method security (`@PreAuthorize`) on your service layer.
2. Inject `Authentication` into a custom `ISpeedyEventHandler` or `ISpeedyCustomValidation` to enforce user-specific rules.
3. Apply field-level filtering in your own serialization layer if needed.

Speedy-API deliberately stays out of auth so you can use whatever security model fits your application.
