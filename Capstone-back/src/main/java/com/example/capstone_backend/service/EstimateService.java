package com.example.capstone_backend.service;

import com.example.capstone_backend.dto.ResponseDTO;
import com.example.capstone_backend.dto.RequestDTO;
import com.example.capstone_backend.entity.*;
import com.example.capstone_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    public ResponseDTO.ResultList generateRecommendations(RequestDTO.EstimateRequest request) {
        Long budget = request.budget();
        String usage = request.usage() != null ? request.usage() : "gaming"; // 기본값 게이밍
        System.out.println("요청 예산: " + budget + ", 용도: " + usage);

        List<Cpu> cpus = cpuRepository.findAll();
        List<Gpu> gpus = gpuRepository.findAll();
        List<Ram> rams = ramRepository.findAll();
        List<Ssd> ssds = ssdRepository.findAll();
        List<Mainboard> mbs = mainboardRepository.findAll();
        List<Power> powers = powerRepository.findAll();
        List<Case> cases = caseRepository.findAll();

        List<ResponseDTO.Recommendation> results = new ArrayList<>();

        for (Cpu cpu : cpus) {
            if (cpu.getPrice() > budget * 0.5) continue;
            for (Mainboard mb : mbs) {
                if (!isCompatible(cpu.getSocketType(), mb.getSocketType())) continue;

                for (Gpu gpu : gpus) {
                    if (cpu.getPrice() + gpu.getPrice() > budget * 0.8) continue;

                    // RAM, SSD, Power, Case는 가성비 데이터로 매칭
                    Ram ram = rams.isEmpty() ? null : rams.get(0);
                    Ssd ssd = ssds.isEmpty() ? null : ssds.get(0);
                    Power power = powers.isEmpty() ? null : powers.get(0);
                    Case pcCase = cases.isEmpty() ? null : cases.get(0);

                    if (ram == null || ssd == null || power == null || pcCase == null) continue;

                    long total = cpu.getPrice() + gpu.getPrice() + ram.getPrice() +
                            ssd.getPrice() + mb.getPrice() + power.getPrice() + pcCase.getPrice();

                    if (total <= budget && total >= budget * 0.5) {
                        // 용도별 가중치 점수 계산
                        long score = calculateUsageScore(cpu, gpu, usage);

                        results.add(new ResponseDTO.Recommendation(
                                getUsageLabel(usage) + " 최적 조합", total, List.of(
                                new ResponseDTO.PartDetail("CPU", cpu.getName(), cpu.getPrice(), "", ""),
                                new ResponseDTO.PartDetail("GPU", gpu.getName(), gpu.getPrice(), "", ""),
                                new ResponseDTO.PartDetail("RAM", ram.getName(), ram.getPrice(), "", ""),
                                new ResponseDTO.PartDetail("SSD", ssd.getName(), ssd.getPrice(), "", ""),
                                new ResponseDTO.PartDetail("메인보드", mb.getName(), mb.getPrice(), "", ""),
                                new ResponseDTO.PartDetail("파워", power.getName(), power.getPrice(), "", ""),
                                new ResponseDTO.PartDetail("케이스", pcCase.getName(), pcCase.getPrice(), "", "")
                        )
                        ));
                    }
                    if (results.size() >= 50) break;
                }
                if (results.size() >= 50) break;
            }
            if (results.size() >= 50) break;
        }

        // 점수 높은 순 정렬 후 상위 3개 반환
        return new ResponseDTO.ResultList(results.stream()
                .sorted((a, b) -> Long.compare(calculateFinalScore(b), calculateFinalScore(a)))
                .limit(3).toList());
    }

    private boolean isCompatible(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        String str1 = s1.replaceAll("[^0-9]", "");
        String str2 = s2.replaceAll("[^0-9]", "");
        if (!str1.isEmpty() && !str2.isEmpty()) return str1.equals(str2);
        return s1.contains(s2) || s2.contains(s1);
    }

    private long calculateUsageScore(Cpu c, Gpu g, String usage) {
        // 벤치마크 점수가 없을 경우 가격을 지표로 활용
        long cpuScore = c.getBenchScore() != null ? c.getBenchScore() : c.getPrice() / 1000;
        long gpuScore = g.getBenchScore() != null ? g.getBenchScore() : g.getPrice() / 1000;

        if ("gaming".equals(usage)) return (gpuScore * 2) + cpuScore;
        if ("work".equals(usage)) return (cpuScore * 2) + gpuScore;
        return -(c.getPrice() + g.getPrice()); // 사무용은 저렴할수록 높은 점수
    }

    private long calculateFinalScore(ResponseDTO.Recommendation r) {
        return r.totalEstimatedPrice(); // 정렬 기준 예시
    }

    private String getUsageLabel(String usage) {
        if ("gaming".equals(usage)) return "게이밍";
        if ("work".equals(usage)) return "전문 작업용";
        return "사무용";
    }
}