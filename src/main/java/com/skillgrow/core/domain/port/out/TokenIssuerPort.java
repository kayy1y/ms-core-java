package com.skillgrow.core.domain.port.out;

import com.skillgrow.core.domain.model.User;

/** Emite el JWT de sesión que después valida APIM. */
public interface TokenIssuerPort {

    String issue(User user);
}
