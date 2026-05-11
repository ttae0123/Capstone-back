package com.example.capstone_backend.service;

import com.example.capstone_backend.dto.ResponseDTO;
import com.example.capstone_backend.dto.RequestDTO;
import com.example.capstone_backend.entity.*;
import com.example.capstone_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
@RequiredArgsConstructor
public class EstimateService {

    private final CpuRepository cpuRepository;
    private final GpuRepository gpuRepository;
    private final RamRepository ramRepository;
    private final SsdRepository ssdRepository;
    private final MainboardRepository mainboardRepository;
    private final PowerRepository powerRepository;
    private final CaseRepository caseRepository;

    private static final int MAX_RECOMMENDATION_COUNT = 3;

    public ResponseDTO.ResultList generateRecommendations(RequestDTO.EstimateRequest request) {

        System.out.println();
        System.out.println("======================================");
        System.out.println("[EstimateService] 추천 요청 수신");
        System.out.println("======================================");

        if (request == null) {
            throw new IllegalArgumentException("요청 데이터가 비어 있습니다.");
        }

        Long budget = request.budget();
        String usage = request.usage() != null
                ? request.usage().trim().toUpperCase()
                : "GAMING";

        System.out.println("[REQUEST] budget = " + budget);
        System.out.println("[REQUEST] usage = " + usage);

        if (budget == null || budget <= 0) {
            throw new IllegalArgumentException("budget 값이 올바르지 않습니다.");
        }

        BudgetRatio ratio = getBudgetRatio(usage);

        System.out.println();
        System.out.println("======================================");
        System.out.println("[DB] 부품 데이터 조회 시작");
        System.out.println("======================================");

        List<Cpu> cpus = cpuRepository.findAll();
        List<Gpu> gpus = gpuRepository.findAll();
        List<Ram> rams = ramRepository.findAll();
        List<Ssd> ssds = ssdRepository.findAll();
        List<Mainboard> mbs = mainboardRepository.findAll();
        List<Power> powers = powerRepository.findAll();
        List<Case> cases = caseRepository.findAll();

        System.out.println("[DB] CPU 개수 = " + cpus.size());
        System.out.println("[DB] GPU 개수 = " + gpus.size());
        System.out.println("[DB] RAM 개수 = " + rams.size());
        System.out.println("[DB] SSD 개수 = " + ssds.size());
        System.out.println("[DB] 메인보드 개수 = " + mbs.size());
        System.out.println("[DB] 파워 개수 = " + powers.size());
        System.out.println("[DB] 케이스 개수 = " + cases.size());

        if (cpus.isEmpty() || gpus.isEmpty() || rams.isEmpty() || ssds.isEmpty()
                || mbs.isEmpty() || powers.isEmpty() || cases.isEmpty()) {

            System.out.println("[ERROR] DB에 필요한 부품 데이터가 부족합니다.");
            return new ResponseDTO.ResultList(List.of());
        }

        long cpuBudget = (long) (budget * ratio.cpu());
        long gpuBudget = (long) (budget * ratio.gpu());
        long ramBudget = (long) (budget * ratio.ram());
        long ssdBudget = (long) (budget * ratio.ssd());
        long mbBudget = (long) (budget * ratio.mainboard());
        long powerBudget = (long) (budget * ratio.power());
        long caseBudget = (long) (budget * ratio.pcCase());

        System.out.println();
        System.out.println("======================================");
        System.out.println("[BudgetRatio] 용도별 예산 분배");
        System.out.println("======================================");
        System.out.println("[CPU] " + cpuBudget);
        System.out.println("[GPU] " + gpuBudget);
        System.out.println("[RAM] " + ramBudget);
        System.out.println("[SSD] " + ssdBudget);
        System.out.println("[MB] " + mbBudget);
        System.out.println("[POWER] " + powerBudget);
        System.out.println("[CASE] " + caseBudget);

        List<Cpu> cpuCandidates = cpus.stream()
                .filter(cpu -> hasValidPrice(cpu.getPrice()))
                .filter(cpu -> cpu.getPrice() <= cpuBudget * 1.7)
                .sorted((a, b) -> Long.compare(getCpuScore(b), getCpuScore(a)))
                .limit(30)
                .toList();

        List<Gpu> gpuCandidates = gpus.stream()
                .filter(gpu -> hasValidPrice(gpu.getPrice()))
                .filter(gpu -> gpu.getPrice() <= gpuBudget * 1.7)
                .sorted((a, b) -> Long.compare(getGpuScore(b), getGpuScore(a)))
                .limit(30)
                .toList();

        List<Ram> ramCandidates = rams.stream()
                .filter(ram -> hasValidPrice(ram.getPrice()))
                .filter(ram -> ram.getPrice() <= ramBudget * 1.8)
                .sorted((a, b) -> Long.compare(getRamScore(b), getRamScore(a)))
                .limit(20)
                .toList();

        List<Ssd> ssdCandidates = ssds.stream()
                .filter(ssd -> hasValidPrice(ssd.getPrice()))
                .filter(ssd -> ssd.getPrice() <= ssdBudget * 1.8)
                .sorted((a, b) -> Long.compare(getSsdScore(b), getSsdScore(a)))
                .limit(20)
                .toList();

        List<Mainboard> mbCandidates = mbs.stream()
                .filter(mb -> hasValidPrice(mb.getPrice()))
                .filter(mb -> mb.getPrice() <= mbBudget * 1.8)
                .sorted(Comparator.comparingLong(Mainboard::getPrice))
                .limit(30)
                .toList();

        List<Power> powerCandidates = powers.stream()
                .filter(power -> hasValidPrice(power.getPrice()))
                .filter(power -> power.getPrice() <= powerBudget * 2.0)
                .sorted((a, b) -> Long.compare(getPowerScore(b), getPowerScore(a)))
                .limit(20)
                .toList();

        List<Case> caseCandidates = cases.stream()
                .filter(pcCase -> hasValidPrice(pcCase.getPrice()))
                .filter(pcCase -> pcCase.getPrice() <= caseBudget * 2.0)
                .sorted((a, b) -> Long.compare(getCaseScore(b), getCaseScore(a)))
                .limit(20)
                .toList();

        System.out.println();
        System.out.println("======================================");
        System.out.println("[Candidate] 부품별 후보 개수");
        System.out.println("======================================");
        System.out.println("[CPU 후보] " + cpuCandidates.size());
        System.out.println("[GPU 후보] " + gpuCandidates.size());
        System.out.println("[RAM 후보] " + ramCandidates.size());
        System.out.println("[SSD 후보] " + ssdCandidates.size());
        System.out.println("[MB 후보] " + mbCandidates.size());
        System.out.println("[POWER 후보] " + powerCandidates.size());
        System.out.println("[CASE 후보] " + caseCandidates.size());

        if (cpuCandidates.isEmpty() || gpuCandidates.isEmpty() || ramCandidates.isEmpty()
                || ssdCandidates.isEmpty() || mbCandidates.isEmpty()
                || powerCandidates.isEmpty() || caseCandidates.isEmpty()) {

            System.out.println("[ERROR] 예산 필터링 후 후보가 부족합니다.");
            return new ResponseDTO.ResultList(List.of());
        }

        PriorityQueue<RecommendationCandidate> topCandidates =
                new PriorityQueue<>(Comparator.comparingLong(RecommendationCandidate::score));

        int failCpuMb = 0;
        int failRamMb = 0;
        int failGpuCase = 0;
        int failPower = 0;
        int failBudget = 0;
        int success = 0;

        System.out.println();
        System.out.println("======================================");
        System.out.println("[Algorithm] 조합 탐색 시작");
        System.out.println("======================================");

        for (Cpu cpu : cpuCandidates) {

            for (Mainboard mb : mbCandidates) {

                if (!isSocketCompatible(cpu.getSocketType(), mb.getSocketType())) {
                    failCpuMb++;
                    continue;
                }

                long cpuMbPrice = cpu.getPrice() + mb.getPrice();

                if (cpuMbPrice > budget) {
                    failBudget++;
                    continue;
                }

                for (Ram ram : ramCandidates) {

                    if (!isMemoryCompatible(ram.getMemoryType(), mb.getMemoryType())) {
                        failRamMb++;
                        continue;
                    }

                    long cpuMbRamPrice = cpuMbPrice + ram.getPrice();

                    if (cpuMbRamPrice > budget) {
                        failBudget++;
                        continue;
                    }

                    for (Gpu gpu : gpuCandidates) {

                        long cpuMbRamGpuPrice = cpuMbRamPrice + gpu.getPrice();

                        if (cpuMbRamGpuPrice > budget) {
                            failBudget++;
                            continue;
                        }

                        for (Case pcCase : caseCandidates) {

                            if (!isGpuCaseCompatible(gpu, pcCase)) {
                                failGpuCase++;
                                continue;
                            }

                            long cpuMbRamGpuCasePrice = cpuMbRamGpuPrice + pcCase.getPrice();

                            if (cpuMbRamGpuCasePrice > budget) {
                                failBudget++;
                                continue;
                            }

                            for (Power power : powerCandidates) {

                                if (!isPowerCompatible(gpu, power)) {
                                    failPower++;
                                    continue;
                                }

                                long cpuMbRamGpuCasePowerPrice =
                                        cpuMbRamGpuCasePrice + power.getPrice();

                                if (cpuMbRamGpuCasePowerPrice > budget) {
                                    failBudget++;
                                    continue;
                                }

                                for (Ssd ssd : ssdCandidates) {

                                    long total = cpuMbRamGpuCasePowerPrice + ssd.getPrice();

                                    if (total > budget) {
                                        failBudget++;
                                        continue;
                                    }

                                    long score = calculateTotalScore(
                                            cpu,
                                            gpu,
                                            ram,
                                            ssd,
                                            mb,
                                            power,
                                            pcCase,
                                            usage,
                                            budget,
                                            total
                                    );

                                    RecommendationCandidate candidate = new RecommendationCandidate(
                                            cpu,
                                            gpu,
                                            ram,
                                            ssd,
                                            mb,
                                            power,
                                            pcCase,
                                            total,
                                            score
                                    );

                                    addTopCandidate(topCandidates, candidate);
                                    success++;
                                }
                            }
                        }
                    }
                }
            }
        }

        System.out.println();
        System.out.println("======================================");
        System.out.println("[Algorithm] 조합 탐색 종료");
        System.out.println("======================================");
        System.out.println("[FAIL] CPU-메인보드 호환 실패 = " + failCpuMb);
        System.out.println("[FAIL] RAM-메인보드 호환 실패 = " + failRamMb);
        System.out.println("[FAIL] GPU-케이스 호환 실패 = " + failGpuCase);
        System.out.println("[FAIL] 파워 용량 부족 실패 = " + failPower);
        System.out.println("[FAIL] 총 예산 초과 실패 = " + failBudget);
        System.out.println("[SUCCESS] 성공 조합 수 = " + success);
        System.out.println("[TOP] 저장된 상위 후보 수 = " + topCandidates.size());

        if (topCandidates.isEmpty()) {
            System.out.println("[WARNING] 생성 가능한 추천 조합이 없습니다.");
            return new ResponseDTO.ResultList(List.of());
        }

        List<RecommendationCandidate> finalCandidates = new ArrayList<>(topCandidates);
        finalCandidates.sort((a, b) -> Long.compare(b.score(), a.score()));

        List<ResponseDTO.Recommendation> results = finalCandidates.stream()
                .map(candidate -> new ResponseDTO.Recommendation(
                        getUsageLabel(usage) + " 최적 조합",
                        candidate.totalPrice(),
                        List.of(
                                new ResponseDTO.PartDetail("CPU", candidate.cpu().getName(), candidate.cpu().getPrice(), "", ""),
                                new ResponseDTO.PartDetail("GPU", candidate.gpu().getName(), candidate.gpu().getPrice(), "", ""),
                                new ResponseDTO.PartDetail("RAM", candidate.ram().getName(), candidate.ram().getPrice(), "", ""),
                                new ResponseDTO.PartDetail("SSD", candidate.ssd().getName(), candidate.ssd().getPrice(), "", ""),
                                new ResponseDTO.PartDetail("메인보드", candidate.mainboard().getName(), candidate.mainboard().getPrice(), "", ""),
                                new ResponseDTO.PartDetail("파워", candidate.power().getName(), candidate.power().getPrice(), "", ""),
                                new ResponseDTO.PartDetail("케이스", candidate.pcCase().getName(), candidate.pcCase().getPrice(), "", "")
                        )
                ))
                .toList();

        System.out.println();
        System.out.println("======================================");
        System.out.println("[Response] 최종 응답 생성");
        System.out.println("======================================");
        System.out.println("[Response] 반환 추천 개수 = " + results.size());

        for (int i = 0; i < finalCandidates.size(); i++) {
            RecommendationCandidate c = finalCandidates.get(i);

            System.out.println();
            System.out.println("---------- 추천 " + (i + 1) + " ----------");
            System.out.println("총액 = " + c.totalPrice());
            System.out.println("점수 = " + c.score());
            System.out.println("CPU = " + c.cpu().getName());
            System.out.println("GPU = " + c.gpu().getName());
            System.out.println("RAM = " + c.ram().getName());
            System.out.println("SSD = " + c.ssd().getName());
            System.out.println("MB = " + c.mainboard().getName());
            System.out.println("POWER = " + c.power().getName());
            System.out.println("CASE = " + c.pcCase().getName());
        }

        System.out.println("======================================");
        System.out.println();

        return new ResponseDTO.ResultList(results);
    }

