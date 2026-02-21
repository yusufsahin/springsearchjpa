package com.innogon.springsearchjpa;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "book")
public interface BookRepository extends CrudRepository<Book, Long>, JpaSpecificationExecutor<Book> {
}
