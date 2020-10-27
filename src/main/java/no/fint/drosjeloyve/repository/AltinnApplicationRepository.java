package no.fint.drosjeloyve.repository;

import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.model.AltinnApplicationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AltinnApplicationRepository extends MongoRepository<AltinnApplication, String> {

    List<AltinnApplication> findAllByStatus(AltinnApplicationStatus status);

    Optional<AltinnApplication> findByRequestorAndSubject(String requestor, String subject);
}