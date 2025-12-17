package com.md.service.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.service.common.CommonKey;
import com.md.service.common.ErrorCodeEnum;
import com.md.service.common.RoomStatus;
import com.md.service.common.RoomUserStatus;
import com.md.service.exception.BaseException;
import com.md.service.model.dto.RoomInfoDTO;
import com.md.service.model.dto.RoomPageDTO;
import com.md.service.model.dto.RtmRoomDTO;
import com.md.service.model.entity.RoomInfo;
import com.md.service.model.entity.Users;
import com.md.service.model.form.RoomCreateForm;
import com.md.service.repository.RoomInfoMapper;
import com.md.service.service.RoomInfoService;
import com.md.service.service.RoomSongService;
import com.md.service.service.RoomUsersService;
import com.md.service.service.UsersService;
import com.md.service.utils.MdStringUtils;
import com.md.service.utils.RtmTokenBuilderSample;
import com.md.service.utils.YidunUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.security.Credential;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 房间信息 服务实现类
 * </p>
 */
@Service
public class RoomInfoServiceImpl extends ServiceImpl<RoomInfoMapper, RoomInfo> implements RoomInfoService {

    @Resource
    private UsersService usersService;

    @Resource
    private RoomUsersService roomUsersService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RoomSongService roomSongService;

    @Resource
    private YidunUtils yidunUtils;

    @Resource
    private RtmTokenBuilderSample rtmTokenBuilderSample;

    @Value("${room.info.close.time}")
    private Long closeTime;

    @Override
    @Transactional
    public String createRoom(RoomCreateForm form) {
        if(StringUtils.isEmpty(form.getName())){
            throw new BaseException(ErrorCodeEnum.room_name_is_empty,ErrorCodeEnum.room_name_is_empty.getMessage());
        }
        if(form.getIsPrivate().equals(1) && StringUtils.isEmpty(form.getPassword())){
            throw new BaseException(ErrorCodeEnum.password,ErrorCodeEnum.password.getMessage());
        }
        Users users = usersService.getUserByNo(form.getUserNo());
        List<RoomInfo> roomInfoList = baseMapper.selectList(new LambdaQueryWrapper<RoomInfo>().eq(RoomInfo::getCreator,users.getId()).
                eq(RoomInfo::getStatus, RoomStatus.OPEN.getCode()));
        if(!CollectionUtils.isEmpty(roomInfoList)){
            roomInfoList.forEach(e -> {//创建房间 关掉之前开过的房间
                e.setStatus(RoomStatus.CLOSE.getCode());
                baseMapper.updateById(e);
            });
//            throw new BaseException(ErrorCodeEnum.there_is_no_closed_room,ErrorCodeEnum.there_is_no_closed_room.getMessage());
        }
        RoomInfo roomInfo = new RoomInfo();
        yidunUtils.checkYidunText(form.getName());
        roomInfo.setName(form.getName());
        roomInfo.setIsPrivate(form.getIsPrivate());
        if(form.getIsPrivate().equals(1)){
            if(StringUtils.isEmpty(form.getPassword())){
                throw new BaseException(ErrorCodeEnum.please_enter_password);
            }
        }
        roomInfo.setPassword(form.getPassword());
        roomInfo.setCreator(users.getId());
        roomInfo.setRoomNo(MdStringUtils.randomDelete(Credential.MD5.digest(form.getName() + System.currentTimeMillis()).substring(4),5));
        roomInfo.setIsChorus(0);
        roomInfo.setBgOption(form.getBgOption());
        roomInfo.setSoundEffect(form.getSoundEffect());
        roomInfo.setBelCanto(form.getBelCanto());
        roomInfo.setIcon(form.getIcon());
        baseMapper.insert(roomInfo);
//        rtmJavaClient.login(users.getUserNo(),roomInfo.getRoomNo());
//        rtmJavaClient.joinRoom(roomInfo.getRoomNo());
        roomUsersService.joinRoom(roomInfo.getRoomNo(),users.getId(),0,0);
        return roomInfo.getRoomNo();
    }

    @Override
    public void closeRoom(String roomNo, String userNo) {
        Users users = usersService.getUserByNo(userNo);
        RoomInfo roomInfo = baseMapper.selectOne(new LambdaQueryWrapper<RoomInfo>().eq(RoomInfo::getCreator,users.getId()).
                eq(RoomInfo::getStatus, RoomStatus.OPEN.getCode()).eq(RoomInfo::getRoomNo,roomNo));
        if(roomInfo == null){
            throw new BaseException(ErrorCodeEnum.cannot_close_room,ErrorCodeEnum.cannot_close_room.getMessage());
        }
        roomInfo.setStatus(RoomStatus.CLOSE.getCode());
        baseMapper.updateById(roomInfo);
//        rtmJavaClient.closeRoom(roomInfo.getRoomNo());
    }

