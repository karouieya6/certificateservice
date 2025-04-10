package com.example.certificateservice.dto;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRequest {
    private Long userId;
    private Long courseId;
    private String courseTitle;
    private String userName;
}