    private void addTopCandidate(
            PriorityQueue<RecommendationCandidate> topCandidates,
            RecommendationCandidate candidate
    ) {
        if (topCandidates.size() < MAX_RECOMMENDATION_COUNT) {
            topCandidates.offer(candidate);
            return;
        }

        RecommendationCandidate lowestCandidate = topCandidates.peek();

        if (lowestCandidate != null && candidate.score() > lowestCandidate.score()) {
            topCandidates.poll();
            topCandidates.offer(candidate);
        }
    }

    private BudgetRatio getBudgetRatio(String usage) {
        return switch (usage) {
            case "GAMING" -> new BudgetRatio(
                    0.200, // CPU
                    0.316, // GPU
                    0.162, // RAM
                    0.117, // SSD
                    0.086, // Mainboard
                    0.065, // Power
                    0.054  // Case
            );

            case "VIDEO_EDITING", "WORK" -> new BudgetRatio(
                    0.277,
                    0.258,
                    0.155,
                    0.124,
                    0.093,
                    0.052,
                    0.041
            );

            case "OFFICE" -> new BudgetRatio(
                    0.284,
                    0.105,
                    0.158,
                    0.158,
                    0.137,
                    0.074,
                    0.084
            );

            default -> new BudgetRatio(
                    0.247,
                    0.299,
                    0.124,
                    0.124,
                    0.093,
                    0.062,
                    0.051
            );
        };
    }

