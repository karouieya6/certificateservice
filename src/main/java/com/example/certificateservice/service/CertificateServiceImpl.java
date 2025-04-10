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

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository repository;

    @Override
    public CertificateResponse generateCertificate(CertificateRequest request) {
        String fileName = "cert_" + request.getUserId() + "_" + request.getCourseId() + ".pdf";
        String filePath = "certificates/" + fileName;

        PdfGenerator.generate(request.getUserName(), request.getCourseTitle(), filePath);

        Certificate cert = new Certificate();
        cert.setUserId(request.getUserId());
        cert.setCourseId(request.getCourseId());
        cert.setCourseTitle(request.getCourseTitle());
        cert.setUserName(request.getUserName());
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
