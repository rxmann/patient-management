package com.pm.patientservice.repository;

import com.pm.patientservice.model.Patient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    boolean existsByEmail(@Valid @NotBlank String email);

    boolean existsByEmailAndIdNot(@Valid @NotBlank String email, UUID id);

}
