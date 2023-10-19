package cn.keking.service.impl;

import cn.keking.model.FileAttribute;
import cn.keking.service.FileHandlerService;
import cn.keking.utils.KkFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kl
 * @date 2018/1/17
 * Content :图片文件处理
 */
@Service
public class PictureFilePreviewImpl extends CommonPreviewImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(PictureFilePreviewImpl.class);
    private final FileHandlerService fileHandlerService;
    private final OtherFilePreviewImpl otherFilePreview;

    public PictureFilePreviewImpl(FileHandlerService fileHandlerService, OtherFilePreviewImpl otherFilePreview) {
        super(fileHandlerService, otherFilePreview);
        this.fileHandlerService = fileHandlerService;
        this.otherFilePreview = otherFilePreview;
    }

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        LOGGER.debug("filePreviewHandle, url:[{}]", url);
        url= KkFileUtils.htmlEscape(url);
        List<String> imgUrls = new ArrayList<>();
        imgUrls.add(url);
        String fileKey = fileAttribute.getFileKey();
        List<String> zipImgUrls = fileHandlerService.getImgCache(fileKey);
        if (!CollectionUtils.isEmpty(zipImgUrls)) {
            imgUrls.addAll(zipImgUrls);
        }
        // 不是http开头，浏览器不能直接访问，需下载到本地
        super.filePreviewHandle(url, model, fileAttribute);
        model.addAttribute("imgUrls", imgUrls);
        return PICTURE_FILE_PREVIEW_PAGE;
    }
}
