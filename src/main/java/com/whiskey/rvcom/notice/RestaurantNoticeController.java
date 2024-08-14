package com.whiskey.rvcom.notice;

import com.whiskey.rvcom.notice.dto.RestaurantNoticeRequestDTO;
import com.whiskey.rvcom.notice.dto.RestaurantNoticeResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class RestaurantNoticeController {
    private final RestaurantNoticeService noticeService;

    // TODO: 해당 음식점의 공지사항 전체 조회
    @GetMapping("/restaurant/notice")
    public List<RestaurantNoticeResponseDTO> findNoticeByRestaurantId(Long restaurantId) {
        return noticeService.findNoticeByRestaurantId(restaurantId);
    }

    // TODO: 해당 음식점의 점주가 공지사항 작성
    @PostMapping("/restaurant/notice")
    public void save(RestaurantNoticeRequestDTO request, Long restaurantId) {
        noticeService.save(request, restaurantId);
    }

    // TODO: 해당 음식점의 점주가 공지사항 삭제
}
