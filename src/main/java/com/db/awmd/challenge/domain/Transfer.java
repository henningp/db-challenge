package com.db.awmd.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Value
public class Transfer {

    @NotNull
    @NotEmpty
    private final String targetAccountId;

    @NotNull
    @DecimalMin(value = "0", inclusive = false, message = "Amount must be positive.")
    private BigDecimal amount;

    @JsonCreator
    public Transfer(@JsonProperty("targetAccountId") String targetAccountId,
                    @JsonProperty("amount") BigDecimal amount) {
        this.targetAccountId = targetAccountId;
        this.amount = amount;
    }
}
