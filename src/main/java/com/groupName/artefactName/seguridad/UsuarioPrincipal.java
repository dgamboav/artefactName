package com.groupName.artefactName.seguridad;

import com.groupName.artefactName.entidad.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String correo;
    private final String password;
    private final String denomination;
    private final String type;
    private final boolean status;
    private final Collection<? extends GrantedAuthority> authorities;

    public UsuarioPrincipal(User user, Collection<? extends GrantedAuthority> authorities) {
        this.id = user.getId();
        this.correo = user.getCorreo();
        this.password = user.getPassword();
        this.denomination = user.getDenomination();
        this.type = user.getType();
        this.status = Boolean.TRUE.equals(user.getStatus());
        this.authorities = authorities;
    }

    @Override
    public String getUsername() {
        return correo; // se usa como "username" en Spring
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
