package org.belex.paymentschecker.service;

import org.belex.paymentschecker.modal.Discrepancy;
import org.belex.paymentschecker.modal.OrderRecord;
import org.belex.paymentschecker.modal.PaymentRecord;
import org.belex.paymentschecker.repo.DiscrepancyRepository;
import org.belex.paymentschecker.repo.OrderRecordRepository;
import org.belex.paymentschecker.repo.PaymentRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReconciliationService {

    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    private final OrderRecordRepository orderRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final DiscrepancyRepository discrepancyRepository;

    public ReconciliationService(OrderRecordRepository orderRecordRepository,
                                  PaymentRecordRepository paymentRecordRepository,
                                  DiscrepancyRepository discrepancyRepository) {
        this.orderRecordRepository = orderRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.discrepancyRepository = discrepancyRepository;
    }

    public void reconcile(Long ownerId) {
        discrepancyRepository.deleteByOwnerId(ownerId);

        List<OrderRecord> orders = orderRecordRepository.findByOwnerId(ownerId);
        List<PaymentRecord> payments = paymentRecordRepository.findByOwnerId(ownerId);

        Map<String, List<OrderRecord>> ordersByKey = groupOrders(orders);
        Map<String, List<PaymentRecord>> paymentsByKey = groupPayments(payments);

        List<Discrepancy> found = new ArrayList<>();

        for (Map.Entry<String, List<OrderRecord>> entry : ordersByKey.entrySet()) {
            String key = entry.getKey();
            List<OrderRecord> group = entry.getValue();
            OrderRecord order = group.get(0);

            if (group.size() > 1) {
                found.add(build(ownerId, "DUPLICATE_ORDER", order.getOrderId(), null,
                        group.size() + " rows found in orders.csv for " + order.getOrderId()
                                + " - the store's own export lists this order more than once.",
                        nullSafe(order.getNetAmount()).multiply(BigDecimal.valueOf(group.size() - 1))));
            }

            List<PaymentRecord> matches = paymentsByKey.get(key);
            evaluateOrder(ownerId, order, matches, found);
        }

        for (Map.Entry<String, List<PaymentRecord>> entry : paymentsByKey.entrySet()) {
            if (ordersByKey.containsKey(entry.getKey())) {
                continue;
            }
            for (PaymentRecord orphan : entry.getValue()) {
                if ("failed".equalsIgnoreCase(orphan.getStatus())) {
                    continue;
                }
                found.add(build(ownerId, "MISSING_ORDER", orphan.getOrderReference(), orphan.getTransactionRef(),
                        "Payment " + orphan.getTransactionRef() + " references order "
                                + orphan.getOrderReference() + " which does not exist in orders.csv.",
                        nullSafe(orphan.getAmount())));
            }
        }

        discrepancyRepository.saveAll(found);
    }

    private void evaluateOrder(Long ownerId, OrderRecord order, List<PaymentRecord> matches,
                                List<Discrepancy> found) {
        boolean completed = "completed".equalsIgnoreCase(order.getStatus());
        boolean cancelledOrRefunded = "cancelled".equalsIgnoreCase(order.getStatus())
                || "refunded".equalsIgnoreCase(order.getStatus());

        if (matches == null || matches.isEmpty()) {
            if (completed) {
                found.add(build(ownerId, "MISSING_PAYMENT", order.getOrderId(), null,
                        "Order " + order.getOrderId() + " is marked completed but no payment for it "
                                + "was found in payments.csv.",
                        nullSafe(order.getNetAmount())));
            }
            return;
        }

        List<PaymentRecord> settledCharges = new ArrayList<>();
        List<PaymentRecord> refunds = new ArrayList<>();
        List<PaymentRecord> other = new ArrayList<>();
        for (PaymentRecord p : matches) {
            if ("charge".equalsIgnoreCase(p.getType()) && "settled".equalsIgnoreCase(p.getStatus())) {
                settledCharges.add(p);
            } else if ("refund".equalsIgnoreCase(p.getType())) {
                refunds.add(p);
            } else {
                other.add(p);
            }
        }

        if (settledCharges.size() > 1) {
            BigDecimal extra = BigDecimal.ZERO;
            StringBuilder refs = new StringBuilder();
            for (int i = 1; i < settledCharges.size(); i++) {
                extra = extra.add(nullSafe(settledCharges.get(i).getAmount()));
                if (refs.length() > 0) refs.append(", ");
                refs.append(settledCharges.get(i).getTransactionRef());
            }
            found.add(build(ownerId, "DUPLICATE_PAYMENT", order.getOrderId(), settledCharges.get(0).getTransactionRef(),
                    "Order " + order.getOrderId() + " was charged " + settledCharges.size()
                            + " times (extra transactions: " + refs + "). The customer was likely overcharged.",
                    extra));
        }

        if (settledCharges.isEmpty()) {
            if (completed) {
                if (!other.isEmpty()) {
                    PaymentRecord p = other.get(0);
                    found.add(build(ownerId, "STATUS_MISMATCH", order.getOrderId(), p.getTransactionRef(),
                            "Order " + order.getOrderId() + " is marked completed but its payment "
                                    + p.getTransactionRef() + " has status '" + p.getStatus() + "', not settled.",
                            nullSafe(order.getNetAmount())));
                } else if (!refunds.isEmpty()) {
                    found.add(build(ownerId, "STATUS_MISMATCH", order.getOrderId(), refunds.get(0).getTransactionRef(),
                            "Order " + order.getOrderId() + " is marked completed but only a refund was found, "
                                    + "with no original charge on file.",
                            nullSafe(order.getNetAmount())));
                }
            }
            return;
        }

        PaymentRecord payment = settledCharges.get(0);

        BigDecimal diff = nullSafe(order.getNetAmount()).subtract(nullSafe(payment.getAmount())).abs();
        if (diff.compareTo(AMOUNT_TOLERANCE) > 0) {
            found.add(build(ownerId, "AMOUNT_MISMATCH", order.getOrderId(), payment.getTransactionRef(),
                    "Order " + order.getOrderId() + " net amount is " + order.getNetAmount()
                            + " but payment " + payment.getTransactionRef() + " charged " + payment.getAmount() + ".",
                    diff));
        }

        if (order.getCurrency() != null && payment.getCurrency() != null
                && !order.getCurrency().equalsIgnoreCase(payment.getCurrency())) {
            found.add(build(ownerId, "CURRENCY_MISMATCH", order.getOrderId(), payment.getTransactionRef(),
                    "Order " + order.getOrderId() + " was placed in " + order.getCurrency()
                            + " but charged in " + payment.getCurrency() + ".",
                    nullSafe(order.getNetAmount())));
        }

        if (cancelledOrRefunded && refunds.isEmpty()) {
            found.add(build(ownerId, "STATUS_MISMATCH", order.getOrderId(), payment.getTransactionRef(),
                    "Order " + order.getOrderId() + " is marked '" + order.getStatus()
                            + "' but a settled charge (" + payment.getTransactionRef()
                            + ") exists with no refund on file.",
                    nullSafe(payment.getAmount())));
        }
    }

    private Discrepancy build(Long ownerId, String type, String orderId, String paymentRef,
                               String description, BigDecimal amountAtRisk) {
        Discrepancy d = new Discrepancy();
        d.setOwnerId(ownerId);
        d.setType(type);
        d.setOrderId(orderId);
        d.setPaymentRef(paymentRef);
        d.setDescription(description);
        d.setAmountAtRisk(amountAtRisk == null ? BigDecimal.ZERO : amountAtRisk);
        return d;
    }

    private Map<String, List<OrderRecord>> groupOrders(List<OrderRecord> orders) {
        Map<String, List<OrderRecord>> map = new LinkedHashMap<>();
        for (OrderRecord o : orders) {
            String key = normalize(o.getOrderId());
            if (key == null) continue;
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(o);
        }
        return map;
    }

    private Map<String, List<PaymentRecord>> groupPayments(List<PaymentRecord> payments) {
        Map<String, List<PaymentRecord>> map = new LinkedHashMap<>();
        for (PaymentRecord p : payments) {
            String key = normalize(p.getOrderReference());
            if (key == null) continue;
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }
        return map;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
