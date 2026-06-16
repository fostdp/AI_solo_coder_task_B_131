package com.nestcart.modules.evolution_analyzer.controller;

import com.nestcart.modules.evolution_analyzer.dto.DynastyEvolutionResult;
import com.nestcart.modules.evolution_analyzer.entity.DynastyCart;
import com.nestcart.modules.evolution_analyzer.repository.DynastyCartRepository;
import com.nestcart.modules.evolution_analyzer.service.DynastyEvolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/evolution-analyzer")
@RequiredArgsConstructor
public class DynastyEvolutionController {

    private final DynastyEvolutionService dynastyEvolutionService;
    private final DynastyCartRepository dynastyCartRepository;

    @GetMapping("/carts")
    public ResponseEntity<List<DynastyCart>> getAllDynastyCarts() {
        return ResponseEntity.ok(dynastyEvolutionService.getAllDynastyCarts());
    }

    @GetMapping("/carts/{id}")
    public ResponseEntity<DynastyCart> getDynastyCart(@PathVariable UUID id) {
        return dynastyCartRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/evolution")
    public ResponseEntity<DynastyEvolutionResult> getEvolutionAnalysis() {
        return ResponseEntity.ok(dynastyEvolutionService.analyzeEvolution());
    }
}
