package com.innogon.springsearchjpa;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "genre")
public interface GenreRepository extends CrudRepository<Genre, Long>, JpaSpecificationExecutor<Genre> {
}
