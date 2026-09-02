package org.belex.paymentschecker.controller;

import org.belex.paymentschecker.dto.ExplanationResult;
import org.belex.paymentschecker.modal.AppUser;
import org.belex.paymentschecker.modal.Discrepancy;
import org.belex.paymentschecker.repo.AppUserRepository;
import org.belex.paymentschecker.repo.DiscrepancyRepository;
import org.belex.paymentschecker.service.LlmExplanationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class DiscrepancyController {

    private static final List<String> DISCREPANCY_TYPES = List.of(
            "MISSING_PAYMENT", "MISSING_ORDER", "AMOUNT_MISMATCH", "CURRENCY_MISMATCH",
            "DUPLICATE_PAYMENT", "DUPLICATE_ORDER", "STATUS_MISMATCH");

    private final AppUserRepository appUserRepository;
    private final DiscrepancyRepository discrepancyRepository;
    private final LlmExplanationService llmExplanationService;

    public DiscrepancyController(AppUserRepository appUserRepository,
                                 DiscrepancyRepository discrepancyRepository,
                                 LlmExplanationService llmExplanationService) {
        this.appUserRepository = appUserRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.llmExplanationService = llmExplanationService;
    }

    @GetMapping("/discrepancies")
    public String list(Authentication authentication,
                        @RequestParam(required = false) String type,
                        @RequestParam(required = false) String search,
                        Model model) {
        AppUser user = appUserRepository.findByEmail(authentication.getName()).orElseThrow();
        List<Discrepancy> all = discrepancyRepository.findByOwnerId(user.getId());

        String searchLower = search == null ? null : search.trim().toLowerCase();
        List<Discrepancy> filtered = all.stream()
                .filter(d -> type == null || type.isBlank() || type.equals(d.getType()))
                .filter(d -> searchLower == null || searchLower.isBlank()
                        || contains(d.getOrderId(), searchLower)
                        || contains(d.getPaymentRef(), searchLower)
                        || contains(d.getDescription(), searchLower))
                .toList();

        model.addAttribute("discrepancies", filtered);
        model.addAttribute("types", DISCREPANCY_TYPES);
        model.addAttribute("selectedType", type == null ? "" : type);
        model.addAttribute("search", search == null ? "" : search);
        return "discrepancies";
    }

    @PostMapping("/discrepancies/{id}/explain")
    @ResponseBody
    public ResponseEntity<ExplanationResult> explain(Authentication authentication, @PathVariable Long id) {
        AppUser user = appUserRepository.findByEmail(authentication.getName()).orElseThrow();
        Discrepancy d = discrepancyRepository.findById(id).orElse(null);

        if (d == null || !d.getOwnerId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ExplanationResult.failure("Not found."));
        }

        ExplanationResult result = llmExplanationService.explain(d);
        if (result.getError() != null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
        }
        return ResponseEntity.ok(result);
    }

    private boolean contains(String value, String searchLower) {
        return value != null && value.toLowerCase().contains(searchLower);
    }
}
