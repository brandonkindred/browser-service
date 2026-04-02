package com.looksee.models.repository;

import com.looksee.browsing.form.FormField;
import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 * Repository for {@link FormField}s
 */
public interface FormFieldRepository extends Neo4jRepository<FormField, Long> {

}
