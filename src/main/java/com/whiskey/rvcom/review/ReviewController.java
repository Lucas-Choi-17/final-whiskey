package com.whiskey.rvcom.review;

import com.whiskey.libs.file.FileNameGroup;
import com.whiskey.libs.file.FileUploader;
import com.whiskey.rvcom.ImageFile.ImageFileService;
import com.whiskey.rvcom.entity.member.Member;
import com.whiskey.rvcom.entity.resource.ImageFile;
import com.whiskey.rvcom.entity.restaurant.Restaurant;
import com.whiskey.rvcom.entity.review.*;
import com.whiskey.rvcom.repository.MemberRepository;
import com.whiskey.rvcom.restaurant.service.RestaurantService;
import com.whiskey.rvcom.review.dto.ReviewDTO;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final RestaurantService restaurantService;
//    private final RestaurantRepository restaurantRepository;    // need. 서비스 모듈로 교체 필요(업요전달)

    private final ReviewService reviewService;
    private final ReviewCommentService reviewCommentService;
    private final ReviewLikeService reviewLikeService;
    private final ReviewImageService reviewImageService;
    private final MemberRepository memberRepository;
    private final ReceiptService receiptService;

    @Autowired
    private ImageFileService imageFileService;

    // use path variable
    // 리뷰 목록 조회 요청
//    @GetMapping("/{restaurantNo}")
//    public String getReviewsByRestaurantId(@PathVariable Long restaurantNo, Model model) {
//        Restaurant restaurant = restaurantRepository.findById(restaurantNo).orElseThrow();
//        List<Review> reviews = reviewService.getReviewsByRestaurant(restaurant);
//
//        model.addAttribute("reviews", reviews);     // desc. 리뷰 목록 바인딩
//
//        return "restaurantDetail";  // need. 뷰 분할 후 리뷰 페이지에 대한 뷰로 변경
//    }

    //    @PostMapping("/list/{restaurantNo}")
    public String getReviewCommentsByReviewId(@PathVariable Long reviewNo, Model model) {
        // todo. 리뷰 아이디로 리뷰 댓글 목록 조회
        Review dest = reviewService.getReviewById(reviewNo);
        List<ReviewComment> comments = reviewCommentService.getCommentsForReview(dest);

        model.addAttribute("comments", comments);     // desc. 리뷰 댓글 목록 바인딩
        return "restaurantDetail";
    }

    // 리뷰 댓글 작성 요청
    @PostMapping("/comment")
    public String saveReviewComment(Long reviewNo, String content) { // need. 리뷰 작성자 세션 정보 추가 필요
        ReviewComment reviewComment = new ReviewComment();
        reviewComment.setCommenter(null);   // block. 로그인한 사용자 정보로 대체
        reviewComment.setContent(content);
        reviewComment.setReview(reviewService.getReviewById(reviewNo));

        return "redirect:/";
    }

    // 리뷰 댓글 삭제 요청
    @PostMapping("/comment/remove")
    public String removeReviewComment(Long commentNo) {
        ReviewComment dest = reviewCommentService.getCommentById(commentNo);
        reviewCommentService.removeComment(dest);

        return "redirect:/";
    }

    // 리뷰 좋아요 추가 요청
    @PostMapping("/reviewlike/add")
    @ResponseBody
    public ResponseEntity<Long> addLikeToReview(@RequestParam("reviewId") Long reviewNo) { // need. 좋아요 처리할 사용자 정보 추가 필요
        Review dest = reviewService.getReviewById(reviewNo);

        Member member = new Member();  // block. 로그인한 사용자 정보로 대체
        member.setId(43L); // 임시로 아무 ID나 넣음
        ReviewLike reviewLike = reviewLikeService.getReviewLikeByReviewAndMember(dest, member);

        // 이미 해당 리뷰에 좋아요를 누른 경우 좋아요 취소(토글처리)
        if (reviewLike == null) {
            reviewLike = new ReviewLike();
            reviewLike.setReview(dest);
            reviewLike.setMember(member);
            reviewLikeService.addReviewLike(reviewLike);
        } else {
            reviewLikeService.removeReviewLike(reviewLike);
        }

        return ResponseEntity.ok(reviewLikeService.getReviewLikeCount(dest));
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<?> saveReview(@ModelAttribute ReviewDTO reviewDTO,
                                        @RequestParam("images") List<MultipartFile> images,
                                        HttpSession session) {
        log.info("Received request to save review: {}", reviewDTO);
        try {
            logger.info("Received reviewDTO: {}", reviewDTO);
            // receiptDataId 확인
            if (reviewDTO.getReceiptDataId() == 0) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "영수증 데이터 ID가 유효하지 않습니다."));
            }
            // 세션에서 사용자 정보 가져오기
            Member member = (Member) session.getAttribute("member");
            if (member == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "로그인이 필요합니다."));
            }

            // images에서 항목을 하나씩 꺼내와 파일을 ncp에 업로드 수행
            List<FileNameGroup> uploadedFiles = new ArrayList<>();
            for (MultipartFile image : images) {
                FileNameGroup fileNameGroup = fileUploader.upload(image);
                uploadedFiles.add(fileNameGroup);
            }

            // FileUpload 클래스로 업로드를 하면 FileNameGroup이 하나씩 반환된다.
            // 1. 업로드 수행 후 파일 이름 객체를 가져와 saveFileName에 저장
            List<String> savedFileNames = uploadedFiles.stream()
                    .map(FileNameGroup::getSaveFileName)
                    .collect(Collectors.toList());

            // 2. 업로드 수행 후 파일 이름 객체를 가지고 ImageFile 타입의 엔티티를 각각 생성하여 데이터베이스에 저장
            List<ImageFile> imageFiles = new ArrayList<>();
            for (FileNameGroup fileNameGroup : uploadedFiles) {
                ImageFile imageFile = new ImageFile();
                imageFile.setOriginalFileName(fileNameGroup.getOriginalFileName());
                imageFile.setUuidFileName(fileNameGroup.getUuidFileName());
                imageFiles.add(imageFileService.saveImageFile(imageFile));
            }

            // 3. 저장된 ImageFile 엔티티를 ReviewImage 엔티티 리스트로만 만듦
            List<ReviewImage> reviewImages = imageFiles.stream()
                    .map(imageFile -> {
                        ReviewImage reviewImage = new ReviewImage();
                        reviewImage.setImageFile(imageFile);
                        return reviewImage;
                    })
                    .collect(Collectors.toList());

            // 리뷰 엔티티 객체 생성
            Review review = new Review();
            review.setRating(reviewDTO.getRating());
            review.setContent(reviewDTO.getContent());
            review.setReviewer(member);
            review.setRestaurant(restaurantService.getRestaurantById(reviewDTO.getRestaurantId()));
            review.setReceiptData(receiptService.getReceipt(reviewDTO.getReceiptDataId()));
            review.setReviewImages(reviewImages);

            // 리뷰 저장 로직 호출
            // Review savedReview = reviewService.saveReview(reviewDTO, images, member);
            Review savedReview = reviewService.saveReview(new Review());

            // 리다이렉트 URL 생성
            String redirectUrl = "/restaurant/" + savedReview.getRestaurant().getId() + "#reviews";

            return ResponseEntity.ok(Map.of("success", true, "redirectUrl", redirectUrl));
        } catch (Exception e) {
            logger.error("리뷰 저장 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

}
