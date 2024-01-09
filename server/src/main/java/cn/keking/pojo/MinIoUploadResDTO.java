package cn.keking.pojo;

import java.io.Serializable;

/**
 * @author douwenjie
 * @create 2024-01-09
 */

public class MinIoUploadResDTO implements Serializable {

    private static final long serialVersionUID = 475040120689218785L;
    private String minFileName;
    private String minFileUrl;

    public String getMinFileName() {
        return minFileName;
    }

    public void setMinFileName(String minFileName) {
        this.minFileName = minFileName;
    }

    public String getMinFileUrl() {
        return minFileUrl;
    }

    public void setMinFileUrl(String minFileUrl) {
        this.minFileUrl = minFileUrl;
    }

    public MinIoUploadResDTO(String minFileName, String minFileUrl) {
        this.minFileName = minFileName;
        this.minFileUrl = minFileUrl;
    }

}

