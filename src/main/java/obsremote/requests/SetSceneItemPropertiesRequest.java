package obsremote.requests;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import obsremote.OBSCommunicator;

@Data
public class SetSceneItemPropertiesRequest extends BaseRequest {
    @SerializedName("scene-name")
    private String scene;
    private final String item;
    private final boolean visible;

    public SetSceneItemPropertiesRequest(OBSCommunicator com, String scene, String source, boolean visible) {
        super(com, RequestType.SetSceneItemProperties);
        this.scene = scene;
        item = source;
        this.visible = visible;
    }
}

