<template>
  <div class="publish-page">
    <h2>发布技能</h2>

    <div class="form-card">
      <el-input v-model.trim="form.title" placeholder="技能标题" maxlength="50" show-word-limit />
      <el-input v-model.number="form.price" placeholder="价格（元）" type="number" />
      <el-input v-model.trim="form.category" placeholder="分类（如：家教/维修/设计）" maxlength="20" />
      <el-input
        v-model.trim="form.description"
        placeholder="技能描述"
        type="textarea"
        :rows="3"
        maxlength="300"
        show-word-limit
      />

      <el-upload
        class="uploader"
        :show-file-list="false"
        :http-request="doUpload"
        :before-upload="beforeUpload"
        accept="image/*"
      >
        <img v-if="form.image" :src="form.image" class="preview-image" />
        <el-button v-else type="primary" plain>上传图片</el-button>
      </el-upload>

      <el-button type="primary" :loading="submitting" @click="submit">发布技能</el-button>
    </div>

    <h2 class="manage-title">我的技能</h2>
    <div class="filter-bar">
      <el-radio-group v-model="activeTab" size="small">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="on">上架中</el-radio-button>
        <el-radio-button label="off">已下架</el-radio-button>
      </el-radio-group>
    </div>

    <div class="manage-grid">
      <div v-for="item in filteredList" :key="item.id" class="manage-card">
        <img :src="item.image || fallbackImage" />
        <div class="info">
          <p class="title">{{ item.title }}</p>
          <span class="price">¥{{ item.price }}</span>
          <p class="meta">{{ item.category || "未分类" }} · {{ item.status === 1 ? "上架中" : "已下架" }}</p>
        </div>
        <div class="actions">
          <el-button size="small" type="warning" @click="toggleStatus(item)">
            {{ item.status === 1 ? "下架" : "上架" }}
          </el-button>
          <el-button size="small" type="danger" @click="removeItem(item.id)">删除</el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="filteredList.length === 0" description="暂无技能" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import type { UploadProps, UploadRequestOptions } from "element-plus";
import { ElMessage, ElMessageBox } from "element-plus";
import { addSkill, deleteSkill, getMySkills, updateSkillStatus, type SkillItem } from "../api/skill";
import { uploadImageApi } from "../api/upload";

const fallbackImage = "https://via.placeholder.com/400x300?text=Skill";

const form = reactive({
  title: "",
  price: 0,
  image: "",
  category: "",
  description: "",
});

const submitting = ref(false);
const myList = ref<SkillItem[]>([]);
const activeTab = ref<"all" | "on" | "off">("all");

const filteredList = computed(() => {
  if (activeTab.value === "all") return myList.value;
  if (activeTab.value === "on") return myList.value.filter((x) => x.status === 1);
  return myList.value.filter((x) => x.status !== 1);
});

const beforeUpload: UploadProps["beforeUpload"] = (file) => {
  if (!file.type.startsWith("image/")) {
    ElMessage.error("仅支持图片文件");
    return false;
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error("图片大小不能超过 5MB");
    return false;
  }
  return true;
};

async function doUpload(options: UploadRequestOptions) {
  try {
    const file = options.file as File;
    const res = await uploadImageApi(file);
    const url = res.data?.data || res.data?.url || res.data;
    form.image = typeof url === "string" ? url : "";
    if (!form.image) throw new Error("上传返回图片地址为空");
    options.onSuccess(res.data);
    ElMessage.success("图片上传成功");
  } catch (error: any) {
    options.onError(error);
    ElMessage.error(error?.response?.data?.message || "图片上传失败");
  }
}

async function submit() {
  if (submitting.value) return;
  if (!form.title || !form.price || !form.image) {
    ElMessage.warning("请至少填写标题、价格并上传图片");
    return;
  }
  try {
    submitting.value = true;
    await addSkill(form);
    ElMessage.success("发布成功");
    form.title = "";
    form.price = 0;
    form.image = "";
    form.category = "";
    form.description = "";
    await loadMine();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || "发布失败");
  } finally {
    submitting.value = false;
  }
}

async function loadMine() {
  try {
    myList.value = await getMySkills();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || "加载我的技能失败");
  }
}

async function toggleStatus(item: SkillItem) {
  const nextStatus = item.status === 1 ? 0 : 1;
  try {
    await updateSkillStatus(item.id, nextStatus);
    item.status = nextStatus;
    ElMessage.success(nextStatus === 1 ? "已上架" : "已下架");
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || "更新状态失败");
  }
}

async function removeItem(id: number) {
  try {
    await ElMessageBox.confirm("确认删除该技能？", "提示", { type: "warning" });
    await deleteSkill(id);
    myList.value = myList.value.filter((x) => x.id !== id);
    ElMessage.success("删除成功");
  } catch (error: any) {
    if (error === "cancel" || error === "close") return;
    ElMessage.error(error?.response?.data?.message || "删除失败");
  }
}

onMounted(loadMine);
</script>

<style scoped>
.publish-page {
  padding: 40px 60px;
  max-width: 1200px;
  margin: auto;
}

.form-card {
  background: #fff;
  padding: 30px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
}

.uploader {
  width: 220px;
}

.preview-image {
  width: 220px;
  height: 160px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #eee;
}

.manage-title {
  margin-top: 40px;
}

.filter-bar {
  margin-top: 12px;
}

.manage-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
  margin-top: 20px;
}

.manage-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
}

.manage-card img {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.manage-card .info {
  padding: 10px;
}

.title {
  margin: 0 0 6px;
}

.price {
  color: #ff5500;
  font-weight: 600;
}

.meta {
  margin: 8px 0 0;
  color: #888;
  font-size: 12px;
}

.actions {
  display: flex;
  justify-content: space-around;
  padding: 10px;
}
</style>
