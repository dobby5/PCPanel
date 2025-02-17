package com.getpcpanel.commands.command;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class CommandVolumeFocus extends CommandVolume implements DialAction {
    @Override
    public void execute(DialActionParameters context) {
        getSndCtrl().setFocusVolume(context.dial() / 100f);
    }
}
