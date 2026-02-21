# Spring Search JPA

A lightweight library that adds advanced search capabilities to Spring Boot JPA repositories. Define a query string in your API requests and get back JPA `Specification` objects — no boilerplate predicate code required.

## Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>com.innogon</groupId>
    <artifactId>springsearchjpa</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Annotate your controller parameter

```java
@GetMapping("/users")
public List<User> searchUsers(@SearchSpec Specification<User> spec) {
    return userRepository.findAll(spec);
}
```

### 3. Send a search query

```
GET /users?search=age>25 AND name:John
```

That's it. The library auto-configures itself via Spring Boot and resolves the `Specification` from the `search` query parameter.

## Query Syntax

### Comparison Operators

| Operator | Meaning                | Example                      |
|----------|------------------------|------------------------------|
| `:`      | Equals                 | `name:John`                  |
| `!`      | Not equals             | `status!INACTIVE`            |
| `>`      | Greater than           | `age>25`                     |
| `<`      | Less than              | `price<100`                  |
| `>:`     | Greater than or equal  | `score>:90`                  |
| `<:`     | Less than or equal     | `score<:50`                  |

### Wildcard Matching (String Fields)

| Pattern    | Meaning           | Example                  |
|------------|-------------------|--------------------------|
| `*value`   | Ends with         | `name:*son`              |
| `value*`   | Starts with       | `name:John*`             |
| `*value*`  | Contains          | `name:*oh*`              |
| `!*value`  | Doesn't end with  | `name!*son`              |
| `!value*`  | Doesn't start with| `name!John*`             |
| `!*value*` | Doesn't contain   | `name!*oh*`              |

### Logical Operators

| Operator | Example                                  |
|----------|------------------------------------------|
| `AND`    | `name:John AND age>25`                   |
| `OR`     | `status:ACTIVE OR role:ADMIN`            |
| `()`     | `(name:John OR name:Jane) AND age>25`    |

### Range & Set Operations

| Operation     | Example                                         |
|---------------|--------------------------------------------------|
| `BETWEEN`     | `age BETWEEN 18 AND 65`                          |
| `NOT BETWEEN` | `price NOT BETWEEN 100 AND 200`                  |
| `IN`          | `status IN [ACTIVE,PENDING]`                     |
| `NOT IN`      | `role NOT IN [GUEST,BANNED]`                     |

### Null & Empty Checks

| Operation      | Example                | Applies To         |
|----------------|------------------------|---------------------|
| `IS NULL`      | `email IS NULL`        | Any field           |
| `IS NOT NULL`  | `email IS NOT NULL`    | Any field           |
| `IS EMPTY`     | `books IS EMPTY`       | Collection fields   |
| `IS NOT EMPTY` | `books IS NOT EMPTY`   | Collection fields   |

### String Values

Use single or double quotes for values that contain spaces or special characters:

```
name:'John Doe'
city:"New York"
description:'it\'s escaped'
```

## Supported Field Types

| Type            | Comparison (`>`, `<`, `>:`, `<:`) | Equality (`:`, `!`) | `IN` / `NOT IN` | `IS NULL` | `BETWEEN` |
|-----------------|:---------------------------------:|:-------------------:|:----------------:|:---------:|:---------:|
| `String`        | Yes                               | Yes + wildcards     | Yes              | Yes       | Yes       |
| `Integer`/`int` | Yes                               | Yes                 | Yes              | Yes       | Yes       |
| `Long`/`long`   | Yes                               | Yes                 | Yes              | Yes       | Yes       |
| `Double`/`double`| Yes                              | Yes                 | Yes              | Yes       | Yes       |
| `Float`/`float` | Yes                               | Yes                 | Yes              | Yes       | Yes       |
| `Short`/`short` | Yes                               | Yes                 | Yes              | Yes       | Yes       |
| `Byte`/`byte`   | Yes                               | Yes                 | Yes              | Yes       | Yes       |
| `BigDecimal`    | Yes                               | Yes                 | Yes              | Yes       | Yes       |
| `Boolean`       | —                                 | Yes                 | Yes              | Yes       | —         |
| `Enum`          | —                                 | Yes (case-insensitive) | Yes           | Yes       | —         |
| `Date`          | Yes                               | Yes                 | Yes              | Yes       | Yes       |
| `LocalDate`     | Yes                               | Yes                 | Yes              | Yes       | Yes       |
| `LocalDateTime` | Yes                               | Yes                 | Yes              | Yes       | Yes       |
| `LocalTime`     | Yes                               | Yes                 | Yes              | Yes       | Yes       |
| `Instant`       | Yes                               | Yes                 | Yes              | Yes       | Yes       |
| `Duration`      | Yes                               | Yes                 | Yes              | Yes       | Yes       |
| `UUID`          | —                                 | Yes                 | Yes              | Yes       | —         |
| `Collection`    | —                                 | —                   | —                | Yes       | —         |

