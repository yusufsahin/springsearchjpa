package com.innogon.springsearchjpa;

import com.innogon.springsearchjpa.annotation.SearchSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = SpringSearchApplication.class,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.data.rest.autoconfigure.DataRestAutoConfiguration"
)
@Import(SearchSpecificationResolverTest.TestSearchController.class)
@Transactional
class SearchSpecificationResolverTest {

    @RestController
    static class TestSearchController {

        @Autowired
        private AuthorRepository authorRepository;

        @GetMapping("/test/authors")
        public List<String> search(@SearchSpec Specification<Author> spec) {
            return authorRepository.findAll(spec).stream()
                    .map(Author::getName).toList();
        }

        @GetMapping("/test/authors/required")
        public List<String> searchRequired(
                @SearchSpec(required = true) Specification<Author> spec) {
            return authorRepository.findAll(spec).stream()
                    .map(Author::getName).toList();
        }

        @GetMapping("/test/authors/maxlength")
        public List<String> searchMaxLength(
                @SearchSpec(maxLength = 10) Specification<Author> spec) {
            return authorRepository.findAll(spec).stream()
                    .map(Author::getName).toList();
        }

        @GetMapping("/test/authors/blacklist")
        public List<String> searchBlacklist(
                @SearchSpec(blackListedFields = "books") Specification<Author> spec) {
            return authorRepository.findAll(spec).stream()
                    .map(Author::getName).toList();
        }

        @GetMapping("/test/authors/default")
        public List<String> searchDefault(
                @SearchSpec(defaultValue = "name:DefaultAuthor") Specification<Author> spec) {
            return authorRepository.findAll(spec).stream()
                    .map(Author::getName).toList();
        }
    }

    private MockMvc mockMvc;

    @Autowired
    private AuthorRepository authorRepository;

    @BeforeEach
    void setup(@Autowired WebApplicationContext wac) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // --- Resolver: valid search ---

    @Test
    void validSearchReturns200() throws Exception {
        authorRepository.save(createAuthor("Tolkien"));

        mockMvc.perform(get("/test/authors").accept(APPLICATION_JSON)
                        .param("search", "name:Tolkien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Tolkien"));
    }

    @Test
    void emptySearchReturnsAll() throws Exception {
        authorRepository.save(createAuthor("Tolkien"));
        authorRepository.save(createAuthor("Orwell"));

        mockMvc.perform(get("/test/authors").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // --- Resolver: required ---

    @Test
    void requiredMissingSearchReturns400() throws Exception {
        mockMvc.perform(get("/test/authors/required").accept(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiredWithValueReturns200() throws Exception {
        authorRepository.save(createAuthor("Tolkien"));

        mockMvc.perform(get("/test/authors/required").accept(APPLICATION_JSON)
                        .param("search", "name:Tolkien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // --- Resolver: maxLength ---

    @Test
    void maxLengthExceededReturns400() throws Exception {
        mockMvc.perform(get("/test/authors/maxlength").accept(APPLICATION_JSON)
                        .param("search", "name:ThisValueExceedsTenChars"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void maxLengthWithinLimitReturns200() throws Exception {
        authorRepository.save(createAuthor("Bob"));

        mockMvc.perform(get("/test/authors/maxlength").accept(APPLICATION_JSON)
                        .param("search", "name:Bob"))
                .andExpect(status().isOk());
    }

    // --- Resolver: defaultValue ---

    @Test
    void defaultValueAppliedWhenEmpty() throws Exception {
        authorRepository.save(createAuthor("DefaultAuthor"));
        authorRepository.save(createAuthor("Other"));

        mockMvc.perform(get("/test/authors/default").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("DefaultAuthor"));
    }

    // --- Resolver: invalid syntax ---

    @Test
    void invalidSyntaxReturns400() throws Exception {
        mockMvc.perform(get("/test/authors").accept(APPLICATION_JSON)
                        .param("search", "!!!invalid!!!"))
                .andExpect(status().isBadRequest());
    }

    // --- Resolver: blacklist ---

    @Test
    void blacklistedFieldReturns400() throws Exception {
        mockMvc.perform(get("/test/authors/blacklist").accept(APPLICATION_JSON)
                        .param("search", "books.title:Hobbit"))
                .andExpect(status().isBadRequest());
    }

    // --- Exception handler: toPredicate errors → 400 ---

    @Test
    void invalidFieldNameReturns400ViaExceptionHandler() throws Exception {
        mockMvc.perform(get("/test/authors").accept(APPLICATION_JSON)
                        .param("search", "books.nonExistentField:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void basicFieldNavigationReturns400ViaExceptionHandler() throws Exception {
        mockMvc.perform(get("/test/authors").accept(APPLICATION_JSON)
                        .param("search", "name.something:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void invalidIntermediateFieldReturns400ViaExceptionHandler() throws Exception {
        mockMvc.perform(get("/test/authors").accept(APPLICATION_JSON)
                        .param("search", "nonExistent.name:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- Helper ---

    private static Author createAuthor(String name) {
        Author author = new Author();
        author.setName(name);
        return author;
    }
}
