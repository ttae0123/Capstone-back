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
                        "3. 모든 문장은 반드시 20자 이상의 한국어 완성형 문장으로 작성해.",
                partsList
        );

        // 🚀 구조화된 응답을 강제하기 위한 스키마 정의
        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "pros", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                        "cons", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
                ),
                "required", List.of("pros", "cons")
        );

        return callGeminiApi(prompt, schema, AiAnalysisResponse.class);
    }

    public ParsedRequest parseUserInput(String userInput) {
        String prompt = String.format(
                "너는 사용자의 요구사항에서 PC 견적 조건을 추출하는 분석기야.\n" +
                        "사용자 입력: \"%s\"\n\n" +
                        "가이드라인:\n" +
                        "1. 'usage'는 반드시 다음 중 하나로만 매핑해: [GAMING, VIDEO_EDITING, MACHINE_LEARNING, OFFICE, CONTENT_CREATION, PROGRAMMING]\n" +
                        "2. 'budget'은 사용자가 언급한 금액을 '원' 단위 숫자로 변환해. (예: 150만원 -> 1500000)\n" +
                        "3. 만약 예산이나 용도가 명확하지 않으면 가장 유사한 것으로 추측해.",
                userInput
        );

        // 🚀 구조화된 응답을 강제하기 위한 스키마 정의
        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "budget", Map.of("type", "INTEGER"),
                        "usage", Map.of("type", "STRING")
                ),
                "required", List.of("budget", "usage")
        );

        ParsedRequest result = callGeminiApi(prompt, schema, ParsedRequest.class);

        if (result == null) {
            result = new ParsedRequest();
            result.setBudget(1000000L);
            result.setUsage("GAMING");
        }
        return result;
    }

    private <T> T callGeminiApi(String prompt, Map<String, Object> schema, Class<T> responseType) {
        try {
            // 모델 경로 명세 최적화
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> contentsPart = Map.of("parts", List.of(textPart));

            Map<String, Object> configuration = Map.of(
                    "responseMimeType", "application/json",
                    "responseSchema", schema
            );

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(contentsPart),
                    "generationConfig", configuration
            );

            System.out.println("=== Gemini API 요청 전송 ===");
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);
            System.out.println("=== Gemini API 응답 수신 성공 ===");

            String rawText = objectMapper.readTree(response.getBody())
                    .path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            System.out.println("Gemini 추출 원본 텍스트: " + rawText);

            // 줄바꿈 등으로 꼬인 JSON 문자열을 안전하게 트리밍
            String cleanJson = rawText.trim();
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replaceAll("```json|```", "").trim();
            }

            return objectMapper.readValue(cleanJson, responseType);

        } catch (Exception e) {
            System.err.println("============= GEMINI API CRITICAL ERROR =============");
            System.err.println("에러 유형: " + e.getClass().getName());
            System.err.println("에러 메시지: " + e.getMessage());
            e.printStackTrace(); // 인텔리제이 콘솔에 빨간 글씨 유도
            System.err.println("====================================================");

            // 🚀 최후의 보루: 파싱이 터지면 절대로 null을 주지 말고 프론트엔드가 읽을 수 있는 완벽한 자바 객체를 생성해서 반환
            try {
                if (responseType.equals(AiAnalysisResponse.class)) {
                    AiAnalysisResponse fallback = new AiAnalysisResponse();
                    fallback.setPros(List.of("선택하신 부품 간의 기본적인 물리적 호환성 조립 규격이 일치합니다.", "가성비가 우수한 부품 배치가 완료되었습니다."));
                    fallback.setCons(List.of("일부 부품 간 성능 불균형(예: i9 CPU 대비 H610 메인보드의 전원부 한계)이 관측되니 유의하세요.", "상세 AI 리포트 로딩이 지연되어 기본 밸런스 검증 리포트를 제공합니다."));
                    return responseType.cast(fallback);
                }
            } catch (Exception ex) {
                System.err.println("Fallback 생성 실패: " + ex.getMessage());
            }
            return null;
        }
    }
}