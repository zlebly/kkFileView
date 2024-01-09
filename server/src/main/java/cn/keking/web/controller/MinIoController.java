package cn.keking.web.controller;

import cn.keking.pojo.Result;
import cn.keking.utils.MinioUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * @author douwenjie
 * @create 2024-01-09
 */

@RestController
public class MinIoController {

    @Resource
    private MinioUtil minioUtil;

    // 存储桶名称
    private static final String MINIO_BUCKET = "test";

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public Result upload(@RequestParam(value = "files") MultipartFile files){
        try {
            return Result.ok(minioUtil.upload(MINIO_BUCKET, files.getName(), files));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
