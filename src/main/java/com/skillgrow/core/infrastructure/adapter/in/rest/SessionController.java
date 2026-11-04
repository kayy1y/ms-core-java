package com.skillgrow.core.infrastructure.adapter.in.rest;

import com.skillgrow.core.domain.model.AcceptInput;
import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.Platform;
import com.skillgrow.core.domain.model.SessionDetails;
import com.skillgrow.core.domain.model.WireEnum;
import com.skillgrow.core.domain.port.in.SessionCommandUseCase;
import com.skillgrow.core.domain.port.in.SessionCommandUseCase.CreateSessionCommand;
import com.skillgrow.core.domain.port.in.SessionQueryUseCase;
import com.skillgrow.core.domain.port.in.SessionQueryUseCase.SessionListQuery;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.PageResponse;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.SessionDtos.AcceptRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.SessionDtos.CommentRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.SessionDtos.CreateSessionRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.SessionDtos.LocationDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.SessionDtos.MeetingRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.SessionDtos.RescheduleRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.SessionDtos.SessionResponse;
import com.skillgrow.core.infrastructure.adapter.in.rest.mapper.SessionRestMapper;
import com.skillgrow.core.infrastructure.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sesiones de mentoría. Cada PATCH es una transición de la máquina de estados. */
@RestController
@RequestMapping("/v1")
@Tag(name = "Sesiones", description = "Reservar, aceptar, rechazar, reprogramar, cancelar y completar")
public class SessionController {

    private final SessionCommandUseCase commands;
    private final SessionQueryUseCase queries;

    public SessionController(SessionCommandUseCase commands, SessionQueryUseCase queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @GetMapping("/sessions")
    @Operation(summary = "Mis sesiones (role=learner|mentor|all, scope=upcoming|past|cancelled)")
    public PageResponse<SessionResponse> list(CurrentUser user,
                                              @RequestParam(required = false) String role,
                                              @RequestParam(required = false) String scope,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String search,
                                              @RequestParam(required = false) String sort,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        var result = queries.list(user.id(), new SessionListQuery(role, scope, status, search, sort),
                new PageQuery(page, pageSize));
        return PageResponse.of(result, d -> SessionRestMapper.toResponse(d, user.id()));
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Detalle de una sesión (solo participantes)")
    public SessionResponse get(CurrentUser user, @PathVariable String id) {
        return view(queries.get(user.id(), id), user);
    }

    @GetMapping("/me/requests")
    @Operation(summary = "Solicitudes que esperan mi respuesta (kind=received) o que envié (kind=sent)")
    public PageResponse<SessionResponse> requests(CurrentUser user,
                                                  @RequestParam(required = false) String kind,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int pageSize) {
        return PageResponse.of(queries.requests(user.id(), kind, new PageQuery(page, pageSize)),
                d -> SessionRestMapper.toResponse(d, user.id()));
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reservar un día (publica SESSION_REQUESTED al mentor)")
    public SessionResponse create(CurrentUser user, @RequestBody CreateSessionRequest body) {
        return view(commands.create(user.id(),
                new CreateSessionCommand(body.serviceId(), body.date(), body.modality(), body.learnerNote())), user);
    }

    @PatchMapping("/sessions/{id}/accept")
    @Operation(summary = "Aceptar (mentor) o confirmar el nuevo día (la otra parte)")
    public SessionResponse accept(CurrentUser user, @PathVariable String id,
                                  @RequestBody(required = false) AcceptRequest body) {
        AcceptInput input = body == null ? AcceptInput.empty() : new AcceptInput(
                WireEnum.find(Platform.class, body.platform()).orElse(null), body.otherPlatformName(),
                body.meetingLink(),
                loc(body.location(), LocationDto::name), loc(body.location(), LocationDto::address),
                loc(body.location(), LocationDto::notes));
        return view(commands.accept(user.id(), id, input), user);
    }

    @PatchMapping("/sessions/{id}/reject")
    @Operation(summary = "Rechazar una solicitud pendiente (mentor)")
    public SessionResponse reject(CurrentUser user, @PathVariable String id,
                                  @RequestBody(required = false) CommentRequest body) {
        return view(commands.reject(user.id(), id, body == null ? null : body.comment()), user);
    }

    @PatchMapping("/sessions/{id}/reschedule")
    @Operation(summary = "Proponer otro día")
    public SessionResponse reschedule(CurrentUser user, @PathVariable String id,
                                      @RequestBody(required = false) RescheduleRequest body) {
        return view(commands.reschedule(user.id(), id, body == null ? null : body.date(),
                body == null ? null : body.comment()), user);
    }

    @PatchMapping("/sessions/{id}/cancel")
    @Operation(summary = "Cancelar")
    public SessionResponse cancel(CurrentUser user, @PathVariable String id,
                                  @RequestBody(required = false) CommentRequest body) {
        return view(commands.cancel(user.id(), id, body == null ? null : body.comment()), user);
    }

    @PatchMapping("/sessions/{id}/complete")
    @Operation(summary = "Marcar como completada (mentor, desde el día de la sesión)")
    public SessionResponse complete(CurrentUser user, @PathVariable String id) {
        return view(commands.complete(user.id(), id), user);
    }

    @PutMapping("/sessions/{id}/meeting")
    @Operation(summary = "Cambiar el enlace de una sesión virtual")
    public SessionResponse meeting(CurrentUser user, @PathVariable String id, @RequestBody MeetingRequest body) {
        return view(commands.updateMeeting(user.id(), id, body.platform(), body.otherPlatformName(),
                body.meetingLink()), user);
    }

    @PutMapping("/sessions/{id}/location")
    @Operation(summary = "Cambiar el lugar de una sesión presencial")
    public SessionResponse location(CurrentUser user, @PathVariable String id, @RequestBody LocationDto body) {
        return view(commands.updateLocation(user.id(), id, body.name(), body.address(), body.notes()), user);
    }

    private static SessionResponse view(SessionDetails d, CurrentUser user) {
        return SessionRestMapper.toResponse(d, user.id());
    }

    private static String loc(LocationDto l, Function<LocationDto, String> f) {
        return l == null ? null : f.apply(l);
    }
}
