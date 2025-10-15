package kopo.userservice.service;

public interface PatientManagerService {
    boolean addPatientToManager(String managerId, String patientId);
    void unlinkManagerFromPatient(String patientId, String managerId);
}
