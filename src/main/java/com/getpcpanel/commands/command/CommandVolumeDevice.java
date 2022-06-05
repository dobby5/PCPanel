package com.getpcpanel.commands.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class CommandVolumeDevice extends CommandVolume implements DialAction {
    private final String deviceId;

    @JsonCreator
    public CommandVolumeDevice(@JsonProperty("deviceId") String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public void execute(int volume) {
        getSndCtrl().setDeviceVolume(deviceId, volume / 100f);
    }
}
