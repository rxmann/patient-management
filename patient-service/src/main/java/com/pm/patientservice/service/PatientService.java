package com.pm.patientservice.service;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientResponseDTO> getPatients () {
        List<Patient> patients = patientRepository.findAll();
        return patients.stream().map(PatientMapper::toDTO).toList();
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        logger.debug(patientRequestDTO.toString());
        this.checkForDuplicateEmail(patientRequestDTO.getEmail());
        Patient patient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));
        return PatientMapper.toDTO(patient);
    }

    public PatientResponseDTO updatePatient(UUID patientId, PatientRequestDTO patientRequestDTO) {
        logger.debug(patientRequestDTO.toString());

        Patient patient = patientRepository.findById(patientId).orElseThrow(() -> new PatientNotFoundException("Patient with id: " + patientId + " not found"));

        this.checkForDuplicateEmail(patientRequestDTO.getEmail(), patientId);

        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

        Patient updatedPatient = patientRepository.save(patient);

        return PatientMapper.toDTO(updatedPatient);

    }

    private void checkForDuplicateEmail (String email) {
        if (patientRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email Already Exists: " + email);
        }
    }

    private void checkForDuplicateEmail (String email, UUID patientId) {
        if (patientRepository.existsByEmailAndIdNot(email, patientId)) {
            throw new EmailAlreadyExistsException("Email Already Exists: " + email);
        }
    }

    public void deletePatient(UUID patientId) {
        patientRepository.deleteById(patientId);
    }


}
