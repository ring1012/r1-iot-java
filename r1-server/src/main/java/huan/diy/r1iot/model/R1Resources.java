package huan.diy.r1iot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class R1Resources {
    private List<ServiceAliasName> aiList;
    private List<ServiceAliasName> musicList;
    private List<ServiceAliasName> newsList;
    private List<ServiceAliasName> audioList;
    private List<ServiceAliasName> weatherList;
    private List<CityLocation> cityLocations;
}
