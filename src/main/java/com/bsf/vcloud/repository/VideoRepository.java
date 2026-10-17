package com.bsf.vcloud.repository;

import com.bsf.vcloud.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {
    Page<Video> findByUserId(UUID userId, Pageable pageable);
    Optional<Video> findByUploadId(String uploadId);
    Optional<Video> findByKeyName(String keyName);
}
