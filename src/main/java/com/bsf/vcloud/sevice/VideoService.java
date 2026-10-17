package com.bsf.vcloud.sevice;

import com.bsf.vcloud.adapter.StorageAdapter;
import com.bsf.vcloud.dtos.thumb.ThumbDTO;
import com.bsf.vcloud.dtos.upload.CompletedUploadDTO;
import com.bsf.vcloud.dtos.upload.MultipartUploadResponse;
import com.bsf.vcloud.dtos.upload.PresignedUploadResponse;
import com.bsf.vcloud.dtos.video.VideoRequestDTO;
import com.bsf.vcloud.dtos.video.VideoResponseDTO;
import com.bsf.vcloud.entity.Users;
import com.bsf.vcloud.entity.Video;
import com.bsf.vcloud.enums.VideoStatus;
import com.bsf.vcloud.exceptions.ResourceNotFoundException;
import com.bsf.vcloud.exceptions.StorageException;
import com.bsf.vcloud.exceptions.UnauthorizedActionException;
import com.bsf.vcloud.exceptions.VideoUploadException;
import com.bsf.vcloud.mapper.VideoMapper;
import com.bsf.vcloud.repository.UserRepository;
import com.bsf.vcloud.repository.VideoRepository;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
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

//    @Deprecated
//    public VideoResponseDTO uploadVideo(MultipartFile file, UUID  userId)  {
//
//        if (file == null || file.getOriginalFilename() == null || file.getContentType() == null)
//            throw new IllegalArgumentException("Invalid file");
//
//        if(!file.getOriginalFilename().endsWith(".mp4") || !"video/mp4".equals(file.getContentType()))
//            throw new VideoUploadException("Only .mp4 files are allowed");
//
//        Users user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
//
//        Video video = new Video();
//        video.setUser(user);
//        video.setContentType(file.getContentType());
//        video.setOriginalFilename(file.getOriginalFilename());
//        video.setSizeBytes(file.getSize());
//
//        Video videoSaved = videoRepository.save(video);
//
//        try{
//            String keyName = storageAdapter.uploadVideo(file, videoSaved.getId());
//            videoSaved.setKeyName(keyName);
//            videoSaved.setStatus(VideoStatus.READY);
//            videoRepository.save(videoSaved);
//
//            return new VideoResponseDTO(videoSaved);
//        }catch (Exception e){
//            videoSaved.setStatus(VideoStatus.FAILED);
//            videoRepository.save(videoSaved);
//            throw new VideoUploadException("Failed to upload video", e);
//        }
//    }

    //TODO: Add pagination and sorting
    @Cacheable(cacheNames = "userVideos", key = "#userId")
    public List<VideoResponseDTO> getVideosByUserId(UUID userId) {

        if(!userRepository.existsById(userId)){
            throw new ResourceNotFoundException("User not found");
        }
        List<Video> videos = videoRepository.findByUserId(userId);
        return videos.stream()
                .map(video -> {
                    URL thumbnailUrl = null;

                    if (video.getThumbnailKey() != null) {
                        thumbnailUrl = storageAdapter.generateThumbnailViewUrl(video.getThumbnailKey().toString());
                    }

                    return VideoMapper.toDto(video, thumbnailUrl);
                })
                .toList();
    }

    @CacheEvict(value = "userVideos", key = "#userId")
    public void deleteVideo(UUID videoId, UUID userId){
        Video video = videoRepository.findById(videoId).orElseThrow(() -> new ResourceNotFoundException("Video Not Found")) ;
        if (!video.getUser().getId().equals(userId)){
            throw new UnauthorizedActionException("You are not authorized to delete this video");
        }
        if(!video.getStatus().equals(VideoStatus.READY)){
            videoRepository.delete(video);
            return;
        }
        try {
            storageAdapter.deleteVideo(video.getKeyName());
            videoRepository.delete(video);
        }catch (StorageException e){
            throw new VideoUploadException("Failed to delete video from storage", e);
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
        video.setThumbnailKey("thumbnails/" + video.getId() + ".jpg");

        videoRepository.save(video);

        return uploadId;
    }

    public MultipartUploadResponse generateMultipartPresignedUrl(String uploadId, Integer partQuantity, VideoRequestDTO videoRequestDTO)  {

        Video video = videoRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

       List<URL> presignedUrls = new ArrayList<>();
       for (int i = 1; i <= partQuantity; i++) {
            presignedUrls.add(storageAdapter.generateMultipartPresignedUrl(uploadId, i, video));
        }

        ThumbDTO thumb = new ThumbDTO(video.getThumbnailKey(), videoRequestDTO.thumb().contentType(), videoRequestDTO.thumb().sizeBytes());
        return new MultipartUploadResponse(storageAdapter.generateSinglePresignedUrl(thumb), presignedUrls);
    }

    @Transactional
    @CacheEvict(value = "userVideos", key = "#userId")
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
            video.setThumbnailKey(null);
            videoRepository.save(video);
            throw e;
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
        video.setThumbnailKey("thumbnails/" + video.getId() + ".jpg");
        ThumbDTO thumb = new ThumbDTO(video.getThumbnailKey(), videoRequestDTO.thumb().contentType(), videoRequestDTO.thumb().sizeBytes());
        try {
            URL urlVideo = storageAdapter.generateSinglePresignedUrl(video);
            URL urlThumb = storageAdapter.generateSinglePresignedUrl(thumb);
            videoRepository.save(video);
            return new PresignedUploadResponse(video.getId(),urlVideo, urlThumb);
        }catch (StorageException e){
            video.setStatus(VideoStatus.FAILED);
            video.setThumbnailKey(null);
            videoRepository.save(video);
            throw e;
        }
    }

    @Transactional
    @CacheEvict(value = "userVideos", key = "#userId")
    public VideoResponseDTO completeSinglePartUpload(UUID videoId)  {
        Video video = videoRepository.findById(videoId).orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        video.setStatus(VideoStatus.READY);

        videoRepository.save(video);

        return VideoMapper.toDto(video);

    }

    public URL generateDownload(UUID videoId, UUID userId) {

        Video video = videoRepository.findById(videoId).orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        if (!video.getUser().getId().equals(userId)){
            throw new VideoUploadException("You are not authorized to download this video");
        }

        if(!video.getStatus().equals(VideoStatus.READY)){
            throw new VideoUploadException("The video are not upload successfully");
        }

//        if (!video.getKeyName().equals(keyName)){
//            throw new VideoUploadException("Key name does not match with video");
//        }

        return storageAdapter.generateDownloadUrl(video);

    }
}
