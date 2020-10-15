package no.fint.drosjeloyve.repository;

import no.fint.drosjeloyve.model.TaxiLicenseApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxiLicenseApplicationRepository extends MongoRepository<TaxiLicenseApplication, String> {
}