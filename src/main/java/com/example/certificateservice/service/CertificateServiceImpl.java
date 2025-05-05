package com.example.certificateservice.service;

import com.example.certificateservice.dto.CertificateRequest;
import com.example.certificateservice.dto.CertificateResponse;
import com.example.certificateservice.exception.ResourceNotFoundException;
import com.example.certificateservice.model.Certificate;
import com.example.certificateservice.repository.CertificateRepository;
import com.example.certificateservice.util.PdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository repository;
    private final RestTemplate restTemplate;
    private final String CONTENT_SERVICE_URL = "http://localhost:8080/contentservice/api/lessons";

    @Override
    public CertificateResponse generateCertificate(CertificateRequest request) {
        // 1. Get enrollment info
        String enrollmentUrl = "http://localhost:8080/enrollmentservice/api/enrollments/" + request.getEnrollmentId();
        Map<String, Object> enrollment = restTemplate.getForObject(enrollmentUrl, Map.class);
        if (enrollment == null || !enrollment.containsKey("userId") || !enrollment.containsKey("courseId")) {
            throw new IllegalStateException("Invalid enrollment response");
        }

        Long userId = Long.valueOf(enrollment.get("userId").toString());
        Long courseId = Long.valueOf(enrollment.get("courseId").toString());


        // 2. Check if certificate already exists
        Optional<Certificate> existing = repository.findByUserIdAndCourseId(userId, courseId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Certificate already exists.");
        }

        // 3. Check course completion
        String checkUrl = "http://localhost:8080/contentservice/api/lessons/course/" + courseId + "/user/" + userId + "/is-complete";
        Boolean isCompleted = restTemplate.getForObject(checkUrl, Boolean.class);
        if (isCompleted == null || !isCompleted) {
            throw new IllegalStateException("Course is not completed.");
        }

        // 4. Get userName & courseTitle
        String userName = restTemplate.getForObject("http://localhost:8080/userservice/user/" + userId + "/name", String.class);
        String courseTitle = restTemplate.getForObject("http://localhost:8080/courseservice/courses/" + courseId + "/title", String.class);

        // 5. Generate PDF
        String fileName = "cert_" + userId + "_" + courseId + ".pdf";
        String filePath = "certificates/" + fileName;
        PdfGenerator.generate(userName, courseTitle, filePath);

        Certificate cert = new Certificate();
        cert.setUserId(userId);
        cert.setCourseId(courseId);
        cert.setCourseTitle(courseTitle);
        cert.setUserName(userName);
        cert.setIssueDate(LocalDate.now());
        cert.setFilePath(filePath);

        Certificate saved = repository.save(cert);

        return CertificateResponse.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .courseId(saved.getCourseId())
                .courseTitle(saved.getCourseTitle())
                .userName(saved.getUserName())
                .issueDate(saved.getIssueDate())
                .filePath(saved.getFilePath())
                .build();
    }



    @Override
    public List<CertificateResponse> getCertificatesByUser(Long userId) {
        return repository.findByUserId(userId).stream().map(cert -> CertificateResponse.builder()
                .id(cert.getId())
                .userId(cert.getUserId())
                .courseId(cert.getCourseId())
                .courseTitle(cert.getCourseTitle())
                .userName(cert.getUserName())
                .issueDate(cert.getIssueDate())
                .filePath(cert.getFilePath())
                .build()).collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<Resource> downloadCertificate(Long id) {
        Certificate cert = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found"));

        File file = new File(cert.getFilePath());
        if (!file.exists()) {
            throw new ResourceNotFoundException("PDF file not found");
        }

        try {
            Path path = file.toPath();
            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Download failed: " + e.getMessage());
        }
    }
}
