package org.belex.paymentschecker.controller;

import org.belex.paymentschecker.modal.AppUser;
import org.belex.paymentschecker.repo.AppUserRepository;
import org.belex.paymentschecker.service.CsvImportService;
import org.belex.paymentschecker.service.ReconciliationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class UploadController {

    private final CsvImportService csvImportService;
    private final ReconciliationService reconciliationService;
    private final AppUserRepository appUserRepository;

    public UploadController(CsvImportService csvImportService,
                            ReconciliationService reconciliationService,
                            AppUserRepository appUserRepository) {
        this.csvImportService = csvImportService;
        this.reconciliationService = reconciliationService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("ordersFile") MultipartFile ordersFile,
                          @RequestParam("paymentsFile") MultipartFile paymentsFile,
                          Authentication authentication,
                          Model model) {
        AppUser user = appUserRepository.findByEmail(authentication.getName()).orElseThrow();

        if (ordersFile.isEmpty() || paymentsFile.isEmpty()) {
            model.addAttribute("error", "Please choose both an orders.csv and a payments.csv file.");
            return "upload";
        }

        try {
            int orders = csvImportService.importOrders(user.getId(), ordersFile);
            int payments = csvImportService.importPayments(user.getId(), paymentsFile);
            reconciliationService.reconcile(user.getId());
            model.addAttribute("message", "Imported " + orders + " orders and " + payments
                    + " payments, and ran reconciliation.");
        } catch (IOException e) {
            model.addAttribute("error", "Could not read one of the files: " + e.getMessage());
            return "upload";
        }

        return "redirect:/dashboard";
    }
}
