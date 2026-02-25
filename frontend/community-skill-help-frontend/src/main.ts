/**
 * 模块说明：前端启动入口。作用：初始化 Vue、路由、状态管理与 UI 组件库。
 */
import { createApp } from "vue";
import App from "./App.vue";

import router from "./router";
import { createPinia } from "pinia";

import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import { preloadAmapSdk } from "./utils/amap";

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(ElementPlus);

preloadAmapSdk();

app.mount("#app");

