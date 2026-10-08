package com.bsf.vcloud.repository;

import com.bsf.vcloud.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {
    List<Video> findByUserId(UUID userId);
    Optional<Video> findByUploadId(String uploadId);
    Optional<Video> findByKeyName(String keyName);
}
