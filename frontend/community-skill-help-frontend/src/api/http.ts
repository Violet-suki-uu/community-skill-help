/**
 * 模块说明：HTTP 基础模块。作用：统一管理请求基地址、超时和 token 注入。
 */
import axios from "axios";

export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8081";

const request = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err?.response?.status === 401) {
      localStorage.removeItem("token");
    }
    return Promise.reject(err);
  }
);

export default request;

