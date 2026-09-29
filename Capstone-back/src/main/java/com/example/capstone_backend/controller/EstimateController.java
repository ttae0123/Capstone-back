package com.example.capstone_backend.controller;

import com.example.capstone_backend.dto.*;
import com.example.capstone_backend.service.EstimateService;
import com.example.capstone_backend.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/estimates")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class EstimateController {

    private final EstimateService estimateService;
    private final OpenAiService openAiService;


    // 1. 기존 DB 추천 API
    // 버튼 선택 방식
    @PostMapping("/recommend")
    public ResponseDTO.ResultList getRecommendation(
            @RequestBody RequestDTO.EstimateRequest request
    ) {

        return estimateService.generateRecommendations(
                request
        );
    }


    // 2. AI 분석 API
    // 추천 결과 장단점 분석
    @PostMapping("/analyze")
    public ResponseEntity<AiAnalysisResponse> analyzeEstimate(
            @RequestBody AiAnalysisRequest request
    ) {

        AiAnalysisResponse analysis =
                openAiService.analyzeParts(
                        request
                );

        return ResponseEntity.ok(
                analysis
        );
    }


    // 3. 자연어 입력 API
    // 채팅 방식
    @PostMapping("/natural-language")
    public ResponseEntity<Map<String, Object>>
    getRecommendationByNaturalLanguage(
            @RequestBody Map<String, String> payload
    ) {

        String userInput =
                payload.get("userInput");


        // ==========================================
        // 자연어에서 추천 조건 추출
        // ==========================================

        ParsedRequest parsed =
                openAiService.parseUserInput(
                        userInput
                );


        // ==========================================
        // DB 추천 요청 생성
        //
        // 자연어 입력에서는 현재 브랜드를
        // 별도로 추출하지 않으므로 빈 리스트 사용
        // ==========================================

        RequestDTO.EstimateRequest dbRequest =
                new RequestDTO.EstimateRequest(
                        parsed.getUsage(),
                        parsed.getBudget().longValue(),
                        List.of()
                );


        // ==========================================
        // 추천 알고리즘 실행
        // ==========================================

        ResponseDTO.ResultList results =
                estimateService.generateRecommendations(
                        dbRequest
                );


        // ==========================================
        // Response 구성
        // ==========================================

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "recommendations",
                results.recommendations()
        );

        response.put(
                "userBudget",
                parsed.getBudget()
        );


        return ResponseEntity.ok(
                response
        );
    }
}