    private boolean hasValidPrice(Long price) {
        return price != null && price > 0;
    }

    private boolean isSocketCompatible(String cpuSocket, String mbSocket) {
        if (cpuSocket == null || mbSocket == null) return false;

        String c = normalizeSocket(cpuSocket);
        String m = normalizeSocket(mbSocket);

        if (c.isBlank() || m.isBlank()) {
            return false;
        }

        if (c.equals(m)) {
            return true;
        }

        return c.contains(m) || m.contains(c);
    }

    private boolean isMemoryCompatible(String ramMemoryType, String mbMemoryType) {
        if (ramMemoryType == null || mbMemoryType == null) return true;

        String ram = normalize(ramMemoryType);
        String mb = normalize(mbMemoryType);

        return ram.contains(mb) || mb.contains(ram);
    }

    private boolean isGpuCaseCompatible(Gpu gpu, Case pcCase) {
        if (gpu.getGpuLength() == null || pcCase.getGpuLength() == null) {
            return true;
        }

        return gpu.getGpuLength() <= pcCase.getGpuLength();
    }

    private boolean isPowerCompatible(Gpu gpu, Power power) {
        if (gpu.getRecommendedPower() == null || power.getWattage() == null) {
            return true;
        }

        return power.getWattage() >= gpu.getRecommendedPower();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toUpperCase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");
    }

