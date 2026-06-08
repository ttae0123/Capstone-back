package com.example.capstone_backend.controller;

import com.example.capstone_backend.dto.*;
import com.example.capstone_backend.service.ManualBuildService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manual-build")
@RequiredArgsConstructor
public class ManualBuildController {

    private final ManualBuildService manualBuildService;

    @GetMapping("/parts/{category}")
    public ResponseEntity<ManualPartListResponse> getParts(
            @PathVariable String category
    ) {
        ManualPartListResponse response = manualBuildService.getParts(category);
        return ResponseEntity.ok(response);
    }
}