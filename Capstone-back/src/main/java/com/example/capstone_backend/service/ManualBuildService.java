package com.example.capstone_backend.service;

import com.example.capstone_backend.dto.ManualPartListResponse;
import com.example.capstone_backend.dto.ManualPartResponse;
import com.example.capstone_backend.entity.*;
import com.example.capstone_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ManualBuildService {

    private final CpuRepository cpuRepository;

    public ManualPartListResponse getParts(String category) {
        String normalizedCategory = category.toLowerCase();

        if (normalizedCategory.equals("cpu")) {
            return getCpuParts();
        }

        throw new IllegalArgumentException("지원하지 않는 부품 카테고리입니다: " + category);
    }

    private ManualPartListResponse getCpuParts() {
        List<Cpu> cpus = cpuRepository.findAll();

        List<ManualPartResponse> parts = new ArrayList<>();

        for (Cpu cpu : cpus) {
            Map<String, Object> specs = new LinkedHashMap<>();
            specs.put("socketType", cpu.getSocketType());
            specs.put("memoryType", cpu.getMemoryType());
            specs.put("benchScore", cpu.getBenchScore());

            ManualPartResponse part = new ManualPartResponse(
                    cpu.getId(),
                    "CPU",
                    cpu.getName(),
                    cpu.getPrice(),
                    specs
            );

            parts.add(part);
        }

        return new ManualPartListResponse(
                "cpu",
                parts.size(),
                parts
        );
    }
}
