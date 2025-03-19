import axios from 'axios';

// 创建一个 axios 实例
const axiosInstance = axios.create({
});

// 添加请求拦截器
axiosInstance.interceptors.request.use(
    (config) => {
        // 从 localStorage 中获取 token
        const token = localStorage.getItem('token'); // 假设你存储在 localStorage 中的 key 为 'token'

        // 如果 token 存在，将其添加到 Authorization 请求头中
        if (token) {
            config.headers['Authorization'] = `${token}`;
        }

        // 返回请求配置
        return config;
    },
    (error) => {
        // 请求失败时的错误处理
        return Promise.reject(error);
    }
);

// 导出 axios 实例
export default axiosInstance;
