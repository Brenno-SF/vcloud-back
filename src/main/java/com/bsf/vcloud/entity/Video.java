package com.bsf.vcloud.entity;

import com.bsf.vcloud.enums.VideoStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Generated;

import org.hibernate.generator.EventType;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "videos")
public class Video {

    @Id
    @Generated(event = EventType.INSERT)
    @Column(columnDefinition = "UUID DEFAULT uuidv7()", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private VideoStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "s3_key")
    private String keyName;

    @Column(name = "upload_id")
    private String uploadId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = VideoStatus.UPLOADING;
    }

    @PreUpdate
    void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

    public String getPath(){
        return  this.keyName = this.id + "/" + this.originalFilename;
    }
}
