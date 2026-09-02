package org.belex.paymentschecker.service;

import org.belex.paymentschecker.modal.OrderRecord;
import org.belex.paymentschecker.modal.PaymentRecord;
import org.belex.paymentschecker.repo.OrderRecordRepository;
import org.belex.paymentschecker.repo.PaymentRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-rolled CSV reader: the two source files have no quoted/embedded commas,
 * so a simple split is enough and keeps this easy to follow.
 */
@Service
public class CsvImportService {

    private static final DateTimeFormatter ORDER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter PAYMENT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrderRecordRepository orderRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    public CsvImportService(OrderRecordRepository orderRecordRepository,
                            PaymentRecordRepository paymentRecordRepository) {
        this.orderRecordRepository = orderRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    public int importOrders(Long ownerId, MultipartFile file) throws IOException {
        List<Map<String, String>> rows = readRows(file);
        List<OrderRecord> records = new ArrayList<>();
        for (Map<String, String> row : rows) {
            OrderRecord order = new OrderRecord();
            order.setOwnerId(ownerId);
            order.setOrderId(trimToNull(row.get("order_id")));
            order.setOrderDate(parseDateTime(row.get("order_date"), ORDER_DATE_FORMAT));
            order.setCustomerEmail(trimToNull(row.get("customer_email")));
            order.setCurrency(trimToNull(row.get("currency")));
            order.setGrossAmount(parseAmount(row.get("gross_amount")));
            order.setDiscount(parseAmount(row.get("discount")));
            order.setNetAmount(parseAmount(row.get("net_amount")));
            order.setStatus(trimToNull(row.get("status")));
            records.add(order);
        }
        orderRecordRepository.saveAll(records);
        return records.size();
    }

    public int importPayments(Long ownerId, MultipartFile file) throws IOException {
        List<Map<String, String>> rows = readRows(file);
        List<PaymentRecord> records = new ArrayList<>();
        for (Map<String, String> row : rows) {
            PaymentRecord payment = new PaymentRecord();
            payment.setOwnerId(ownerId);
            payment.setTransactionRef(trimToNull(row.get("transaction_ref")));
            payment.setProcessedAt(parseDateTime(row.get("processed_at"), PAYMENT_DATE_FORMAT));
            payment.setOrderReference(trimToNull(row.get("order_reference")));
            payment.setCurrency(trimToNull(row.get("currency")));
            payment.setAmount(parseAmount(row.get("amount")));
            payment.setFee(parseAmount(row.get("fee")));
            payment.setNetSettled(parseAmount(row.get("net_settled")));
            payment.setType(trimToNull(row.get("type")));
            payment.setStatus(trimToNull(row.get("status")));
            records.add(payment);
        }
        paymentRecordRepository.saveAll(records);
        return records.size();
    }

    private List<Map<String, String>> readRows(MultipartFile file) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return rows;
            }
            String[] headers = headerLine.split(",", -1);
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim();
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] values = line.split(",", -1);
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    row.put(headers[i], values[i]);
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal parseAmount(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDateTime parseDateTime(String value, DateTimeFormatter format) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(trimmed, format);
        } catch (Exception e) {
            return null;
        }
    }
}
