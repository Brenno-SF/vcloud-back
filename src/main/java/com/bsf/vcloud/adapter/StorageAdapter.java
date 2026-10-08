package com.bsf.vcloud.adapter;

import com.bsf.vcloud.entity.Video;
import com.bsf.vcloud.exceptions.StorageException;
import com.bsf.vcloud.exceptions.VideoUploadException;
import com.bsf.vcloud.ports.StoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

//Todo aplicar ListPartsRequest
@Slf4j
@Component
public class StorageAdapter implements StoragePort {

    private final String bucketName;
    private final S3Client s3Client;

    private final S3Presigner s3Presigner;

    public StorageAdapter(@Value("${aws.s3.bucket-name}") String bucketName,
                          @Value("${aws.s3.region}") String region) {
        this.bucketName = bucketName;
        s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();

        s3Presigner = S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public URL generateSinglePresignedUrl(Video video) {

        try{
            AwsRequestOverrideConfiguration overrideConfig = AwsRequestOverrideConfiguration.builder()
                    .putRawQueryParameter("x-amz-acl", "public-read")
                    .build();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(video.getPath())
                    .contentType(video.getContentType())
                    .contentLength(video.getSizeBytes())
                    .build();


            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(java.time.Duration.ofMinutes(15))
                    .putObjectRequest(putObjectRequest)
                    .build();

            return s3Presigner.presignPutObject(presignRequest).url();
        }catch(SdkException e){
            log.error("Failed to generate presigned URL for video {}: {}", video.getId(), e.getMessage());
            throw new StorageException("Failed to generate presigned URL", e);
        }
    }

    @Override
    public String startMultipartUpload(Video video) {

        try{
            CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(video.getPath())
                    .contentType(video.getContentType())
                    .build();

            CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
            return createResponse.uploadId();
        }catch (S3Exception e){
            log.error("S3 Failed to start multipart upload for video {}: {}", video.getId(), e.getMessage());
            throw new StorageException("Failed to start multipart upload", e);
        }catch (SdkClientException  e){
            log.error("AWS SDK failed to start multipart upload for video {}: {}", video.getId(), e.getMessage());
            throw new StorageException("Failed to start multipart upload", e);
        }
    }

    @Override
    public URL generateMultipartPresignedUrl(String uploadId, Integer partNumber, Video video){

        try{//monta o request
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .key(video.getPath())
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();

            UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60))
                    .uploadPartRequest(uploadPartRequest)
                    .build();

            PresignedUploadPartRequest presignedUploadPartRequest = s3Presigner.presignUploadPart(presignRequest);

            return presignedUploadPartRequest.url();
        }catch (SdkException e){
            log.error("Failed to generate presigned URL for part {} of video {}: {}", partNumber, video.getId(), e.getMessage());
            throw new StorageException("Failed to generate presigned URL for multipart upload", e);
        }
    }

    @Override
    public void completeMultipartUpload(String uploadId, Video video, List<CompletedPart> completedParts) {

        try{
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(video.getPath())
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                    .build();

            s3Client.completeMultipartUpload(completeRequest);
        }catch (NoSuchUploadException e) {

            log.error(
                    "Multipart upload does not exist. " +
                            "videoId={}, uploadId={}, key={}, requestId={}",
                    video.getId(),
                    uploadId,
                    video.getPath(),
                    e.requestId(),
                    e
            );

            throw new StorageException(
                    "Multipart upload does not exist or has already been completed",
                    e
            );

        } catch (S3Exception e) {

            log.error( "S3 failed to complete multipart upload. " +"videoId={}, uploadId={}, key={}, status={}, errorCode={}, requestId={}",
                    video.getId(), uploadId,video.getPath(),e.statusCode(),e.awsErrorDetails().errorCode(),e.requestId(),e);

            throw new StorageException("Failed to complete multipart upload",e);

        } catch (SdkClientException e) {

            log.error("AWS SDK failed to complete multipart upload. " +"videoId={}, uploadId={}, key={}",video.getId(),uploadId,video.getPath(),e);

            throw new StorageException("Unable to communicate with S3",e);
        }
    }

    @Override
    public String uploadVideo(MultipartFile file, UUID videoId) {

        String keyName = "videos/" + videoId + "/original";

        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();

        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();

        List<CompletedPart> completedParts = new ArrayList<>();
        // Upload partes (chunks)
        int partNumber = 1;
        int partSize = 10 * 1024 * 1024; // 10MB por parte

        try(InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[partSize];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] partData = Arrays.copyOf(buffer, bytesRead);

                //monta o request
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucketName)
                        .key(keyName)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .build();
                //manda o request com a parte do vídeo e recebe o eTag
                UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(partData));
                //pra avisar ao s3 pra montar o arquivo no final
                completedParts.add(CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(uploadPartResponse.eTag())
                        .build());

                partNumber++;
            }
            //monta o request completo
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                    .build();
            //envia o request completo
            s3Client.completeMultipartUpload(completeRequest);
            return keyName;
        }catch (Exception e){//todo tirar isso
            abortMultipartUpload(uploadId, keyName);
            throw new VideoUploadException("Failed upload video to S3", e);
        }
    }

    public void deleteVideo(String keyName) {

        try{
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        }catch (S3Exception e) {

            log.error("S3 failed to delete object. bucket={}, key={}, " +"status={}, errorCode={}, requestId={}",bucketName,keyName,e.statusCode(),
                    e.awsErrorDetails().errorCode(),e.requestId(),e);

            throw new StorageException("Failed to delete object from storage",e);

        } catch (SdkClientException e) {

            log.error("AWS SDK failed to delete object. bucket={}, key={}",bucketName, keyName,e);

            throw new StorageException("Unable to communicate with storage",e);
        }
    }

    private void abortMultipartUpload(String uploadId, String keyName) {
        AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                .bucket(bucketName)
                .uploadId(uploadId)
                .key(keyName)
                .build();
        s3Client.abortMultipartUpload(abortRequest);
    }

    //return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, keyName);
}
