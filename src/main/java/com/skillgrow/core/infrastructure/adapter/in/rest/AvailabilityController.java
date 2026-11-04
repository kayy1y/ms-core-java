package com.skillgrow.core.infrastructure.adapter.in.rest;

import com.skillgrow.core.domain.model.AvailabilityRule;
import com.skillgrow.core.domain.model.BlockedDate;
import com.skillgrow.core.domain.port.in.AvailabilityUseCase;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AvailabilityDtos.AvailabilityResponse;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AvailabilityDtos.AvailableDayDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AvailabilityDtos.BlockedDateDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AvailabilityDtos.BlockedDateRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AvailabilityDtos.RuleDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AvailabilityDtos.RuleRequest;
import com.skillgrow.core.infrastructure.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disponibilidad del mentor. /services/{id}/available-days vive aquí (MS1) aunque empiece por
 * /services: el prefijo de la web debe mandarla a /core, no a /dominio.
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "Disponibilidad", description = "Días de atención, fechas bloqueadas y días reservables")
public class AvailabilityController {

    private final AvailabilityUseCase availability;

    public AvailabilityController(AvailabilityUseCase availability) {
        this.availability = availability;
    }

    @GetMapping("/me/availability")
    @Operation(summary = "Mis días de atención y fechas bloqueadas")
    public AvailabilityResponse get(CurrentUser user) {
        var a = availability.get(user.id());
        return new AvailabilityResponse(a.rules().stream().map(AvailabilityController::rule).toList(),
                a.blockedDates().stream().map(AvailabilityController::blocked).toList());
    }

    @PostMapping("/me/availability/rules")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Marcar un día de la semana como disponible")
    public RuleDto addRule(CurrentUser user, @RequestBody RuleRequest body) {
        return rule(availability.addRule(user.id(), body.dayOfWeek(), body.serviceIds()));
    }

    @PutMapping("/me/availability/rules/{id}")
    @Operation(summary = "Cambiar los servicios de un día")
    public RuleDto updateRule(CurrentUser user, @PathVariable String id, @RequestBody RuleRequest body) {
        return rule(availability.updateRule(user.id(), id, body.serviceIds()));
    }

    @DeleteMapping("/me/availability/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quitar un día")
    public void deleteRule(CurrentUser user, @PathVariable String id) {
        availability.deleteRule(user.id(), id);
    }

    @PostMapping("/me/availability/blocked-dates")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Bloquear una fecha")
    public BlockedDateDto addBlocked(CurrentUser user, @RequestBody BlockedDateRequest body) {
        return blocked(availability.addBlockedDate(user.id(), body.date(), body.reason()));
    }

    @DeleteMapping("/me/availability/blocked-dates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desbloquear una fecha")
    public void deleteBlocked(CurrentUser user, @PathVariable String id) {
        availability.deleteBlockedDate(user.id(), id);
    }

    @GetMapping("/services/{id}/available-days")
    @Operation(summary = "Días reservables de un servicio (desde mañana, con cupos)")
    public List<AvailableDayDto> availableDays(@PathVariable String id,
                                               @RequestParam(required = false) String from,
                                               @RequestParam(required = false) String to,
                                               @RequestParam(required = false) String excludeSessionId) {
        return availability.availableDays(id, from, to, excludeSessionId).stream()
                .map(d -> new AvailableDayDto(d.date().toString(), d.remainingSeats()))
                .toList();
    }

    private static RuleDto rule(AvailabilityRule r) {
        return new RuleDto(r.id(), r.dayOfWeek(), r.serviceIds());
    }

    private static BlockedDateDto blocked(BlockedDate b) {
        return new BlockedDateDto(b.id(), b.date().toString(), b.reason());
    }
}
