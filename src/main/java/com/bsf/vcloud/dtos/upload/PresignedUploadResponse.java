package com.bsf.vcloud.dtos.upload;

import java.net.URL;
import java.util.UUID;

public record PresignedUploadResponse(
        UUID videoId,
        URL uploadUrl
) {
}
