package com.groupName.artefactName.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String token;
    private UserDTO usuario;
    private List<String> roles;
    private List<String> permisos;
}
