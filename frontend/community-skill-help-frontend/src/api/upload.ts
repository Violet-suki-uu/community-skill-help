/**
 * 模块说明：上传 API 模块。作用：封装图片上传请求。
 */
import http from "./http";

export function uploadImageApi(file: File) {
  const form = new FormData();
  form.append("file", file);
  return http.post("/api/upload/image", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}

