package vn.edu.hcmute.fakebook.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hcmute.fakebook.enums.OtpType;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // Bắt buộc phải có dòng này để dùng OtpToken.builder()
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String code;

    @Enumerated(EnumType.STRING)
    private OtpType type;

    private LocalDateTime expiredAt;
    private boolean used;
}