    @Override
    public void closeRoom(String roomNo) {
        RoomInfo roomInfo = baseMapper.selectOne(new LambdaQueryWrapper<RoomInfo>().
                eq(RoomInfo::getStatus, RoomStatus.OPEN.getCode()).eq(RoomInfo::getRoomNo,roomNo));
        if(roomInfo == null){
            throw new BaseException(ErrorCodeEnum.cannot_close_room,ErrorCodeEnum.cannot_close_room.getMessage());
        }
        roomInfo.setStatus(RoomStatus.CLOSE.getCode());
        baseMapper.updateById(roomInfo);
    }

    @Override
    public void onSeat(String roomNo, String userNo,Integer seat) {
        RLock rlock = redissonClient.getLock(CommonKey.on_seat_lock + roomNo);
        try {
            if(rlock.tryLock(10, TimeUnit.SECONDS)){
                Users users = usersService.getUserByNo(userNo);
                RoomInfo roomInfo = baseMapper.selectOne(new LambdaQueryWrapper<RoomInfo>().
                        eq(RoomInfo::getStatus, RoomStatus.OPEN.getCode()).eq(RoomInfo::getRoomNo,roomNo));
                if(roomInfo == null){
                    throw new BaseException(ErrorCodeEnum.no_room,ErrorCodeEnum.no_room.getMessage());
                }
                if(roomUsersService.checkStatus(roomNo, users.getId(),seat,0)){
                    roomUsersService.joinRoom(roomNo, users.getId(),seat,0);
                    RtmRoomDTO rtmRoomDTO = new RtmRoomDTO();
                    rtmRoomDTO.setRoomNo(roomNo);
                    rtmRoomDTO.setStatus(RoomUserStatus.on_seat.getCode());
                    rtmRoomDTO.setUserNo(userNo);
                    rtmRoomDTO.setSeat(seat);
//                    rtmJavaClient.sendMessage(roomNo, JsonUtil.toJsonString(rtmRoomDTO));
                }
            }
        } catch (InterruptedException e) {
            throw new BaseException(ErrorCodeEnum.system_error);
        }finally {
            rlock.unlock();
        }
    }

    @Override
    public void outSeat(String roomNo, String userNo) {
        Users users = usersService.getUserByNo(userNo);
        RoomInfo roomInfo = baseMapper.selectOne(new LambdaQueryWrapper<RoomInfo>().
                eq(RoomInfo::getStatus, RoomStatus.OPEN.getCode()).eq(RoomInfo::getRoomNo,roomNo));
        if(roomInfo == null){
            throw new BaseException(ErrorCodeEnum.no_room,ErrorCodeEnum.no_room.getMessage());
        }
        roomSongService.delSong(roomNo,userNo);
        roomSongService.delChorus(roomNo,userNo);
        roomUsersService.outSeat(roomNo,users.getId(),userNo);
    }

    @Override
    public void updateRoom(RoomCreateForm form) {
        usersService.getUserByNo(form.getUserNo());
        RoomInfo roomInfo = baseMapper.selectOne(new LambdaQueryWrapper<RoomInfo>().eq(RoomInfo::getRoomNo,form.getRoomNo()));
        if(roomInfo == null){
            throw new BaseException(ErrorCodeEnum.no_room);
        }
        if(form.getIsChorus() != null){
            roomInfo.setIsChorus(form.getIsChorus());
        }
        if(StringUtils.isNoneBlank(form.getBgOption())){
            roomInfo.setBgOption(form.getBgOption());
        }
        if(StringUtils.isNoneBlank(form.getSoundEffect())){
            roomInfo.setSoundEffect(form.getSoundEffect());
        }
        if(StringUtils.isNoneBlank(form.getBelCanto())){
            roomInfo.setBelCanto(form.getBelCanto());
        }
        baseMapper.updateById(roomInfo);
        RtmRoomDTO rtmRoomDTO = new RtmRoomDTO();
        rtmRoomDTO.setRoomNo(roomInfo.getRoomNo());
        rtmRoomDTO.setStatus(RoomStatus.UPDATE_ROOM_INFO.getCode());
        rtmRoomDTO.setData(roomInfo);
//        rtmJavaClient.sendMessage(roomInfo.getRoomNo(),JsonUtil.toJsonString(rtmRoomDTO));
    }

