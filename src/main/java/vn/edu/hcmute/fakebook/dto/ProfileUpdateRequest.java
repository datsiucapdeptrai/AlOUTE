package vn.edu.hcmute.fakebook.dto;

import java.time.LocalDate;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;
@Data
public class ProfileUpdateRequest {
	private MultipartFile avatar;

	public MultipartFile getAvatar() { return avatar; }
	public void setAvatar(MultipartFile avatar) { this.avatar = avatar; }

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 120, message = "Họ tên không được quá 120 ký tự")
    private String fullName;

    @Size(max = 1000, message = "Giới thiệu không được quá 1000 ký tự")
    private String bio;

    @Pattern(
        regexp = "^$|^[0-9+().\\s-]{8,20}$",
        message = "Số điện thoại không hợp lệ"
    )
    private String phone;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @Size(max = 255, message = "Địa điểm không được quá 255 ký tự")
    private String location;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}