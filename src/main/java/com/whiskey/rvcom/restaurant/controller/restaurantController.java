package com.whiskey.rvcom.restaurant.controller;

import com.whiskey.rvcom.entity.restaurant.Restaurant;
import com.whiskey.rvcom.entity.restaurant.menu.Menu;
import com.whiskey.rvcom.entity.review.Review;
import com.whiskey.rvcom.restaurant.service.RestaurantService;
import com.whiskey.rvcom.review.ReviewService;
import com.whiskey.rvcom.util.ImagePathParser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/restaurant")
public class restaurantController {
    private final RestaurantService restaurantService;
    private final ReviewService reviewService;

    public restaurantController(RestaurantService restaurantService, ReviewService reviewService) {
        this.restaurantService = restaurantService;
        this.reviewService = reviewService;
    }

    @GetMapping("/{restaurantId}/{tab}")
    public String getRestaurantDetailWithTab(Model model, @PathVariable Long restaurantId, @PathVariable String tab) {
        // TODO: restaurantId와 tab에 따른 데이터 로딩 로직 구현
        // TODO: 모델에 restaurantId와 tab 정보 추가
        Map<String, Object> restaurantDetail = restaurantService.getRestaurantDetailById(restaurantId);

        Restaurant restaurant = (Restaurant) restaurantDetail.get("restaurant");

        model.addAttribute("restaurant", restaurant);
        model.addAttribute("restaurantId", restaurantId);

        if (restaurant.getCoverImage() != null) {
            model.addAttribute("restaurantCoverImageUrl", ImagePathParser.parse(restaurant.getCoverImage().getUuidFileName()));
        }

        model.addAttribute("tab", tab); // 저 탭과 관련한 내용은 PathVariable이 아닌 상수 info 로~

        model.addAttribute("status", restaurantDetail.get("status"));
        model.addAttribute("hours", restaurantDetail.get("hours"));

        model.addAttribute("monday", restaurantDetail.get("monday"));
        model.addAttribute("tuesday", restaurantDetail.get("tuesday"));
        model.addAttribute("wednesday", restaurantDetail.get("wednesday"));
        model.addAttribute("thursday", restaurantDetail.get("thursday"));
        model.addAttribute("friday", restaurantDetail.get("friday"));
        model.addAttribute("saturday", restaurantDetail.get("saturday"));
        model.addAttribute("sunday", restaurantDetail.get("sunday"));

        List<Menu> menuList = restaurantService.getMenuList(restaurantId);
        model.addAttribute("menuList", menuList);

        // 리뷰 관련 정보
        Map<String, Object> reviewAttributes = reviewService.getReviewsByRestaurant(restaurant);
        String ratingPhase = (String) reviewAttributes.get("ratingPhase");

        // 정규식을 사용하여 숫자와 소수점을 제거
        String onlyStars = ratingPhase.replaceAll("[0-9.]", "").trim();

        // 좋아요 개수 상위 2개의 리뷰 가져오기
        // reviewAttributes를 순회하여 좋아요 개수가 가장 많은 리뷰 2개를 가져옴
        List<Review> reviews = (List<Review>) reviewAttributes.get("reviews");
        reviews.sort((r1, r2) -> Integer.compare(r2.getLikes().size(), r1.getLikes().size()));
        List<Review> topReviews = reviews.stream().limit(2).collect(Collectors.toList());

        model.addAttribute("topReviews", topReviews);

        model.addAttribute("ratingOnlyStars", onlyStars);

        model.addAttribute("ratingPhase", reviewAttributes.get("ratingPhase"));
        model.addAttribute("reviews", reviewAttributes.get("reviews"));

        return "restaurantDetail";
    }
}
