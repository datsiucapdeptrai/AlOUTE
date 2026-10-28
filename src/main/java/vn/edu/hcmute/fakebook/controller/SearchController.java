package vn.edu.hcmute.fakebook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final UserAccountRepository userRepository;

    @GetMapping("/search")
    public String searchPage(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        if (keyword.trim().isEmpty()) {
            model.addAttribute("message", "Vui lòng nhập từ khóa để tìm kiếm!");
            return "search/index";
        }

        Page<UserAccount> userPage = userRepository.searchUsers(keyword.trim(), PageRequest.of(page, 10));
        
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        
        return "search/index";
    }
}