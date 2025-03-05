# Getting Started with Speedy

Speedy is supported with Spring boot 2.1 and above.

### Maven Dependency

Speedy spring boot auto configurations

```xml
<dependency>
    <groupId>com.github.SilentSamurai</groupId>
    <artifactId>spring-boot-starter-speedy-api</artifactId>
    <version>3.0.1</version>
</dependency>
```

include the jpa implementation of the Speedy System

```xml
<dependency>
    <groupId>com.github.SilentSamurai</groupId>
    <artifactId>speedy-jpa-impl</artifactId>
    <version>3.0.1</version>
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
