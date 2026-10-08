package com.bsf.vcloud.mapper;

import com.bsf.vcloud.dtos.video.VideoRequestDTO;
import com.bsf.vcloud.dtos.video.VideoResponseDTO;
import com.bsf.vcloud.entity.Video;
import org.springframework.stereotype.Component;

@Component
public class VideoMapper {

    public static Video toEntity(VideoRequestDTO dto) {
        Video video = new Video();
        video.setOriginalFilename(dto.originalFilename());
        video.setContentType(dto.contentType());
        video.setSizeBytes(dto.sizeBytes());
        return video;
    }

    public static VideoResponseDTO toDto(Video video){
        return new VideoResponseDTO(video);
    }
}
