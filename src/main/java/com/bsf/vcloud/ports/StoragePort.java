package com.bsf.vcloud.ports;

import com.bsf.vcloud.entity.Video;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

public interface StoragePort {
    URL generateSinglePresignedUrl(Video video) ;
    String uploadVideo(MultipartFile file, UUID videoId) ;
    String startMultipartUpload(Video video) ;
    URL generateMultipartPresignedUrl(String uploadId, Integer partNumber, Video video) ;
    void completeMultipartUpload(String uploadId, Video video, List<CompletedPart> completedParts) ;
}
