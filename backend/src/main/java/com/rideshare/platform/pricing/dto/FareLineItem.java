package com.rideshare.platform.pricing.dto;

import java.math.BigDecimal;

public record FareLineItem(String calculator, String label, BigDecimal amount) {}
