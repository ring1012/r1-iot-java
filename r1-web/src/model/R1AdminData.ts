export interface R1AdminData {
  defaultAI: "Gemini" | "Grok"; // 只能是 Gemini 或 Grok
  chat: {
    systemInfo: string; // 字符串类型
    chatAI: "Gemini" | "Grok"; // 只能是 Gemini 或 Grok
  };
  music: "qq" | "neast"; // 只能是 qq 或 neast
}