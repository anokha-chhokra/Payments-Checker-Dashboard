package org.belex.paymentschecker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.belex.paymentschecker.modal.AppUser;
import org.belex.paymentschecker.modal.Discrepancy;
import org.belex.paymentschecker.modal.OrderRecord;
import org.belex.paymentschecker.repo.AppUserRepository;
import org.belex.paymentschecker.repo.DiscrepancyRepository;
import org.belex.paymentschecker.repo.OrderRecordRepository;
import org.belex.paymentschecker.repo.PaymentRecordRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private static final List<String> DISCREPANCY_TYPES = List.of(
            "MISSING_PAYMENT", "MISSING_ORDER", "AMOUNT_MISMATCH", "CURRENCY_MISMATCH",
            "DUPLICATE_PAYMENT", "DUPLICATE_ORDER", "STATUS_MISMATCH");

    private final AppUserRepository appUserRepository;
    private final OrderRecordRepository orderRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final DiscrepancyRepository discrepancyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DashboardController(AppUserRepository appUserRepository,
                               OrderRecordRepository orderRecordRepository,
                               PaymentRecordRepository paymentRecordRepository,
                               DiscrepancyRepository discrepancyRepository) {
        this.appUserRepository = appUserRepository;
        this.orderRecordRepository = orderRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.discrepancyRepository = discrepancyRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) throws Exception {
        AppUser user = appUserRepository.findByEmail(authentication.getName()).orElseThrow();
        Long ownerId = user.getId();

        List<OrderRecord> orders = orderRecordRepository.findByOwnerId(ownerId);
        long totalPayments = paymentRecordRepository.countByOwnerId(ownerId);
        List<Discrepancy> discrepancies = discrepancyRepository.findByOwnerId(ownerId);

        Set<String> disputedOrderIds = discrepancies.stream()
                .map(Discrepancy::getOrderId)
                .filter(id -> id != null)
                .map(id -> id.trim().toUpperCase())
                .collect(Collectors.toSet());

        BigDecimal reconciled = BigDecimal.ZERO;
        BigDecimal inDispute = BigDecimal.ZERO;
        for (OrderRecord o : orders) {
            BigDecimal amount = o.getNetAmount() == null ? BigDecimal.ZERO : o.getNetAmount();
            boolean disputed = o.getOrderId() != null && disputedOrderIds.contains(o.getOrderId().trim().toUpperCase());
            if (disputed) {
                inDispute = inDispute.add(amount);
            } else {
                reconciled = reconciled.add(amount);
            }
        }

        BigDecimal moneyAtRisk = discrepancies.stream()
                .map(d -> d.getAmountAtRisk() == null ? BigDecimal.ZERO : d.getAmountAtRisk())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (String type : DISCREPANCY_TYPES) {
            breakdown.put(type, 0L);
        }
        for (Discrepancy d : discrepancies) {
            breakdown.merge(d.getType(), 1L, Long::sum);
        }

        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("totalPayments", totalPayments);
        model.addAttribute("totalReconciled", reconciled);
        model.addAttribute("totalInDispute", inDispute);
        model.addAttribute("moneyAtRisk", moneyAtRisk);
        model.addAttribute("totalDiscrepancies", discrepancies.size());
        model.addAttribute("breakdown", breakdown);
        model.addAttribute("breakdownLabelsJson", objectMapper.writeValueAsString(breakdown.keySet()));
        model.addAttribute("breakdownValuesJson", objectMapper.writeValueAsString(breakdown.values()));

        return "dashboard";
    }
}
