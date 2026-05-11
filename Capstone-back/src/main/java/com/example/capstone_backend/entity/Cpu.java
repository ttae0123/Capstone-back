package com.example.capstone_backend.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "cpu")
public class Cpu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(name = "socket_type", columnDefinition = "TEXT")
    private String socketType;

    @Column(name = "memory_type", columnDefinition = "TEXT")
    private String memoryType;

    @Column(name = "bench_score")
    private Long benchScore;

}
