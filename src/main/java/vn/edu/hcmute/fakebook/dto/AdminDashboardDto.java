package vn.edu.hcmute.fakebook.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminDashboardDto {

    private long totalUsers;
    private long totalPosts;
    private long totalVideos;
    private long totalComments;
    private long totalViews;
    private long totalShares;
}