package com.pm.patientservice.service;

import billing.BillingResponse;
import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    private final Logger log = LoggerFactory.getLogger(PatientService.class);
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;

    @Autowired
    public PatientService(PatientRepository patientRepository, BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
    }

    public List<PatientResponseDTO> getPatients() {
        List<Patient> patients = patientRepository.findAll();
        var ans = billingServiceGrpcClient.addTwoNumbers(9.0, 1.0);
        log.info("Result of add two num: {}", ans);
        return patients.stream().map(PatientMapper::toDTO).toList();
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        log.debug(patientRequestDTO.toString());
        this.checkForDuplicateEmail(patientRequestDTO.getEmail());
        Patient patient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));
        PatientResponseDTO patientResponseDTO = PatientMapper.toDTO(patient);
        BillingResponse billingResponse = billingServiceGrpcClient.createBillingAccount(patientResponseDTO.getId(), patientResponseDTO.getName(), patientResponseDTO.getEmail());
        kafkaProducer.sendEvent(patient);
        log.info("Result of create billing account: {}", billingResponse);
        return patientResponseDTO;
    }

    public PatientResponseDTO updatePatient(UUID patientId, PatientRequestDTO patientRequestDTO) {
        log.debug(patientRequestDTO.toString());

        Patient patient = patientRepository.findById(patientId).orElseThrow(() -> new PatientNotFoundException("Patient with id: " + patientId + " not found"));

        this.checkForDuplicateEmail(patientRequestDTO.getEmail(), patientId);

        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

        Patient updatedPatient = patientRepository.save(patient);

        return PatientMapper.toDTO(updatedPatient);

    }

    private void checkForDuplicateEmail(String email) {
        if (patientRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email Already Exists: " + email);
        }
    }

    private void checkForDuplicateEmail(String email, UUID patientId) {
        if (patientRepository.existsByEmailAndIdNot(email, patientId)) {
            throw new EmailAlreadyExistsException("Email Already Exists: " + email);
        }
    }

    public void deletePatient(UUID patientId) {
        patientRepository.deleteById(patientId);
    }


}
