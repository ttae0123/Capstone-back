package com.example.capstone_backend.controller;

import com.example.capstone_backend.dto.*;
import com.example.capstone_backend.service.EstimateService;
import com.example.capstone_backend.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/estimates")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") // 프론트엔드 서버 허용
public class EstimateController {

    private final EstimateService estimateService;
    private final OpenAiService openAiService;

    // 1. 기존 DB 추천 API (버튼 선택 방식)
    @PostMapping("/recommend")
    public ResponseDTO.ResultList getRecommendation(@RequestBody RequestDTO.EstimateRequest request) {
        return estimateService.generateRecommendations(request);
    }

    // 2. AI 분석 API (장단점 출력)
    @PostMapping("/analyze")
    public ResponseEntity<AiAnalysisResponse> analyzeEstimate(@RequestBody AiAnalysisRequest request) {
        AiAnalysisResponse analysis = openAiService.analyzeParts(request);
        return ResponseEntity.ok(analysis);
    }

    // 3. 자연어 입력 API (채팅 방식)
    @PostMapping("/natural-language")
    public ResponseEntity<Map<String, Object>> getRecommendationByNaturalLanguage(@RequestBody Map<String, String> payload) {
        String userInput = payload.get("userInput");

        // AI를 통해 자연어에서 조건 추출
        ParsedRequest parsed = openAiService.parseUserInput(userInput);

        // record 구조에 맞게 생성자 호출
        RequestDTO.EstimateRequest dbRequest = new RequestDTO.EstimateRequest(
                parsed.getUsage(),
                parsed.getBudget().longValue(),
                ""
        );

        ResponseDTO.ResultList results = estimateService.generateRecommendations(dbRequest);

        // [수정 포인트 2] Response 구성
        Map<String, Object> response = new HashMap<>();

        response.put("recommendations", results.recommendations());
        response.put("userBudget", parsed.getBudget()); // AI가 추출한 목표 예산

        return ResponseEntity.ok(response);
    }
}