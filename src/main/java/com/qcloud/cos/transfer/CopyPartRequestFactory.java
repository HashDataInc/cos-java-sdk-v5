package com.qcloud.cos.transfer;

import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.CopyPartRequest;

/**
 * Factory for creating all the individual CopyPartRequest objects for a multipart copy.
 * <p>
 * This allows us to delay creating each CopyPartRequest until we're ready for it, instead of
 * immediately creating thousands of CopyPartRequest objects for each large copy, when we won't need
 * most of those request objects for a while.
 */
public class CopyPartRequestFactory {

    /** Upload id to be used in each copy part request. */
    private final String uploadId;
    /** Optimal size of each part in the copy request. */
    private final long optimalPartSize;
    /** The original copy object request. */
    private final CopyObjectRequest origReq;
    /** Part Number to be specified in each copy part request. */
    private int partNumber = 1;
    /** Starting byte for each part. */
    private long offset = 0;
    /** The number of remaining bytes to be copied. */
    private long remainingBytes;

    public CopyPartRequestFactory(CopyObjectRequest origReq, String uploadId, long optimalPartSize,
            long contentLength) {
        this.origReq = origReq;
        this.uploadId = uploadId;
        this.optimalPartSize = optimalPartSize;
        this.remainingBytes = contentLength;
    }

    public synchronized boolean hasMoreRequests() {
        return (remainingBytes > 0);
    }

    /**
     * Constructs a copy part requests and returns it.
     *
     * @return Returns a new copy part request
     */
    public synchronized CopyPartRequest getNextCopyPartRequest() {
        final long partSize = Math.min(optimalPartSize, remainingBytes);

        CopyPartRequest req = new CopyPartRequest().withSourceAppid(origReq.getSourceAppid())
                .withSourceBucketRegion(origReq.getSourceBucketRegion())
                .withSourceEndpointSuffix(origReq.getSourceEndpointSuffix())
                .withSourceBucketName(origReq.getSourceBucketName())
                .withSourceKey(origReq.getSourceKey()).withUploadId(uploadId)
                .withPartNumber(partNumber++)
                .withDestinationBucketName(origReq.getDestinationBucketName())
                .withDestinationKey(origReq.getDestinationKey())
                .withSourceVersionId(origReq.getSourceVersionId())
                .withFirstByte(Long.valueOf(offset))
                .withLastByte(Long.valueOf(offset + partSize - 1))
                // other meta data
                .withMatchingETagConstraints(origReq.getMatchingETagConstraints())
                .withModifiedSinceConstraint(origReq.getModifiedSinceConstraint())
                .withNonmatchingETagConstraints(origReq.getNonmatchingETagConstraints())
                .withSourceVersionId(origReq.getSourceVersionId())
                .withUnmodifiedSinceConstraint(origReq.getUnmodifiedSinceConstraint())
                // general meta data
                .withGeneralProgressListener(origReq.getGeneralProgressListener());
        offset += partSize;
        remainingBytes -= partSize;
        return req;
    }
}
