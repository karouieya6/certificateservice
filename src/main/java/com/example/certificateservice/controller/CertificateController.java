package com.example.certificateservice.controller;

import com.example.certificateservice.dto.CertificateRequest;
import com.example.certificateservice.dto.CertificateResponse;
import com.example.certificateservice.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping
    public ResponseEntity<CertificateResponse> generateCertificate(@RequestBody CertificateRequest request) {
        return ResponseEntity.ok(certificateService.generateCertificate(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CertificateResponse>> getCertificatesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(certificateService.getCertificatesByUser(userId));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadCertificate(@PathVariable Long id) {
        return certificateService.downloadCertificate(id);
    }
}
