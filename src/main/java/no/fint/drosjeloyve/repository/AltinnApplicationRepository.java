package no.fint.drosjeloyve.repository;

import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.model.AltinnApplicationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AltinnApplicationRepository extends MongoRepository<AltinnApplication, String> {

    List<AltinnApplication> findByStatus(AltinnApplicationStatus status);
}