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

export interface NewsConfig {
    choice: string;
}

export interface Device {
    id: string;
    name: string;
    aiConfig: AIConfig;
    hassConfig: HASSConfig;
    newsConfig: NewsConfig;
}

export interface ServiceAliasName {
    serviceName: string;
    aliasName: string;
}

export interface R1Resources {
    aiList: ServiceAliasName[];
    musicList: ServiceAliasName[];
    audioList: ServiceAliasName[];
}

export interface R1AdminData {
    r1Resources: R1Resources;
    devices: Device[];
    currentDeviceId: string;
}