## `@SearchSpec` Annotation Options

```java
@GetMapping("/users")
public List<User> search(
    @SearchSpec(
        searchParam = "q",               // query parameter name (default: "search")
        caseSensitiveFlag = false,        // case-insensitive string matching
        blackListedFields = {"password"}, // fields that cannot be searched
        whiteListedFields = {"name","email"}, // only these fields can be searched
        required = true,                  // 400 if search param is missing
        defaultValue = "active:true",     // fallback when param is empty
        maxLength = 512,                   // max query length (default: 1024)
        collectionJoinStrategy = CollectionJoinStrategy.EXISTS  // EXISTS subqueries for collections
    )
    Specification<User> spec
) {
    return userRepository.findAll(spec);
}
```

| Attribute                | Default        | Description                                                                 |
|--------------------------|----------------|-----------------------------------------------------------------------------|
| `searchParam`            | `"search"`     | Name of the HTTP query parameter                                           |
| `caseSensitiveFlag`      | `true`         | When `false`, string comparisons are case-insensitive                       |
| `blackListedFields`      | `{}`           | Fields excluded from search                                                 |
| `whiteListedFields`      | `{}`           | When non-empty, only these fields are allowed (takes precedence over blacklist) |
| `required`               | `false`        | When `true`, a missing/empty search param returns 400                       |
| `defaultValue`           | `""`           | Default query when search param is empty                                    |
| `maxLength`              | `1024`         | Maximum allowed query string length                                         |
| `collectionJoinStrategy` | `SHARED_JOIN`  | How collection associations are joined (see [Join Strategies](#join-strategies)) |

## End-to-End Example

A complete example showing how a search request flows through all layers: **Model -> Repository -> Service -> Controller -> HTTP**.

### Model

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String category;

    private BigDecimal price;

    private Boolean active;

    @Enumerated(EnumType.STRING)
    private ProductStatus status; // DRAFT, PUBLISHED, ARCHIVED

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @OneToMany(mappedBy = "product")
    private List<Review> reviews = new ArrayList<>();

    // getters & setters
}

@Entity
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String country;

    // getters & setters
}
```

### Repository

```java
public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
}
```

`JpaSpecificationExecutor<Product>` is the only thing needed. No custom query methods required.

### Service

```java
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<Product> search(Specification<Product> spec, Pageable pageable) {
        return productRepository.findAll(spec, pageable);
    }
}
```

### Controller

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Page<Product> search(
            @SearchSpec(
                searchParam = "q",
                caseSensitiveFlag = false,
                blackListedFields = {"reviews"}
            )
            Specification<Product> spec,
            Pageable pageable
    ) {
        return productService.search(spec, pageable);
    }
}
```

### Example HTTP Requests

```bash
# Find active products priced between 10 and 100
GET /api/products?q=active:true AND price BETWEEN 10 AND 100

# Find published products from a specific brand (nested field)
GET /api/products?q=status:PUBLISHED AND brand.country:Germany

# Case-insensitive search with wildcards and pagination
GET /api/products?q=name:*phone* AND category:electronics&page=0&size=20&sort=price,asc

# Products created this year that are not archived
GET /api/products?q=createdAt>:2025-01-01T00:00:00 AND status!ARCHIVED

# Multiple values with IN
GET /api/products?q=category IN [electronics,clothing] AND price<:50

# Products without a brand assigned
GET /api/products?q=brand IS NULL

# Combine complex conditions with parentheses
GET /api/products?q=(status:PUBLISHED OR status:DRAFT) AND price>:10 AND brand.name:Samsung
```

### What Happens Under the Hood

```
HTTP Request
  │
  ▼
SearchSpecificationResolver          — reads @SearchSpec, extracts "q" param
  │
  ▼
CriteriaParser                       — ANTLR4 lexer/parser tokenizes the query
  │
  ▼
QueryVisitorImpl                     — walks the parse tree, builds SearchCriteria
  │
  ▼
SpecificationImpl.toPredicate()      — resolves field type, picks a ParsingStrategy
  │                                    (StringStrategy, ComparableStrategy, etc.)
  ▼
JPA Criteria API                     — Hibernate generates SQL
  │
  ▼
Database
```

