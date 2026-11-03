package com.dis.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DrugStockChangedEvent extends ApplicationEvent {
    private final String drugId;

    public DrugStockChangedEvent(Object source, String drugId) {
        super(source);
        this.drugId = drugId;
    }
}
