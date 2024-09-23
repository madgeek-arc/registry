package gr.uoa.di.madgik.registry.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class BatchResult {

    @JsonIgnore
    private String resourceType;

    private boolean droped;

    private String status;

    private long readCount;

    private long writeCount;

    private long readSkipCount;

    private long writeSkipCount;

    public boolean isDroped() {
        return droped;
    }

    public void setDroped(boolean droped) {
        this.droped = droped;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getReadCount() {
        return readCount;
    }

    public void setReadCount(long readCount) {
        this.readCount = readCount;
    }

    public long getWriteCount() {
        return writeCount;
    }

    public void setWriteCount(long writeCount) {
        this.writeCount = writeCount;
    }

    public long getReadSkipCount() {
        return readSkipCount;
    }

    public void setReadSkipCount(long readSkipCount) {
        this.readSkipCount = readSkipCount;
    }

    public long getWriteSkipCount() {
        return writeSkipCount;
    }

    public void setWriteSkipCount(long writeSkipCount) {
        this.writeSkipCount = writeSkipCount;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