## Nested Fields

Search through entity relationships using dot notation. All JPA association types are supported:

| Association     | Direction         | Example                                     |
|-----------------|-------------------|---------------------------------------------|
| `@ManyToOne`    | child → parent    | `GET /books?search=author.name:Tolkien`     |
| `@OneToMany`    | parent → children | `GET /authors?search=books.title:Hobbit`    |
| `@OneToOne`     | either direction  | `GET /authors?search=publisher.name:Penguin`|
| `@ManyToMany`   | either direction  | `GET /books?search=genres.name:Fantasy`     |

```
# ManyToOne — find books by author name
GET /books?search=author.name:Tolkien

# OneToMany — find authors who wrote a specific book
GET /authors?search=books.title:Hobbit

# OneToOne — find authors by publisher
GET /authors?search=publisher.name:HarperCollins

# ManyToMany — find books by genre
GET /books?search=genres.name:Fantasy

# Combine nested and direct fields
GET /authors?search=books.title:Hobbit AND name:Tolkien

# Wildcards work on nested fields too
GET /authors?search=books.title:*ring*

# IN operator with nested fields
GET /authors?search=books.title IN [Hobbit,Foundation]
```

### Join Strategies

Singular associations (`@ManyToOne`, `@OneToOne`) always use `INNER JOIN`. Collection associations (`@OneToMany`, `@ManyToMany`) depend on the configured strategy:

| Strategy       | SQL Pattern                     | Behavior                                                                 |
|----------------|---------------------------------|--------------------------------------------------------------------------|
| `SHARED_JOIN`  | `LEFT JOIN` + `DISTINCT`        | Default. AND'd predicates on the same collection apply to the **same row**. |
| `EXISTS`       | `EXISTS (SELECT 1 FROM ... WHERE ...)` | Better for pagination & large datasets. AND'd predicates are evaluated **independently**. |

```java
// Default: SHARED_JOIN — finds authors who have ONE book matching both conditions
@SearchSpec Specification<Author> spec
// Query: books.title:Hobbit AND books.title:Foundation → matches only if same book has both titles (unlikely)

// EXISTS — finds authors who have ANY book with Hobbit AND ANY book with Foundation
@SearchSpec(collectionJoinStrategy = CollectionJoinStrategy.EXISTS) Specification<Author> spec
// Query: books.title:Hobbit AND books.title:Foundation → matches if author has both books (separate rows)
```

Both whitelist and blacklist support root-level matching for nested paths. Blacklisting `author` also blocks `author.name`, `author.email`, etc.

## Programmatic Usage

You can also build specifications directly without the annotation:

```java
SearchSpec annotation = /* your SearchSpec instance */;
Specification<User> spec = new SpecificationsBuilder<User>(annotation)
        .withSearch("name:John AND age>25")
        .build();

List<User> results = userRepository.findAll(spec);
```

## Error Handling

The library provides two layers of error handling:

**1. Resolver layer** — catches errors during query parsing (syntax errors, blacklisted fields, max length):

| Error                          | HTTP Response |
|--------------------------------|---------------|
| Invalid query syntax           | 400           |
| Blacklisted / non-whitelisted field | 400      |
| Query exceeding `maxLength`    | 400           |
| Missing required search param  | 400           |

**2. Exception handler** — catches errors during query execution (`toPredicate()`), such as invalid field names or type mismatches:

| Error                                          | HTTP Response |
|------------------------------------------------|---------------|
| Non-existent field name (e.g., `books.xyz:v`)  | 400           |
| Navigation through basic field (e.g., `name.x:v`) | 400        |
| Unsupported operation for field type           | 400           |
| Value cannot be parsed to field type           | 400           |

The exception handler is auto-configured and can be disabled:

```properties
spring-search.exception-handler.enabled=false
```

When disabled (or when used programmatically without the resolver), the raw exceptions are thrown:

| Exception                  | Cause                                           |
|----------------------------|------------------------------------------------|
| `SearchQueryException`     | Malformed query syntax                          |
| `InvalidFieldException`    | Non-existent, blacklisted, or non-navigable field |
| `InvalidOperationException`| Unsupported operation for the field type        |
| `InvalidValueException`    | Value cannot be parsed to the field's type      |

All exceptions extend `SearchQueryException` (a `RuntimeException`), so you can catch them with a single type.

## Requirements

- Java 17+
- Spring Boot 3.x / 4.x
- Spring Data JPA

## License

See [LICENSE.txt](LICENSE.txt) for details.
