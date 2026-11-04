package com.skillgrow.core.infrastructure.adapter.in.rest;

import com.skillgrow.core.domain.port.in.ProfileUseCase;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.PublicUserResponse;
import com.skillgrow.core.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@Tag(name = "Usuarios", description = "Perfil público de otros usuarios")
public class UserController {

    private final ProfileUseCase profiles;

    public UserController(ProfileUseCase profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Perfil público (sin correo)")
    public PublicUserResponse get(@PathVariable String id) {
        return UserRestMapper.toPublic(profiles.publicProfile(id));
    }
}
