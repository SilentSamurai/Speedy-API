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

    @Autowired
    EntityManagerFactory entityManagerFactory;

    // this is called for every speedy request
    @Override
    public EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    // this is called only on startup
    @Override
    public MetaModelProcessor createMetaModelProcessor() {
        return new JpaMetaModelProcessor(entityManagerFactory);
    }

    @Override
    public ISpeedyCustomValidation getCustomValidator() {
        return null;
    }
}
```

### Jpa Entity

Configure Jpa Entity so that speedy can retrieve the resource details

**Transaction Entity**

```java
@Getter
@Setter
@Entity
@Table(name = "transactions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Transaction {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id")
    protected String id;

    @NotNull
    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "note", length = 250)
    private String note;

    @Column(name = "comments", length = 1024)
    private String comments;

    @NotNull
    @Column(name = "transaction_type", length = 64, nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @NotNull
    @Column(name = "date", nullable = false)
    private Date date;


    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = true)
    private Category category;
}
```

**Category Entity**

```java
@Getter
@Setter
@Table(name = "categories", indexes = {
        @Index(name = "categories_name_key", columnList = "name", unique = true)
})
@Entity
public class Category {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id")
    protected String id;
    
    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @OneToMany(mappedBy = "category")
    private List<Transaction> transactions;

}
```

**Account Entity**

```java
@Getter
@Setter
@Entity
@Table(name = "accounts")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Account {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id")
    protected String id;

    @NotNull
    @Column(name = "name", length = 250)
    private String name;

    @NotNull
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Date createdAt;

    @OneToMany(mappedBy = "account")
    private List<Transaction> transactions;

}
```