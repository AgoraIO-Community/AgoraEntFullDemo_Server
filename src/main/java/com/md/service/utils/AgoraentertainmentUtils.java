package com.md.service.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.md.service.model.entity.Songs;
import com.md.service.service.SongsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 声网服务
 */
@Component
@Slf4j
public class AgoraentertainmentUtils {

    @Resource
    private RestTemplate restTemplate;

    @Value("${rtm.java.appId}")
    private String appId;

    @Value("${rtm.java.customerKey}")
    private String customerKey;

    @Value("${rtm.java.restfulapi.customerSecret}")
    private String customerSecret;

    @Value("${rtm.java.requestId}")
    private String requestId;

    @Value("${al.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${al.oss.endpoint}")
    private String endpoint;

    @Value("${al.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${al.oss.bucketName}")
    private String bucketName;

    @Value("${yitu.appId}")
    private String yituAppId;

    @Value("${yitu.devId}")
    private String devId;

    @Value("${yitu.devKey}")
    private String devKey;

    @Value("${callback.seed}")
    private String callbackSeed;

    @Value("${al.oss.objectNameCheck.url}")
    private String objectNameCheck;

    @Resource
    private SongsService songsService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private RtmTokenBuilderSample rtmTokenBuilderSample;

    public JSONObject getAll(Integer pageType, String pageCode, Integer size) {
        while (true) {
            String plainCredentials = customerKey + ":" + customerSecret;
            String base64Credentials = new String(Base64.getEncoder().encode(plainCredentials.getBytes()));
            String authorizationHeader = "Basic " + base64Credentials;
            String url = "https://api.agora.io/cn/v1.0/projects/" + appId + "/ktv-service/api/serv/songs?requestId=" + requestId
                    + "&pageType=" + pageType + "&pageCode=" + pageCode + "&size=" + size;
            log.info("url : {}", url);
            MultiValueMap<String, String> paramJson = new LinkedMultiValueMap<>();
            HttpEntity<?> httpEntity = new HttpEntity<>(paramJson, getJsonHeader(authorizationHeader));
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
            log.info("responseEntity : {}", responseEntity.getBody());
            String ret;
            try {
                ret = new String(responseEntity.getBody().getBytes("ISO-8859-1"), "utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            JSONObject result = JSON.parseObject(ret);
            if (!result.getString("msg").equals("ok")) {
                log.info("导入所有歌曲");
                break;
            }
            JSONArray lists = result.getJSONObject("data").getJSONArray("list");
            for (int i = 0; i < lists.size(); i++) {
                JSONObject object = lists.getJSONObject(i);
                if (i == 0) {
                    pageCode = object.getString("songCode");
                }
                songsService.init(object.getString("name"), "",
                        object.getString("songCode"), object.getString("poster"), object.getString("singer"));
            }
        }
        return null;
    }

    public JSONObject getSong(String songCode, Integer lyricType) {
//        List<Songs> songsList = songsService.list(new LambdaQueryWrapper<Songs>().eq(Songs::getLyric,""));
//        songsList.forEach(e -> {
        String plainCredentials = customerKey + ":" + customerSecret;
        String base64Credentials = new String(Base64.getEncoder().encode(plainCredentials.getBytes()));
        String authorizationHeader = "Basic " + base64Credentials;
        String url = "https://api.agora.io/cn/v1.0/projects/" + appId + "/ktv-service/api/serv/song-url?requestId=" + requestId
                + "&songCode=" + songCode + "&lyricType=" + lyricType;
        log.info("url : {}", url);
        MultiValueMap<String, String> paramJson = new LinkedMultiValueMap<>();
        HttpEntity<?> httpEntity = new HttpEntity<>(paramJson, getJsonHeaderNoType(authorizationHeader));
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
        log.info("responseEntity : {}", responseEntity.getBody());
        String ret;
        try {
            ret = new String(responseEntity.getBody().getBytes("ISO-8859-1"), "utf-8");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        JSONObject result = JSON.parseObject(ret);
//            e.setLyric(result.getJSONObject("data").getString("lyric"));
//            songsService.updateById(e);
//        });
        return result;
    }

    public JSONObject getSongDb(String songCode, Integer lyricType) {
        List<Songs> songsList = songsService.list(new LambdaQueryWrapper<Songs>().eq(Songs::getLyric, ""));
        songsList.forEach(e -> {
            String plainCredentials = customerKey + ":" + customerSecret;
            String base64Credentials = new String(Base64.getEncoder().encode(plainCredentials.getBytes()));
            String authorizationHeader = "Basic " + base64Credentials;
            String url = "https://api.agora.io/cn/v1.0/projects/" + appId + "/ktv-service/api/serv/song-url?requestId=" + requestId
                    + "&songCode=" + songCode + "&lyricType" + lyricType;
            log.info("url : {}", url);
            MultiValueMap<String, String> paramJson = new LinkedMultiValueMap<>();
            HttpEntity<?> httpEntity = new HttpEntity<>(paramJson, getJsonHeader(authorizationHeader));
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
            log.info("responseEntity : {}", responseEntity.getBody());
            String ret;
            try {
                ret = new String(responseEntity.getBody().getBytes("ISO-8859-1"), "utf-8");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            JSONObject result = JSON.parseObject(ret);
            e.setLyric(result.getJSONObject("data").getString("lyric"));
            songsService.updateById(e);
        });
        return null;
    }

    public JSONObject getAdd(Integer pageType, String pageCode, Integer size, String unix) {
        while (true) {
            String plainCredentials = customerKey + ":" + customerSecret;
            String base64Credentials = new String(Base64.getEncoder().encode(plainCredentials.getBytes()));
            String authorizationHeader = "Basic " + base64Credentials;
            String url = "https://api.agora.io/cn/v1.0/projects/" + appId + "/ktv-service/api/serv/songs-incr?requestId=" + requestId
                    + "&pageType=" + pageType + "&pageCode=" + pageCode + "&size=" + size + "&lastUpdateTime=" + unix;
            log.info("url : {}", url);
            MultiValueMap<String, String> paramJson = new LinkedMultiValueMap<>();
            HttpEntity<?> httpEntity = new HttpEntity<>(paramJson, getJsonHeader(authorizationHeader));
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
            log.info("responseEntity : {}", responseEntity.getBody());
            String ret;
            try {
                ret = new String(responseEntity.getBody().getBytes("ISO-8859-1"), "utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            JSONObject result = JSON.parseObject(ret);
            if (!result.getString("msg").equals("ok")) {
                log.info("导入所有歌曲");
                break;
            }
            JSONArray lists = result.getJSONObject("data").getJSONArray("list");
            for (int i = 0; i < lists.size(); i++) {
                JSONObject object = lists.getJSONObject(i);
                if (i == 0) {
                    pageCode = object.getString("songCode");
                }
                songsService.init(object.getString("name"), "",
                        object.getString("songCode"), object.getString("poster"), object.getString("singer"));
            }
        }
        return null;
    }


    public JSONObject songHot(Integer hotType) {
        String plainCredentials = customerKey + ":" + customerSecret;
        String base64Credentials = new String(Base64.getEncoder().encode(plainCredentials.getBytes()));
        String authorizationHeader = "Basic " + base64Credentials;
        String url = "https://api.agora.io/cn/v1.0/projects/" + appId + "/ktv-service/api/serv/song-hot?requestId=" + requestId
                + "&hotType=" + hotType;
        log.info("url : {}", url);
        MultiValueMap<String, String> paramJson = new LinkedMultiValueMap<>();
        HttpEntity<?> httpEntity = new HttpEntity<>(paramJson, getJsonHeader(authorizationHeader));
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
        log.info("songHot responseEntity : {}", responseEntity.getBody());
        String ret;
        try {
            ret = new String(responseEntity.getBody().getBytes("ISO-8859-1"), "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        JSONObject result = JSON.parseObject(ret);
        JSONArray lists = result.getJSONObject("data").getJSONArray("list");
        for (int i = 0; i < lists.size(); i++) {
            JSONObject object = lists.getJSONObject(i);
            String songCode = object.getString("songCode");
            songsService.update(new LambdaUpdateWrapper<Songs>().eq(Songs::getSongNo, songCode).set(Songs::getType, hotType));
        }
        return result;
    }

    /**
     * 打开视频截图
     */
    @Async
    public void openRecording(Integer userNo, String roomNo) {
        String plainCredentials = customerKey + ":" + customerSecret;
        String base64Credentials = new String(Base64.getEncoder().encode(plainCredentials.getBytes()));
        String authorizationHeader = "Basic " + base64Credentials;
        String resourceId = resourceId(roomNo,userNo,authorizationHeader);
        String startUrl = "https://api.agora.io/v1/apps/" + appId + "/cloud_recording/resourceid/" + resourceId + "/mode/individual/start";
        log.info("startUrl : {}", startUrl);
        JSONObject bodyStart = new JSONObject();
        bodyStart.put("cname", roomNo);
        bodyStart.put("uid", String.valueOf(userNo * 1000 + 999));
        JSONObject clientRequestStart = new JSONObject();
        JSONObject recordingConfig = new JSONObject();
        recordingConfig.put("channelType",1);
        recordingConfig.put("subscribeUidGroup",0);
        JSONArray subscribeAudioUids = new JSONArray();
        subscribeAudioUids.add(userNo.toString());
        recordingConfig.put("subscribeVideoUids",subscribeAudioUids);
        JSONObject snapshotConfig = new JSONObject();
        snapshotConfig.put("captureInterval",5);
        JSONArray fileType = new JSONArray();
        fileType.add("jpg");
        snapshotConfig.put("fileType",fileType);
        JSONObject storageConfig = new JSONObject();
        storageConfig.put("accessKey",accessKeyId);
        storageConfig.put("region",3);
        storageConfig.put("bucket",bucketName);
        storageConfig.put("secretKey",accessKeySecret);
        storageConfig.put("vendor",2);
        JSONArray fileNamePrefix = new JSONArray();
        if(StringUtils.isNoneBlank(objectNameCheck)){
            for (String s : objectNameCheck.split("\\/")) {
                fileNamePrefix.add(s);
            }
        }
        fileNamePrefix.add(roomNo);
        fileNamePrefix.add(userNo.toString());
        storageConfig.put("fileNamePrefix",fileNamePrefix);
        clientRequestStart.put("snapshotConfig",snapshotConfig);
        clientRequestStart.put("storageConfig",storageConfig);
        clientRequestStart.put("recordingConfig",recordingConfig);
        try {
            clientRequestStart.put("token",rtmTokenBuilderSample.getRtcToken(userNo * 1000 + 999,roomNo));
        } catch (Exception e) {
            log.error("reviewVoice get token error",e);
        }
        bodyStart.put("clientRequest",clientRequestStart);
        log.info("openRecording bodyStart : {}",bodyStart);
        HttpEntity<?> httpEntityStart = new HttpEntity<>(bodyStart, getJsonHeader(authorizationHeader));
        log.info("openRecording httpEntityStart : {}",httpEntityStart);
        ResponseEntity<String> responseEntityStart = restTemplate.postForEntity(startUrl, httpEntityStart, String.class);
        log.info("openRecording responseEntityStart : {} code: {}", responseEntityStart.getBody(),
                responseEntityStart.getStatusCode());
        try {
            JSONObject result = JSONObject.parseObject(responseEntityStart.getBody());
            String redisKey = "sid:"+ userNo + "_" + roomNo;
            String redisKeyResourceId = "resourceId:"+ userNo + "_" + roomNo;
            redisTemplate.opsForValue().set(redisKey,result.getString("sid"),24,TimeUnit.HOURS);
            redisTemplate.opsForValue().set(redisKeyResourceId,resourceId,24,TimeUnit.HOURS);
        }catch (Exception e){
            log.error("add redis key error",e);
        }
    }

    @Async
    public void closeRecording(Integer userNo, String roomNo) {
        String plainCredentials = customerKey + ":" + customerSecret;
        String base64Credentials = new String(Base64.getEncoder().encode(plainCredentials.getBytes()));
        String authorizationHeader = "Basic " + base64Credentials;
        String resourceId = "";
        String redisKey = "sid:"+ userNo + "_" + roomNo;
        String redisKeyResourceId = "resourceId:"+ userNo + "_" + roomNo;
        String sid = "";
        if(redisTemplate.hasKey(redisKey)){
            sid = redisTemplate.opsForValue().get(redisKey).toString();
        }
        if(redisTemplate.hasKey(redisKeyResourceId)){
            resourceId = redisTemplate.opsForValue().get(redisKeyResourceId).toString();
        }
        String startUrl = "https://api.agora.io/v1/apps/" + appId + "/cloud_recording/resourceid/" + resourceId + "/sid/"+sid+"/mode/individual/stop";
        log.info("closeRecording startUrl : {}", startUrl);
        JSONObject bodyStart = new JSONObject();
        bodyStart.put("cname", roomNo);
        bodyStart.put("uid", String.valueOf(userNo * 1000 + 999));
        JSONObject clientRequestStart = new JSONObject();
        clientRequestStart.put("async_stop",false);
        bodyStart.put("clientRequest",clientRequestStart);
        log.info("closeRecording bodyStart : {}",bodyStart);
        HttpEntity<?> httpEntityStart = new HttpEntity<>(bodyStart, getJsonHeader(authorizationHeader));
        log.info("closeRecording httpEntityStart : {}",httpEntityStart);
        ResponseEntity<String> responseEntityStart = restTemplate.postForEntity(startUrl, httpEntityStart, String.class);
        log.info("closeRecording responseEntityStart : {} code: {}", responseEntityStart.getBody(),
                responseEntityStart.getStatusCode());
    }

    @Async
    public void reviewVoice(Integer userNo, String roomNo,Integer onSeat) {
        String redisKey = "reviewVoice_"+userNo+ ":" + roomNo + ":" +onSeat;
        if(redisTemplate.hasKey(redisKey)){
            return;
        }
        String plainCredentials = customerKey + ":" + customerSecret;
        String base64Credentials = new String(Base64.getEncoder().encode(plainCredentials.getBytes()));
        String authorizationHeader = "Basic " + base64Credentials;
        String resourceId = resourceId(roomNo,userNo,authorizationHeader);
        String startUrl = "https://api.agora.io/v1/apps/" + appId + "/cloud_recording/resourceid/" + resourceId + "/mode/individual/start";
        log.info("startUrl : {}", startUrl);
        JSONObject bodyStart = new JSONObject();
        bodyStart.put("cname", roomNo);
        bodyStart.put("uid", String.valueOf(userNo * 1000 + 999));
        JSONObject clientRequest = new JSONObject();
        JSONObject extensionServiceConfig = new JSONObject();
        extensionServiceConfig.put("apiVersion","v1");
        extensionServiceConfig.put("errorHandlePolicy","error_abort");
        JSONArray extensionServices = new JSONArray();
        JSONObject extensionService = new JSONObject();
        extensionService.put("errorHandlePolicy","error_abort");
        extensionService.put("serviceName","yitu_voice_async_scan");
        JSONObject serviceParam = new JSONObject();
        JSONObject apiData = new JSONObject();
        apiData.put("accessKey",devId);
        apiData.put("bizType",yituAppId);
        apiData.put("callbackSeed",roomNo);
        apiData.put("secretKey",devKey);
        serviceParam.put("apiData",apiData);
        serviceParam.put("callbackAddr",callbackSeed);
        extensionService.put("serviceParam",serviceParam);
        extensionService.put("streamTypes",0);
        extensionServices.add(extensionService);
        extensionServiceConfig.put("extensionServices",extensionServices);
        JSONObject recordingConfig = new JSONObject();
        recordingConfig.put("channelType",1);
        recordingConfig.put("maxIdleTime",15);
        recordingConfig.put("streamTypes",0);
        recordingConfig.put("subscribeUidGroup",0);
        clientRequest.put("recordingConfig",recordingConfig);
        clientRequest.put("extensionServiceConfig",extensionServiceConfig);
        clientRequest.put("recordingConfig",recordingConfig);
        try {
            clientRequest.put("token",rtmTokenBuilderSample.getRtcToken(userNo * 1000 + 999,roomNo));
        } catch (Exception e) {
            log.error("reviewVoice get token error",e);
        }
        bodyStart.put("clientRequest",clientRequest);
        log.info("reviewVoice bodyStart : {}",bodyStart);
        HttpEntity<?> httpEntityStart = new HttpEntity<>(bodyStart, getJsonHeader(authorizationHeader));
        log.info("reviewVoice httpEntityStart : {}",httpEntityStart);
        ResponseEntity<String> responseEntityStart = restTemplate.postForEntity(startUrl, httpEntityStart, String.class);
        log.info("reviewVoice responseEntityStart : {}", responseEntityStart.getBody());
        if(responseEntityStart.getStatusCode().equals(HttpStatus.OK)){
            redisTemplate.opsForValue().set(redisKey,"ok", 2,TimeUnit.HOURS);
        }
    }

    private String resourceId(String roomNo,Integer userNo,String authorizationHeader){
        String acquireUrl = "https://api.agora.io/v1/apps/" + appId + "/cloud_recording/acquire";
        log.info("acquireUrl : {}", acquireUrl);
        JSONObject body = new JSONObject();
        body.put("cname", roomNo);
        body.put("uid", String.valueOf(userNo * 1000 + 999));
        JSONObject clientRequest = new JSONObject();
        body.put("clientRequest", clientRequest);
        log.info("body : {}",body);
        HttpEntity<?> httpEntity = new HttpEntity<>(body, getJsonHeader(authorizationHeader));
        log.info("Authorization :{}",getJsonHeader(authorizationHeader));
        ResponseEntity<String> responseEntity = restTemplate.exchange(acquireUrl, HttpMethod.POST, httpEntity, String.class);
        log.info("responseEntity : {}", responseEntity.getBody());
        JSONObject result = JSON.parseObject(responseEntity.getBody());
        log.info("resourceId : {}", result);
        return result.getString("resourceId");
    }
    private LinkedMultiValueMap<String, String> getJsonHeader(String authorizationHeader) {
        LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.set("Authorization", authorizationHeader);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    private LinkedMultiValueMap<String, String> getJsonHeaderNoType(String authorizationHeader) {
        LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.set("Authorization", authorizationHeader);
        return headers;
    }
}
