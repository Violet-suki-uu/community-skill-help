# 邻光前端

Vue 3 + TypeScript + Vite 前端项目，生产环境由 Nginx 托管静态资源，并把 `/api`、`/uploads` 代理到后端容器。

## 本地开发

```bash
npm install
npm run dev
```

## 生产构建

```bash
npm run build
```

Docker 部署时从项目根目录执行：

```bash
docker compose up -d --build
```
