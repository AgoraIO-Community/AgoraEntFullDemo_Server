package com.md.service.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class UploadFile {

    @Value("${al.image.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${al.image.oss.endpoint}")
    private String endpoint;

    @Value("${al.image.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${al.image.oss.bucketName}")
    private String bucketName;

    @Value("${al.image.oss.objectName.url}")
    private String objectNameUrl;

    @Value("${al.image.oss.objectNameCheck.url}")
    private String objectNameCheck;


    @Value("${al.log.oss.accessKeyId}")
    private String logAccessKeyId;

    @Value("${al.log.oss.endpoint}")
    private String logEndpoint;

    @Value("${al.log.oss.accessKeySecret}")
    private String logAccessKeySecret;

    @Value("${al.log.oss.bucketName}")
    private String logBucketName;

    @Value("${al.log.oss.objectName.url}")
    private String logObjectNameUrl;

    @Resource
    private YiDunUtils yidunUtils;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    public String uploadLog(MultipartFile file, String objectName) {
        OSS ossClient = new OSSClientBuilder().build(logEndpoint, logAccessKeyId, logAccessKeySecret);
        try {
            objectName = logObjectNameUrl + objectName;
            PutObjectRequest putObjectRequest;
            try {
                ObjectMetadata objectMetadata=new ObjectMetadata();
                objectMetadata.setContentDisposition("attachment");
                putObjectRequest = new PutObjectRequest(logBucketName, objectName, new ByteArrayInputStream(file.getBytes()));
                putObjectRequest.setMetadata(objectMetadata);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // 上传字符串
            ossClient.putObject(putObjectRequest);
            return "https://" + logBucketName + "." + logEndpoint + "/" + objectName;
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return null;
    }

    public String uploadFile(MultipartFile file, String objectName) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            objectName = objectNameUrl + objectName;
            PutObjectRequest putObjectRequest = null;
            try {
                ObjectMetadata objectMetadata=new ObjectMetadata();
                objectMetadata.setContentDisposition("attachment");
                putObjectRequest = new PutObjectRequest(bucketName, objectName, new ByteArrayInputStream(file.getBytes()));
                putObjectRequest.setMetadata(objectMetadata);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // 上传字符串。
            ossClient.putObject(putObjectRequest);
            String url = "https://" + bucketName + "." + endpoint + "/" + objectName;
            yidunUtils.checkImage(url);
            return url;
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return null;
    }

    public String getImages(String roomNo, String userNo) {
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        String keyPrefix = objectNameCheck + roomNo + "/" + userNo;
        try {
            String mark = "";
            if (redisTemplate.hasKey(keyPrefix)) {
                mark = redisTemplate.opsForValue().get(keyPrefix).toString();
            }
            // 列举包含指定前缀的文件。默认列举100个文件。
            ObjectListing objectListing = ossClient.listObjects(bucketName, keyPrefix);
            objectListing.setMarker(mark);
            List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
            log.info("keyPrefix：{}  sums size:{}", keyPrefix, sums.size());
            for (OSSObjectSummary s : sums) {
                Thread.sleep(1000);
                yidunUtils.checkImage("https://" + bucketName + "." + endpoint + "/" + s.getKey());
                mark = s.getKey();
            }
            redisTemplate.opsForValue().set(keyPrefix, mark, 7, TimeUnit.DAYS);
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException | InterruptedException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return null;
    }
}                    