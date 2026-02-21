package com.innogon.springsearchjpa;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "publisher")
public interface PublisherRepository extends CrudRepository<Publisher, Long>, JpaSpecificationExecutor<Publisher> {
}
