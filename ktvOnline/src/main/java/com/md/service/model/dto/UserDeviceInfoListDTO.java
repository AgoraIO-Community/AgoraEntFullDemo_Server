package com.md.service.model.dto;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;


@Data
@Accessors(chain = true)
public class UserDeviceInfoListDTO {

    @ApiModelProperty("设备型号")
    private List<DeviceItemDTO> models;

    @ApiModelProperty("设备制造商")
    private List<DeviceItemDTO> manufactures;

    @ApiModelProperty("设备操作系统版本")
    private List<DeviceItemDTO> osVersions;

}