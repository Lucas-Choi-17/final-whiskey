package com.whiskey.rvcom.report.service;

import com.whiskey.rvcom.entity.report.ReviewReport;
import com.whiskey.rvcom.entity.review.Review;
import com.whiskey.rvcom.repository.ReviewReportRepository;
import com.whiskey.rvcom.repository.ReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class ReviewReportService {

    private final ModelMapper modelMapper;

    private final ReviewReportRepository reviewReportRepository;

    private final ReviewRepository reviewRepository;


    // 리뷰 신고 전체 조회
    public Page<ReviewReport> getAllReviewReports(int page, String sortOrder) {

        Sort sort = Sort.by("reportedAt");

        if ("desc".equalsIgnoreCase(sortOrder)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }

        Pageable pageable = PageRequest.of(page, 10, sort);
        return reviewReportRepository.findAllByIsCheckedFalse(pageable);
    }


    // 리뷰 신고 세부 조회
    public ReviewReport getReviewReport(Long id) {

        return reviewReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReviewReport not found with ID: " + id));
    }

    // 리뷰 신고 상태값 변경
    @Transactional
    public void reviewReportPunish(Long id, boolean isPunish) {

        ReviewReport reviewReport = reviewReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ReviewReport not found with ID: " + id));

        reviewReport.setChecked(true);

        if (isPunish) {
            reviewReport.setVisible(false);
        }

        reviewReportRepository.save(reviewReport);

        // 메일 발송 API 추후 추가 예정
    }


    // 리뷰 신고 등록
    @Transactional
    public void saveReviewReport(ReviewReport report) {

        ReviewReport reviewReport = modelMapper.map(report, ReviewReport.class);

        Review review = report.getReview();

        Long reviewId = review.getId();

        review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found with ID: " + reviewId));

        reviewReport.setReview(review);

        reviewReportRepository.save(reviewReport);
    }


    public Review returnReview(Long id) {

        return reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Review not found with ID: " + id));
    }
}
