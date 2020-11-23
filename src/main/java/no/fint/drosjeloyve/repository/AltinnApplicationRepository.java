package no.fint.drosjeloyve.repository;

import no.fint.drosjeloyve.model.AltinnApplication;
import no.fint.drosjeloyve.model.AltinnApplicationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AltinnApplicationRepository extends MongoRepository<AltinnApplication, String> {
    List<AltinnApplication> findAllByStatus(AltinnApplicationStatus status);

    List<AltinnApplication> findAllByRequestorAndSubject(String requestor, String subject);

    @Query(value = "{}", fields = "{archiveReference: 0, caseId: 1, requestor: 1, requestorName: 1, subject: 1, subjectName: 1, status: 1, archivedDate: 1, updatedDate: 1}")
    List<AltinnApplication> findAllMinified();
}