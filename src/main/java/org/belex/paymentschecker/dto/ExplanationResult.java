package org.belex.paymentschecker.dto;

public class ExplanationResult {

    private String explanation;
    private String recommendedAction;
    private String error;

    public static ExplanationResult ok(String explanation, String recommendedAction) {
        ExplanationResult r = new ExplanationResult();
        r.explanation = explanation;
        r.recommendedAction = recommendedAction;
        return r;
    }

    public static ExplanationResult failure(String error) {
        ExplanationResult r = new ExplanationResult();
        r.error = error;
        return r;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public String getError() {
        return error;
    }
}