    @Override
    public RoomInfoDTO getRooInfo(String roomNo,String userNo,String password) {
        RoomInfoDTO result = new RoomInfoDTO();
        RoomInfo roomInfo = baseMapper.selectOne(new LambdaQueryWrapper<RoomInfo>().eq(RoomInfo::getRoomNo,roomNo));
        if(roomInfo == null){
            throw new BaseException(ErrorCodeEnum.no_room);
        }
        if(roomInfo.getIsPrivate().equals(1)){
            if(!roomInfo.getPassword().equals(password)){
                throw new BaseException(ErrorCodeEnum.password_is_not_correct);
            }
        }
        Users users = usersService.getUserByNo(userNo);
        roomUsersService.joinRoom(roomInfo.getRoomNo(),users.getId(),-1,0);
        Users creator = usersService.getById(roomInfo.getCreator());
        result.setName(roomInfo.getName());
        result.setCreatorNo(creator.getUserNo());
        result.setRoomNo(roomNo);
        result.setIsChorus(roomInfo.getIsChorus());
        result.setBgOption(roomInfo.getBgOption());
        result.setSoundEffect(roomInfo.getSoundEffect());
        result.setBelCanto(roomInfo.getBelCanto());
        result.setRoomUserInfoDTOList(roomUsersService.roomUserInfoDTOList(roomNo,creator.getUserNo()));
        result.setRoomSongInfoDTOS(roomSongService.getRoomSongInfo(roomNo));
        try {
            result.setAgoraRTMToken(rtmTokenBuilderSample.getToken(users.getId(),roomNo));
            result.setAgoraPlayerRTCToken(rtmTokenBuilderSample.getRtcToken(users.getId() * 10 + 1,roomNo));
            result.setAgoraRTCToken(rtmTokenBuilderSample.getRtcToken(users.getId() ,roomNo));
        } catch (Exception e) {
           log.error("getToken error",e);
        }
//        rtmJavaClient.sendMessagePeer(JsonUtil.toJsonString(result),creator.getUserNo());
        return result;
    }

    @Override
    public void outRoom(String roomNo, String userNo) {
        roomUsersService.outRoom(roomNo,userNo);
        Users users = usersService.getUserByNo(userNo);
        roomUsersService.outSeat(roomNo,users.getId(),userNo);
        roomSongService.delSong(roomNo,userNo);
        roomSongService.delChorus(roomNo,userNo);
    }

    @Override
    public IPage<RoomPageDTO> roomList(Page page) {
        IPage<RoomInfo> list = baseMapper.selectPage(page,new LambdaQueryWrapper<RoomInfo>().
                eq(RoomInfo::getStatus,RoomStatus.OPEN).orderByDesc(RoomInfo::getCreatedAt));
        IPage<RoomPageDTO> result = new Page<>();
        result.setPages(list.getPages());
        List<RoomPageDTO> roomPageDTOS = new ArrayList<>();
        list.getRecords().forEach(e -> {
            RoomPageDTO obj = new RoomPageDTO();
            BeanUtils.copyProperties(e,obj);
            obj.setRoomPeopleNum(roomUsersService.getRoomNum(e.getRoomNo()));
            try {
                obj.setCreatorNo(usersService.getUserNoById(e.getCreator()));
                roomPageDTOS.add(obj);
            }catch (Exception ex){
                log.error("房间创建者异常",ex);
            }
        });
        result.setRecords(roomPageDTOS);
        result.setTotal(list.getTotal());
        result.setSize(list.getSize());
        result.setCurrent(list.getCurrent());
        return result;
    }

    @Override
    public JSONObject getRoomNum(String roomNo) {
        Long num = roomUsersService.getRoomNum(roomNo);
        JSONObject result = new JSONObject();
        result.put("num",num);
        return result;
    }

    @Override
    public void searchRoomAndClose() {
        LocalDateTime localDateTime = LocalDateTime.now();
        localDateTime = localDateTime.minusHours(closeTime);
        List<RoomInfo> roomInfoList = baseMapper.
                selectList(new LambdaQueryWrapper<RoomInfo>().
                        eq(RoomInfo::getStatus,0).
                        lt(RoomInfo::getCreatedAt,localDateTime));
        Set<String> users = new HashSet<>();
        Map<String,String> roomUsers = new HashMap<>();
        roomInfoList.forEach(e -> {
            String userNo = usersService.getUserNoById(e.getCreator());
            users.add(userNo);
            roomUsers.put(userNo,e.getRoomNo());
        });
        if(users.size() > 0){
//            rtmJavaClient.queryPeersOnlineStatus(users,roomUsers);
        }
    }
}
