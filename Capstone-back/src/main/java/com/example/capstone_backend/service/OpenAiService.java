package com.example.capstone_backend.service;

import com.example.capstone_backend.dto.AiAnalysisRequest;
import com.example.capstone_backend.dto.AiAnalysisResponse;
import com.example.capstone_backend.dto.ParsedRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AiAnalysisResponse analyzeParts(AiAnalysisRequest request) {
        String partsList = request.getParts().stream()
                .map(p -> p.getCategory() + ": " + p.getName())
                .collect(Collectors.joining(", "));

        String prompt = String.format(
                "너는 하이엔드 PC 하드웨어 전문가야. 다음 부품 조합을 사용자 친화적으로 분석해줘.\n" +
                        "부품 리스트: [%s]\n\n" +
                        "가이드라인:\n" +
                        "1. 'pros'(장점)에는 부품 간의 성능 시너지, 선택한 용도에서의 강점, 가성비를 구체적으로 2개 적어줘.\n" +
                        "2. 'cons'(아쉬운 점)에는 발생 가능한 병목 현상, 부품 급 차이로 인한 불균형을 날카롭게 분석해서 2개 적어줘.\n" +
                        "3. 모든 문장은 반드시 20자 이상의 한국어 완성형 문장으로 작성해.\n" +
                        "4. **중요**: 다른 설명 없이 오직 아래 JSON 형식으로만 응답해.\n" +
                        "{\"pros\": [\"...\", \"...\"], \"cons\": [\"...\", \"...\"]}",
                partsList
        );

        return callGeminiApi(prompt, AiAnalysisResponse.class);
    }

    /**
     * 사용자의 자연어 입력에서 예산과 용도를 추출합니다.
     */
    public ParsedRequest parseUserInput(String userInput) {
        String prompt = String.format(
                "너는 사용자의 요구사항에서 PC 견적 조건을 추출하는 분석기야.\n" +
                        "사용자 입력: \"%s\"\n\n" +
                        "가이드라인:\n" +
                        "1. 'usage'는 반드시 다음 중 하나로만 매핑해: [GAMING, VIDEO_EDITING, MACHINE_LEARNING, OFFICE, CONTENT_CREATION, PROGRAMMING]\n" +
                        "2. 'budget'은 사용자가 언급한 금액을 '원' 단위 숫자로 변환해. (예: 150만원 -> 1500000)\n" +
                        "3. 만약 예산이나 용도가 명확하지 않으면 가장 유사한 것으로 추측해.\n" +
                        "4. **중요**: 설명 없이 오직 아래 JSON 형식으로만 응답해.\n" +
                        "{\"budget\": 1500000, \"usage\": \"GAMING\"}",
                userInput
        );

        ParsedRequest result = callGeminiApi(prompt, ParsedRequest.class);

        // 예외 발생으로 null이 반환될 경우를 대비한 기본값 설정
        if (result == null) {
            result = new ParsedRequest();
            result.setBudget(1000000L);
            result.setUsage("GAMING");
        }
        return result;
    }

    /**
     * 공통 Gemini API 호출 로직
     */
    private <T> T callGeminiApi(String prompt, Class<T> responseType) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey;

            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> contentsPart = Map.of("parts", List.of(textPart));
            Map<String, Object> requestBody = Map.of("contents", List.of(contentsPart));

            ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);

            String rawText = objectMapper.readTree(response.getBody())
                    .path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            String cleanJson = rawText.substring(rawText.indexOf("{"), rawText.lastIndexOf("}") + 1);

            return objectMapper.readValue(cleanJson, responseType);
        } catch (Exception e) {
            System.err.println("Gemini API 호출 실패: " + e.getMessage());
            return null;
        }
    }
}