package gr.uoa.di.madgik.registry.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class BatchResult {

    @JsonIgnore
    private String resourceType;

    private boolean droped;

    private String status;

    private int readCount;

    private int writeCount;

    private int readSkipCount;

    private int writeSkipCount;

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

    public int getReadCount() {
        return readCount;
    }

    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }

    public int getWriteCount() {
        return writeCount;
    }

    public void setWriteCount(int writeCount) {
        this.writeCount = writeCount;
    }

    public int getReadSkipCount() {
        return readSkipCount;
    }

    public void setReadSkipCount(int readSkipCount) {
        this.readSkipCount = readSkipCount;
    }

    public int getWriteSkipCount() {
        return writeSkipCount;
    }

    public void setWriteSkipCount(int writeSkipCount) {
        this.writeSkipCount = writeSkipCount;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
}
