package com.bsf.vcloud.sevice;

import com.bsf.vcloud.dtos.user.UserRequestDTO;
import com.bsf.vcloud.dtos.user.UserResponseDTO;
import com.bsf.vcloud.entity.Users;
import com.bsf.vcloud.exceptions.BusinessRuleException;
import com.bsf.vcloud.exceptions.ResourceNotFoundException;
import com.bsf.vcloud.exceptions.UnauthorizedActionException;
import com.bsf.vcloud.exceptions.VideoUploadException;
import com.bsf.vcloud.mapper.UserMapper;
import com.bsf.vcloud.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO requestDTO) {

        if (userRepository.existsByEmail((requestDTO.email()))){
            throw new BusinessRuleException("Email already in use");
        }
        Users user  = UserMapper.toEntity(requestDTO);
        user.setUsername(requestDTO.username());
        String encryptedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encryptedPassword);

        user = userRepository.save(user);

        return UserMapper.toDto(user);
    }



    @Transactional
    public UserResponseDTO getById(UUID id) {
        return UserMapper.toDto(userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("User not found")));
    }
    @Transactional
    public List<UserResponseDTO> getAll(){
        return userRepository.findAll().stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }


    public UserResponseDTO update(UUID id, UserResponseDTO UserResponseDTO, UUID authUserId) {
        if (!id.equals(authUserId)) {
            throw new UnauthorizedActionException("You can only update your own user data");
        }
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userRepository.existsByEmail(UserResponseDTO.email())) {
            throw new BusinessRuleException("Email already in use");
        }

        user.setEmail(UserResponseDTO.email());
        user.setUsername(UserResponseDTO.username());
        user.setPassword(UserResponseDTO.password());


        Users userSaved = userRepository.save(user);

        return UserMapper.toDto(userSaved);

    }

    public void deleteById(UUID id, UUID authUserId) {
        if (!id.equals(authUserId)) {
            throw new UnauthorizedActionException("You can only delete your own user");
        }
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userRepository.delete(user);
    }
}
