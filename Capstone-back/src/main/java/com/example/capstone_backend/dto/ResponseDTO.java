package com.example.capstone_backend.dto;

import java.util.List;

public class ResponseDTO {

    public record ResultList(
            List<Recommendation> recommendations
    ) {
    }


    public record Recommendation(
            String rankName,
            Long totalEstimatedPrice,
            List<PartDetail> parts
    ) {
    }


    public record PartDetail(
            String category,
            String name,
            Long price,
            String brand,
            String chipsetBrand,
            Number benchScore,
            String specSummary,
            Long productCode,
            String productUrl
    ) {
    }
}