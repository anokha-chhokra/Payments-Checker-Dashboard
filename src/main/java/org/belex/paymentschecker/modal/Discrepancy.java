package org.belex.paymentschecker.modal;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
public class Discrepancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerId;

    //Type of discrepancy
    private String type;

    private String orderId;
    private String paymentRef;

    @Column(length = 1000)
    private String description;

    private BigDecimal amountAtRisk;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmountAtRisk() {
        return amountAtRisk;
    }

    public void setAmountAtRisk(BigDecimal amountAtRisk) {
        this.amountAtRisk = amountAtRisk;
    }
}
