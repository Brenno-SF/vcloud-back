package com.bsf.vcloud.dtos.upload;

import java.net.URL;
import java.util.List;

public record MultipartUploadResponse(
        URL thumbUploadUrl,
        List<URL> presignedUrls
) {
}
