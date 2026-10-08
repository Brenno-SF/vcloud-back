package com.bsf.vcloud.mapper;


import com.bsf.vcloud.dtos.user.UserRequestDTO;
import com.bsf.vcloud.dtos.user.UserResponseDTO;
import com.bsf.vcloud.entity.Users;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public static Users toEntity(UserRequestDTO dto) {
        Users user = new Users();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPassword(dto.password());
        return user;
    }

    public static UserResponseDTO toDto(Users user){
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getCreatedAt()
        );
    }
}
