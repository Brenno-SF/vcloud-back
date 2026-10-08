package com.bsf.vcloud.controller;

import com.bsf.vcloud.dtos.upload.CompletedUploadDTO;
import com.bsf.vcloud.dtos.upload.PresignedUploadResponse;
import com.bsf.vcloud.dtos.video.VideoRequestDTO;
import com.bsf.vcloud.dtos.video.VideoResponseDTO;
import com.bsf.vcloud.sevice.VideoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService){
        this.videoService = videoService;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public VideoResponseDTO uploadVideo(@RequestParam("file") MultipartFile file, @RequestParam("userId") UUID userId)  {
        return videoService.uploadVideo(file, userId);
    }

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public List<VideoResponseDTO> getVideos( @AuthenticationPrincipal Jwt jwt) {
        return videoService.getVideosByUserId(UUID.fromString(jwt.getClaimAsString("userId")));
    }

    @PostMapping("/upload-single-presigned-url")
    @ResponseStatus(HttpStatus.CREATED)
    public PresignedUploadResponse uploadSinglePresignedUrl(@Valid @RequestBody VideoRequestDTO videoRequestDTO, @AuthenticationPrincipal Jwt jwt)  {
        return videoService.uploadSinglePresignedUrl(videoRequestDTO, UUID.fromString(jwt.getClaimAsString("userId")));
    }

    @PostMapping("/start-multipart")
    @ResponseStatus(HttpStatus.OK)
    public String startMultipartUpload(@Valid @RequestBody VideoRequestDTO videoRequestDTO, @AuthenticationPrincipal Jwt jwt)  {
        return videoService.multipartUpload(videoRequestDTO, UUID.fromString(jwt.getClaimAsString("userId")));
    }

    @PostMapping("/generate-multipart-presigned-url/{partNumbers}/{uploadId}")
    @ResponseStatus(HttpStatus.OK)
    public List<URL> multipartUpload(@Valid @RequestBody VideoRequestDTO videoRequestDTO, @PathVariable Integer partNumbers, @PathVariable String uploadId )  {
        return videoService.generateMultipartPresignedUrl(uploadId, partNumbers,videoRequestDTO);
    }

    @PostMapping("/complete-multipart")
    @ResponseStatus(HttpStatus.OK)
    public VideoResponseDTO completeMultipartUpload(@RequestBody CompletedUploadDTO completedUploadDTO)  {
        return videoService.completeMultipartUpload(completedUploadDTO);
    }


    @PostMapping("/complete-singleupload/{videoId}")
    @ResponseStatus(HttpStatus.OK)
    public VideoResponseDTO completeSinglePartUpload(@PathVariable UUID videoId)  {
        return videoService.completeSinglePartUpload(videoId);
    }
}
