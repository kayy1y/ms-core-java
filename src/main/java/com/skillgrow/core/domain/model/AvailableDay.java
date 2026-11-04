package com.skillgrow.core.domain.model;

import java.time.LocalDate;

/** Un día reservable con los cupos que le quedan. */
public record AvailableDay(LocalDate date, int remainingSeats) {}
