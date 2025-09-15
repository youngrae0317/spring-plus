package org.example.expert.domain.common.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Getter
public class AuthUser {

    private final Long id;
    private final String email;
    private final String nickname;
    private final UserRole userRole;

    public AuthUser(Long id, String email, String nickname, UserRole userRole) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.userRole = userRole;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(userRole.getUserRole()));
    }
}
