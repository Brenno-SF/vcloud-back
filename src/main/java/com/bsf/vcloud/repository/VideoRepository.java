package com.bsf.vcloud.repository;

import com.bsf.vcloud.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID>, JpaSpecificationExecutor<Video> {
    Optional<Video> findByUploadId(String uploadId);
    Optional<Video> findByKeyName(String keyName);
}
