package vn.edu.hcmute.fakebook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.service.AdminService;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final AdminService adminService;

    // =====================================================
    // DASHBOARD
    // =====================================================
    @GetMapping({"", "/"})
    public String showDashboard(Model model) {
        model.addAttribute("statistics", adminService.getDashboardStatistics());
        return "manager/index";
    }

    // =====================================================
    // QUẢN LÝ BÀI VIẾT
    // =====================================================
    @GetMapping("/posts")
    public String showPosts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        int safePage = Math.max(page, 0);
        model.addAttribute("postPage",
            adminService.getPosts(keyword, safePage, 10));
        model.addAttribute("keyword", keyword);
        return "manager/posts";
    }

    @PostMapping("/posts/{postId}/delete")
    public String deletePost(
            @PathVariable Long postId,
            RedirectAttributes redirectAttributes) {
        try {
            adminService.deletePost(postId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ẩn bài viết.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/manager/posts";
    }

    @PostMapping("/posts/{postId}/restore")
    public String restorePost(
            @PathVariable Long postId,
            RedirectAttributes redirectAttributes) {
        try {
            adminService.restorePost(postId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã khôi phục bài viết.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/manager/posts";
    }

    // =====================================================
    // QUẢN LÝ BÌNH LUẬN
    // =====================================================
    @GetMapping("/comments")
    public String showComments(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        int safePage = Math.max(page, 0);
        model.addAttribute("commentPage",
            adminService.getComments(keyword, safePage, 10));
        model.addAttribute("keyword", keyword);
        return "manager/comments";
    }

    @PostMapping("/comments/{commentId}/delete")
    public String deleteComment(
            @PathVariable Long commentId,
            RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteComment(commentId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ẩn bình luận.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/manager/comments";
    }

    @PostMapping("/comments/{commentId}/restore")
    public String restoreComment(
            @PathVariable Long commentId,
            RedirectAttributes redirectAttributes) {
        try {
            adminService.restoreComment(commentId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã khôi phục bình luận.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/manager/comments";
    }

    // =====================================================
    // QUẢN LÝ VIDEO
    // =====================================================
    @GetMapping("/videos")
    public String showVideos(
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        int safePage = Math.max(page, 0);
        model.addAttribute("videoPage", adminService.getVideos(safePage, 10));
        return "manager/videos";
    }
}