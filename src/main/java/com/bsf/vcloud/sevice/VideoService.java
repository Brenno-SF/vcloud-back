package com.bsf.vcloud.sevice;

import com.bsf.vcloud.adapter.StorageAdapter;
import com.bsf.vcloud.dtos.upload.CompletedUploadDTO;
import com.bsf.vcloud.dtos.upload.PresignedUploadResponse;
import com.bsf.vcloud.dtos.video.VideoRequestDTO;
import com.bsf.vcloud.dtos.video.VideoResponseDTO;
import com.bsf.vcloud.entity.Users;
import com.bsf.vcloud.entity.Video;
import com.bsf.vcloud.enums.VideoStatus;
import com.bsf.vcloud.exceptions.ResourceNotFoundException;
import com.bsf.vcloud.exceptions.StorageException;
import com.bsf.vcloud.exceptions.VideoUploadException;
import com.bsf.vcloud.mapper.VideoMapper;
import com.bsf.vcloud.repository.UserRepository;
import com.bsf.vcloud.repository.VideoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final StorageAdapter storageAdapter;

    public VideoService(VideoRepository videoRepository, UserRepository userRepository, StorageAdapter storageAdapter) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.storageAdapter = storageAdapter;
    }

    @Deprecated
    public VideoResponseDTO uploadVideo(MultipartFile file, UUID  userId)  {

        if (file == null || file.getOriginalFilename() == null || file.getContentType() == null)
            throw new IllegalArgumentException("Invalid file");

        if(!file.getOriginalFilename().endsWith(".mp4") || !"video/mp4".equals(file.getContentType()))
            throw new VideoUploadException("Only .mp4 files are allowed");

        Users user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        Video video = new Video();
        video.setUser(user);
        video.setContentType(file.getContentType());
        video.setOriginalFilename(file.getOriginalFilename());
        video.setSizeBytes(file.getSize());

        Video videoSaved = videoRepository.save(video);

        try{
            String keyName = storageAdapter.uploadVideo(file, videoSaved.getId());
            videoSaved.setKeyName(keyName);
            videoSaved.setStatus(VideoStatus.READY);
            videoRepository.save(videoSaved);

            return new VideoResponseDTO(videoSaved);
        }catch (Exception e){
            videoSaved.setStatus(VideoStatus.FAILED);
            videoRepository.save(videoSaved);
            throw new VideoUploadException("Failed to upload video", e);
        }
    }

    public List<VideoResponseDTO> getVideosByUserId(UUID userId) {

        if(!userRepository.existsById(userId)){
            throw new ResourceNotFoundException("User not found");
        }

        return videoRepository.findByUserId(userId).stream()
                .map(VideoResponseDTO::new)
                .toList();
    }


    public void deleteVideo(String keyName, UUID userId){
        Video video = videoRepository.findByKeyName(keyName).orElseThrow(() -> new ResourceNotFoundException("Video Not Found")) ;
        if (!video.getUser().getId().equals(userId)){
            throw new VideoUploadException("You are not authorized to delete this video");
        }

        try {
            storageAdapter.deleteVideo(keyName);
            videoRepository.delete(video);
        }catch (StorageException e){
            throw new VideoUploadException("Failed to delete video from storage", e);
        }
    }

    @Transactional
    public PresignedUploadResponse uploadSinglePresignedUrl(VideoRequestDTO videoRequestDTO, UUID userId) {

        if (videoRequestDTO == null || videoRequestDTO.contentType() == null || videoRequestDTO.originalFilename() == null)
            throw new IllegalArgumentException("Invalid file");

        if(!videoRequestDTO.originalFilename().endsWith(".mp4") || !"video/mp4".equals(videoRequestDTO.contentType()))
            throw new VideoUploadException("Only .mp4 files are allowed");

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Video video = new Video();

        video.setUser(user);
        video.setOriginalFilename(videoRequestDTO.originalFilename());
        video.setContentType(videoRequestDTO.contentType());
        video.setSizeBytes(videoRequestDTO.sizeBytes());

        videoRepository.save(video);
        try {
            URL url = storageAdapter.generateSinglePresignedUrl(video);
            return new PresignedUploadResponse(video.getId(),url);
        }catch (StorageException e){
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);
            throw e;
        }

    }


    public String multipartUpload(VideoRequestDTO videoRequestDTO, UUID userId)  {

        if (videoRequestDTO == null || videoRequestDTO.contentType() == null || videoRequestDTO.originalFilename() == null)
            throw new IllegalArgumentException("Invalid file");

        if(!videoRequestDTO.originalFilename().endsWith(".mp4") || !"video/mp4".equals(videoRequestDTO.contentType()))
            throw new VideoUploadException("Only .mp4 files are allowed");


        Users user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Video video = VideoMapper.toEntity(videoRequestDTO);
        video.setUser(user);
        videoRepository.save(video);

        String uploadId = storageAdapter.startMultipartUpload(video);
        video.setUploadId(uploadId);

        videoRepository.save(video);

        return uploadId;
    }

    public List<URL> generateMultipartPresignedUrl(String uploadId, Integer partQuantity, VideoRequestDTO videoRequestDTO)  {

        Video video = videoRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

       List<URL> presignedUrls = new ArrayList<>();
       for (int i = 1; i <= partQuantity; i++) {
            presignedUrls.add(storageAdapter.generateMultipartPresignedUrl(uploadId, i, video));
        }

        return presignedUrls;
    }

    @Transactional
    public VideoResponseDTO completeMultipartUpload(CompletedUploadDTO dto)  {
        Video video = videoRepository.findByUploadId(dto.uploadId()).orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        List<CompletedPart> completedParts = dto.parts().stream()
                .map(p -> CompletedPart.builder().partNumber(p.partNumber()).eTag(p.eTag()).build())
                .collect(Collectors.toList());

        try {
            storageAdapter.completeMultipartUpload(dto.uploadId(), video, completedParts);
            video.setStatus(VideoStatus.READY);
            videoRepository.save(video);
            return VideoMapper.toDto(video);
        }catch (StorageException e){
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);
            throw  e;
        }
    }


    @Transactional
    public VideoResponseDTO completeSinglePartUpload(UUID videoId)  {
        Video video = videoRepository.findById(videoId).orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        video.setStatus(VideoStatus.READY);

        videoRepository.save(video);

        return VideoMapper.toDto(video);

    }

    public URL generateDownload(UUID videoId, String keyName, UUID userId) {

        Video video = videoRepository.findById(videoId).orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        if (!video.getUser().getId().equals(userId)){
            throw new VideoUploadException("You are not authorized to download this video");
        }

        if (!video.getKeyName().equals(keyName)){
            throw new VideoUploadException("Key name does not match with video");
        }

        return storageAdapter.generateDownloadUrl(video);

    }
}
