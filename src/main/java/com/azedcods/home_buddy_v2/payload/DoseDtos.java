package com.azedcods.home_buddy_v2.payload;

import com.azedcods.home_buddy_v2.enums.DoseUnit;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public class DoseDtos {

    public record CreateRequest(
            @NotNull Long userId,
            @NotNull Long localMedicationId,
            @NotNull @Min(1) @Max(24) Integer timeFrequency,
            Set<DayOfWeek> daysOfWeek,
            Set<LocalTime> times,
            @NotNull @DecimalMin("0.001") BigDecimal quantityAmount,
            @NotNull DoseUnit quantityUnit,
            LocalDate startDate,
            LocalDate endDate,
            @Size(max = 500) String instructions
    ) {}


    public record UpdateRequest(
            @Min(1) @Max(24) Integer timeFrequency,
            Set<DayOfWeek> daysOfWeek,
            Set<LocalTime> times,
            @DecimalMin("0.001") BigDecimal quantityAmount,
            DoseUnit quantityUnit,
            LocalDate startDate,
            LocalDate endDate,
            @Size(max = 500) String instructions
    ) {}

    public record Response(
            Long id,
            Long userId,
            Long medicationId,
            String medicationName,

            Integer timeFrequency,
            Set<DayOfWeek> daysOfWeek,
            Set<LocalTime> times,

            BigDecimal quantityAmount,
            DoseUnit quantityUnit,

            LocalDate startDate,
            LocalDate endDate,
            String instructions
    ) {}
}
