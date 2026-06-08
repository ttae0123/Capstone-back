package com.example.capstone_backend.dto;

import java.util.Map;

public record ManualPartResponse(
        Long id,
        String category,
        String name,
        Long price,
        Map<String, Object> specs
) {
}