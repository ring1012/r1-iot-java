package huan.diy.r1iot.model;

import huan.diy.r1iot.anno.AIDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MusicAiResp {
    @AIDescription("the author of the music")
    private String author;

    @AIDescription("the name of the music")
    private String musicName;

    @AIDescription("the keyword for searching musics")
    private String keyword;
}
