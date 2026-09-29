package com.example.capstone_backend.service;

import com.example.capstone_backend.dto.ManualBuildSelection;
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
    private final MainboardRepository mainboardRepository;
    private final RamRepository ramRepository;
    private final GpuRepository gpuRepository;
    private final SsdRepository ssdRepository;
    private final PowerRepository powerRepository;
    private final CaseRepository caseRepository;
    private final CoolerRepository coolerRepository;

    public ManualPartListResponse getParts(String category, ManualBuildSelection selection) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("부품 카테고리가 비어 있습니다.");
        }

        String normalizedCategory = category.trim().toLowerCase();

        if (normalizedCategory.equals("cpu")) {
            return getCpuParts(selection);
        }

        if (normalizedCategory.equals("mainboard")) {
            return getMainboardParts(selection);
        }

        if (normalizedCategory.equals("ram")) {
            return getRamParts(selection);
        }

        if (normalizedCategory.equals("gpu")) {
            return getGpuParts(selection);
        }

        if (normalizedCategory.equals("ssd")) {
            return getSsdParts();
        }

        if (normalizedCategory.equals("power")) {
            return getPowerParts(selection);
        }

        if (normalizedCategory.equals("case")) {
            return getCaseParts(selection);
        }

        if (normalizedCategory.equals("cooler")) {
            return getCoolerParts(selection);
        }

        throw new IllegalArgumentException("지원하지 않는 부품 카테고리입니다: " + category);
    }

    private ManualPartListResponse getCpuParts(ManualBuildSelection selection) {
        List<Cpu> cpus = cpuRepository.findAll();

        if (selection != null && selection.mainboardId() != null) {
            Mainboard selectedMainboard = getMainboardById(selection.mainboardId());

            cpus = cpus.stream()
                    .filter(cpu -> isSame(cpu.getSocketType(), selectedMainboard.getSocketType()))
                    .toList();
        }

        if (selection != null && selection.coolerId() != null) {
            Cooler selectedCooler = getCoolerById(selection.coolerId());

            cpus = cpus.stream()
                    .filter(cpu -> isCpuCoolerCompatible(
                            cpu.getSocketType(),
                            selectedCooler.getSocketType()
                    ))
                    .toList();
        }

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
                    cpu.getBrand(),
                    null,
                    cpu.getProductCode(),
                    cpu.getProductUrl(),
                    specs
            );

            parts.add(part);
        }

        return new ManualPartListResponse("cpu", parts.size(), parts);
    }

    private ManualPartListResponse getMainboardParts(ManualBuildSelection selection) {
        List<Mainboard> mainboards = mainboardRepository.findAll();

        if (selection != null && selection.cpuId() != null) {
            Cpu selectedCpu = getCpuById(selection.cpuId());

            mainboards = mainboards.stream()
                    .filter(mainboard -> isSame(mainboard.getSocketType(), selectedCpu.getSocketType()))
                    .toList();
        }

        if (selection != null && selection.ramId() != null) {
            Ram selectedRam = getRamById(selection.ramId());

            mainboards = mainboards.stream()
                    .filter(mainboard -> isSame(mainboard.getMemoryType(), selectedRam.getMemoryType()))
                    .filter(mainboard -> isMemoryClockCompatible(
                            selectedRam.getMemoryClock(),
                            mainboard.getMemoryClock()
                    ))
                    .toList();
        }

        if (selection != null && selection.gpuId() != null) {
            Gpu selectedGpu = getGpuById(selection.gpuId());

            mainboards = mainboards.stream()
                    .filter(mainboard -> isPcieCompatible(
                            mainboard.getPcieType(),
                            selectedGpu.getPcieType()
                    ))
                    .toList();
        }

        if (selection != null && selection.caseId() != null) {
            Case selectedCase = getCaseById(selection.caseId());

            mainboards = mainboards.stream()
                    .filter(mainboard -> isCaseSizeCompatible(
                            selectedCase.getSize(),
                            mainboard.getSize()
                    ))
                    .toList();
        }

        List<ManualPartResponse> parts = new ArrayList<>();

        for (Mainboard mainboard : mainboards) {
            Map<String, Object> specs = new LinkedHashMap<>();
            specs.put("socketType", mainboard.getSocketType());
            specs.put("memoryType", mainboard.getMemoryType());
            specs.put("pcieType", mainboard.getPcieType());
            specs.put("size", mainboard.getSize());
            specs.put("memoryClock", mainboard.getMemoryClock());

            ManualPartResponse part = new ManualPartResponse(
                    mainboard.getId(),
                    "MAINBOARD",
                    mainboard.getName(),
                    mainboard.getPrice(),
                    mainboard.getBrand(),
                    null,
                    mainboard.getProductCode(),
                    mainboard.getProductUrl(),
                    specs
            );

            parts.add(part);
        }

        return new ManualPartListResponse("mainboard", parts.size(), parts);
    }

    private ManualPartListResponse getRamParts(ManualBuildSelection selection) {
        List<Ram> rams = ramRepository.findAll();

        if (selection != null && selection.mainboardId() != null) {
            Mainboard selectedMainboard = getMainboardById(selection.mainboardId());

            rams = rams.stream()
                    .filter(ram -> isSame(
                            ram.getMemoryType(),
                            selectedMainboard.getMemoryType()
                    ))
                    .filter(ram -> isMemoryClockCompatible(
                            ram.getMemoryClock(),
                            selectedMainboard.getMemoryClock()
                    ))
                    .toList();
        }

        List<ManualPartResponse> parts = new ArrayList<>();

        for (Ram ram : rams) {
            Map<String, Object> specs = new LinkedHashMap<>();
            specs.put("memoryType", ram.getMemoryType());
            specs.put("memoryClock", ram.getMemoryClock());
            specs.put("capacity", ram.getCapacity());
            specs.put("moduleCount", ram.getModuleCount());
            specs.put("benchScore", ram.getBenchScore());

            ManualPartResponse part = new ManualPartResponse(
                    ram.getId(),
                    "RAM",
                    ram.getName(),
                    ram.getPrice(),
                    null,
                    null,
                    ram.getProductCode(),
                    ram.getProductUrl(),
                    specs
            );

            parts.add(part);
        }

        return new ManualPartListResponse("ram", parts.size(), parts);
    }

    private ManualPartListResponse getGpuParts(ManualBuildSelection selection) {
        List<Gpu> gpus = gpuRepository.findAll();

        if (selection != null && selection.mainboardId() != null) {
            Mainboard selectedMainboard = getMainboardById(selection.mainboardId());

            gpus = gpus.stream()
                    .filter(gpu -> isPcieCompatible(
                            selectedMainboard.getPcieType(),
                            gpu.getPcieType()
                    ))
                    .toList();
        }

        if (selection != null && selection.caseId() != null) {
            Case selectedCase = getCaseById(selection.caseId());

            gpus = gpus.stream()
                    .filter(gpu -> isGpuLengthCompatible(
                            gpu.getGpuLength(),
                            selectedCase.getGpuLength()
                    ))
                    .toList();
        }

        if (selection != null && selection.powerId() != null) {
            Power selectedPower = getPowerById(selection.powerId());

            gpus = gpus.stream()
                    .filter(gpu -> isPowerCompatible(
                            selectedPower.getWattage(),
                            gpu.getRecommendedPower()
                    ))
                    .toList();
        }

        List<ManualPartResponse> parts = new ArrayList<>();

        for (Gpu gpu : gpus) {
            Map<String, Object> specs = new LinkedHashMap<>();
            specs.put("pcieType", gpu.getPcieType());
            specs.put("gpuLength", gpu.getGpuLength());
            specs.put("recommendedPower", gpu.getRecommendedPower());
            specs.put("benchScore", gpu.getBenchScore());

            ManualPartResponse part = new ManualPartResponse(
                    gpu.getId(),
                    "GPU",
                    gpu.getName(),
                    gpu.getPrice(),
                    gpu.getBrand(),
                    gpu.getChipsetBrand(),
                    gpu.getProductCode(),
                    gpu.getProductUrl(),
                    specs
            );

            parts.add(part);
        }

        return new ManualPartListResponse("gpu", parts.size(), parts);
    }

    private ManualPartListResponse getSsdParts() {
        List<Ssd> ssds = ssdRepository.findAll();

        List<ManualPartResponse> parts = new ArrayList<>();

        for (Ssd ssd : ssds) {
            Map<String, Object> specs = new LinkedHashMap<>();
            specs.put("capacity", ssd.getCapacity());
            specs.put("benchScore", ssd.getBenchScore());

            ManualPartResponse part = new ManualPartResponse(
                    ssd.getId(),
                    "SSD",
                    ssd.getName(),
                    ssd.getPrice(),
                    null,
                    null,
                    ssd.getProductCode(),
                    ssd.getProductUrl(),
                    specs
            );

            parts.add(part);
        }

        return new ManualPartListResponse("ssd", parts.size(), parts);
    }

    private ManualPartListResponse getPowerParts(ManualBuildSelection selection) {
        List<Power> powers = powerRepository.findAll();

        if (selection != null && selection.gpuId() != null) {
            Gpu selectedGpu = getGpuById(selection.gpuId());

            powers = powers.stream()
                    .filter(power -> isPowerCompatible(
                            power.getWattage(),
                            selectedGpu.getRecommendedPower()
                    ))
                    .toList();
        }

        List<ManualPartResponse> parts = new ArrayList<>();

        for (Power power : powers) {
            Map<String, Object> specs = new LinkedHashMap<>();
            specs.put("size", power.getSize());
            specs.put("wattage", power.getWattage());

            ManualPartResponse part = new ManualPartResponse(
                    power.getId(),
                    "POWER",
                    power.getName(),
                    power.getPrice(),
                    null,
                    null,
                    power.getProductCode(),
                    power.getProductUrl(),
                    specs
            );

            parts.add(part);
        }

        return new ManualPartListResponse("power", parts.size(), parts);
    }

    private ManualPartListResponse getCaseParts(ManualBuildSelection selection) {
        List<Case> cases = caseRepository.findAll();

        if (selection != null && selection.mainboardId() != null) {
            Mainboard selectedMainboard = getMainboardById(selection.mainboardId());

            cases = cases.stream()
                    .filter(pcCase -> isCaseSizeCompatible(
                            pcCase.getSize(),
                            selectedMainboard.getSize()
                    ))
                    .toList();
        }

        if (selection != null && selection.gpuId() != null) {
            Gpu selectedGpu = getGpuById(selection.gpuId());

            cases = cases.stream()
                    .filter(pcCase -> isGpuLengthCompatible(
                            selectedGpu.getGpuLength(),
                            pcCase.getGpuLength()
                    ))
                    .toList();
        }

        if (selection != null && selection.coolerId() != null) {
            Cooler selectedCooler = getCoolerById(selection.coolerId());

            cases = cases.stream()
                    .filter(pcCase -> isCoolerHeightCompatible(
                            selectedCooler.getCoolerLength(),
                            pcCase.getCoolerLength()
                    ))
                    .toList();
        }

        List<ManualPartResponse> parts = new ArrayList<>();

        for (Case pcCase : cases) {
            Map<String, Object> specs = new LinkedHashMap<>();
            specs.put("size", pcCase.getSize());
            specs.put("gpuLength", pcCase.getGpuLength());
            specs.put("coolerLength", pcCase.getCoolerLength());

            ManualPartResponse part = new ManualPartResponse(
                    pcCase.getId(),
                    "CASE",
                    pcCase.getName(),
                    pcCase.getPrice(),
                    null,
                    null,
                    pcCase.getProductCode(),
                    pcCase.getProductUrl(),
                    specs
            );

            parts.add(part);
        }

        return new ManualPartListResponse("case", parts.size(), parts);
    }

    private ManualPartListResponse getCoolerParts(ManualBuildSelection selection) {
        List<Cooler> coolers = coolerRepository.findAll();

        if (selection != null && selection.cpuId() != null) {
            Cpu selectedCpu = getCpuById(selection.cpuId());

            coolers = coolers.stream()
                    .filter(cooler -> isCpuCoolerCompatible(
                            selectedCpu.getSocketType(),
                            cooler.getSocketType()
                    ))
                    .toList();
        }

        if (selection != null && selection.caseId() != null) {
            Case selectedCase = getCaseById(selection.caseId());

            coolers = coolers.stream()
                    .filter(cooler -> isCoolerHeightCompatible(
                            cooler.getCoolerLength(),
                            selectedCase.getCoolerLength()
                    ))
                    .toList();
        }

        List<ManualPartResponse> parts = new ArrayList<>();

        for (Cooler cooler : coolers) {
            Map<String, Object> specs = new LinkedHashMap<>();
            specs.put("socketType", cooler.getSocketType());
            specs.put("coolerLength", cooler.getCoolerLength());

            ManualPartResponse part = new ManualPartResponse(
                    cooler.getId(),
                    "COOLER",
                    cooler.getName(),
                    cooler.getPrice(),
                    null,
                    null,
                    cooler.getProductCode(),
                    cooler.getProductUrl(),
                    specs
            );

            parts.add(part);
        }

        return new ManualPartListResponse("cooler", parts.size(), parts);
    }

    private Cpu getCpuById(Long cpuId) {
        return cpuRepository.findById(cpuId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 CPU입니다. cpuId = " + cpuId
                        )
                );
    }

    private Mainboard getMainboardById(Long mainboardId) {
        return mainboardRepository.findById(mainboardId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 메인보드입니다. mainboardId = " + mainboardId
                        )
                );
    }

    private Ram getRamById(Long ramId) {
        return ramRepository.findById(ramId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 RAM입니다. ramId = " + ramId
                        )
                );
    }

    private Gpu getGpuById(Long gpuId) {
        return gpuRepository.findById(gpuId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 GPU입니다. gpuId = " + gpuId
                        )
                );
    }

    private Power getPowerById(Long powerId) {
        return powerRepository.findById(powerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 파워입니다. powerId = " + powerId
                        )
                );
    }

    private Case getCaseById(Long caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 케이스입니다. caseId = " + caseId
                        )
                );
    }

    private Cooler getCoolerById(Long coolerId) {
        return coolerRepository.findById(coolerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 쿨러입니다. coolerId = " + coolerId
                        )
                );
    }

    private boolean isSame(String value1, String value2) {
        if (value1 == null || value2 == null) {
            return false;
        }

        return value1.trim().equalsIgnoreCase(value2.trim());
    }

    private boolean isMemoryClockCompatible(Long ramClock, Long mainboardClock) {
        if (ramClock == null || mainboardClock == null) {
            return true;
        }

        return ramClock <= mainboardClock;
    }

    private boolean isGpuLengthCompatible(Long gpuLength, Long caseGpuLength) {
        if (gpuLength == null || caseGpuLength == null) {
            return true;
        }

        return caseGpuLength >= gpuLength;
    }

    private boolean isPowerCompatible(Long powerWattage, Long recommendedPower) {
        if (powerWattage == null || recommendedPower == null) {
            return true;
        }

        return powerWattage >= recommendedPower;
    }

    private boolean isCoolerHeightCompatible(Long coolerLength, Long caseCoolerLength) {
        if (coolerLength == null || caseCoolerLength == null) {
            return true;
        }

        return caseCoolerLength >= coolerLength;
    }

    private boolean isCpuCoolerCompatible(String cpuSocketType, String coolerSocketType) {
        if (cpuSocketType == null || coolerSocketType == null) {
            return false;
        }

        String cpuSocket = normalizeSocket(cpuSocketType);
        String coolerSocket = normalizeSocket(coolerSocketType);

        if (cpuSocket.isBlank() || coolerSocket.isBlank()) {
            return false;
        }

        if (coolerSocket.contains(cpuSocket) || cpuSocket.contains(coolerSocket)) {
            return true;
        }

        if (isIntel115xCompatible(cpuSocket, coolerSocket)) {
            return true;
        }

        return isAmdSocketCompatible(cpuSocket, coolerSocket);
    }

    private boolean isIntel115xCompatible(String cpuSocket, String coolerSocket) {
        boolean cpuIs115x =
                cpuSocket.equals("1150")
                        || cpuSocket.equals("1151")
                        || cpuSocket.equals("1151V2")
                        || cpuSocket.equals("1155")
                        || cpuSocket.equals("1156");

        boolean coolerSupports115x =
                coolerSocket.contains("115X")
                        || coolerSocket.contains("1150")
                        || coolerSocket.contains("1151")
                        || coolerSocket.contains("1151V2")
                        || coolerSocket.contains("1155")
                        || coolerSocket.contains("1156");

        return cpuIs115x && coolerSupports115x;
    }

    private boolean isAmdSocketCompatible(String cpuSocket, String coolerSocket) {
        if (cpuSocket.equals("AMD4")) {
            return coolerSocket.contains("AM4")
                    || coolerSocket.contains("AMD4");
        }

        if (cpuSocket.equals("AMD5")) {
            return coolerSocket.contains("AM5")
                    || coolerSocket.contains("AMD5");
        }

        return false;
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

    private boolean isPcieCompatible(String mainboardPcieType, String gpuPcieType) {
        if (mainboardPcieType == null || gpuPcieType == null) {
            return true;
        }

        int mainboardGeneration = extractPcieGeneration(mainboardPcieType);
        int gpuGeneration = extractPcieGeneration(gpuPcieType);

        if (mainboardGeneration == 0 || gpuGeneration == 0) {
            return true;
        }

        return mainboardGeneration >= gpuGeneration;
    }

    private int extractPcieGeneration(String pcieType) {
        String value = pcieType.toUpperCase();

        if (value.contains("5.0")) {
            return 5;
        }

        if (value.contains("4.0")) {
            return 4;
        }

        if (value.contains("3.0")) {
            return 3;
        }

        if (value.contains("2.0")) {
            return 2;
        }

        return 0;
    }

    private boolean isCaseSizeCompatible(String caseSize, String mainboardSize) {
        if (caseSize == null || mainboardSize == null) {
            return true;
        }

        String normalizedCaseSize = caseSize.trim().toUpperCase();
        String normalizedMainboardSize = mainboardSize.trim().toUpperCase();

        if (normalizedCaseSize.equals("EATX")) {
            return normalizedMainboardSize.equals("EATX")
                    || normalizedMainboardSize.equals("ATX")
                    || normalizedMainboardSize.equals("MATX")
                    || normalizedMainboardSize.equals("ITX");
        }

        if (normalizedCaseSize.equals("ATX")) {
            return normalizedMainboardSize.equals("ATX")
                    || normalizedMainboardSize.equals("MATX")
                    || normalizedMainboardSize.equals("ITX");
        }

        if (normalizedCaseSize.equals("MATX")) {
            return normalizedMainboardSize.equals("MATX")
                    || normalizedMainboardSize.equals("ITX");
        }

        if (normalizedCaseSize.equals("ITX")) {
            return normalizedMainboardSize.equals("ITX");
        }

        return normalizedCaseSize.equals(normalizedMainboardSize);
    }
}