export interface AIConfig {
    choice: string;
    key: string;
    systemPrompt: string;
    chatHistoryNum: number;
}

export interface HASSConfig {
    endpoint: string;
    token: string;
}

export interface MusicConfig {
    choice: string;
    endpoint?: string;
}


export interface Device {
    id: string;
    name: string;
    aiConfig: AIConfig;
    hassConfig: HASSConfig;
    newsConfig: MusicConfig;
    musicConfig: MusicConfig;
    audioConfig: MusicConfig;
}

export interface ServiceAliasName {
    serviceName: string;
    aliasName: string;
}

export interface R1Resources {
    aiList: ServiceAliasName[];
    musicList: ServiceAliasName[];
    newsList: ServiceAliasName[];
    audioList: ServiceAliasName[];
}

export interface R1AdminData {
    r1Resources: R1Resources;
    devices: Device[];
    currentDeviceId: string;
    r1GlobalConfig: R1GlobalConfig;
}

export interface R1GlobalConfig {
    hostIp: string;
    ytdlpEndpoint: string;
    cfServiceId: string;
}