    private String normalizeSocket(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toUpperCase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace("소켓", "")
                .replace("지원", "")
                .replace("INTEL", "")
                .replace("인텔", "")
                .replace("LGA", "")
                .replace("SOCKET", "");
    }

    private long calculateTotalScore(
            Cpu cpu,
            Gpu gpu,
            Ram ram,
            Ssd ssd,
            Mainboard mb,
            Power power,
            Case pcCase,
            String usage,
            Long budget,
            long total
    ) {
        long cpuScore = getCpuScore(cpu);
        long gpuScore = getGpuScore(gpu);
        long ramScore = getRamScore(ram);
        long ssdScore = getSsdScore(ssd);
        long powerScore = getPowerScore(power);
        long caseScore = getCaseScore(pcCase);

        long performanceScore;

        if ("GAMING".equals(usage)) {
            performanceScore =
                    gpuScore * 46
                            + cpuScore * 25
                            + ramScore * 11
                            + ssdScore * 8
                            + powerScore * 6
                            + caseScore * 4;
        } else if ("VIDEO_EDITING".equals(usage) || "WORK".equals(usage)) {
            performanceScore =
                    cpuScore * 36
                            + gpuScore * 26
                            + ramScore * 20
                            + ssdScore * 10
                            + powerScore * 5
                            + caseScore * 3;
        } else if ("OFFICE".equals(usage)) {
            performanceScore =
                    cpuScore * 36
                            + ramScore * 22
                            + ssdScore * 22
                            + gpuScore * 5
                            + powerScore * 8
                            + caseScore * 7;
        } else {
            performanceScore =
                    gpuScore * 35
                            + cpuScore * 30
                            + ramScore * 16
                            + ssdScore * 10
                            + powerScore * 5
                            + caseScore * 4;
        }

        long utilizationScore = (long) (((double) total / budget) * 10000);
        long valueScore = performanceScore / Math.max(total / 10000, 1);

        return performanceScore + utilizationScore + valueScore;
    }

    private long getCpuScore(Cpu cpu) {
        if (cpu.getBenchScore() != null && cpu.getBenchScore() > 0) {
            return cpu.getBenchScore();
        }

        return cpu.getPrice() != null ? cpu.getPrice() / 1000 : 0;
    }

    private long getGpuScore(Gpu gpu) {
        if (gpu.getBenchScore() != null && gpu.getBenchScore() > 0) {
            return gpu.getBenchScore();
        }

        return gpu.getPrice() != null ? gpu.getPrice() / 1000 : 0;
    }

    private long getRamScore(Ram ram) {
        long score = 0;

        if (ram.getBenchScore() != null && ram.getBenchScore() > 0) {
            score += ram.getBenchScore();
        }

        if (ram.getMemoryClock() != null && ram.getMemoryClock() > 0) {
            score += ram.getMemoryClock();
        }

        if (score == 0 && ram.getPrice() != null) {
            score = ram.getPrice() / 1000;
        }

        return score;
    }

    private long getSsdScore(Ssd ssd) {
        if (ssd.getBenchScore() != null && ssd.getBenchScore() > 0) {
            return ssd.getBenchScore();
        }

        return ssd.getPrice() != null ? ssd.getPrice() / 1000 : 0;
    }

    private long getPowerScore(Power power) {
        long wattage = power.getWattage() != null ? power.getWattage() : 0;
        long priceScore = power.getPrice() != null ? power.getPrice() / 1000 : 0;

        return wattage + priceScore;
    }

    private long getCaseScore(Case pcCase) {
        long gpuLength = pcCase.getGpuLength() != null ? pcCase.getGpuLength() : 0;
        long coolerLength = pcCase.getCoolerLength() != null ? pcCase.getCoolerLength() : 0;
        long priceScore = pcCase.getPrice() != null ? pcCase.getPrice() / 1000 : 0;

        return gpuLength + coolerLength + priceScore;
    }

    private String getUsageLabel(String usage) {
        if ("GAMING".equals(usage)) return "게이밍";
        if ("WORK".equals(usage)) return "전문 작업용";
        if ("VIDEO_EDITING".equals(usage)) return "영상 편집용";
        if ("OFFICE".equals(usage)) return "사무용";
        return "게이밍";
    }

    private record BudgetRatio(
            double cpu,
            double gpu,
            double ram,
            double ssd,
            double mainboard,
            double power,
            double pcCase
    ) {
    }

    private record RecommendationCandidate(
            Cpu cpu,
            Gpu gpu,
            Ram ram,
            Ssd ssd,
            Mainboard mainboard,
            Power power,
            Case pcCase,
            long totalPrice,
            long score
    ) {
    }
}