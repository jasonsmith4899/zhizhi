import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";

function isTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      name: "Login",
      component: () => import("../views/Login.vue"),
      meta: { public: true },
    },
    {
      path: "/",
      component: () => import("../views/Layout.vue"),
      redirect: "/dashboard",
      children: [
        {
          path: "dashboard",
          name: "Dashboard",
          component: () => import("../views/Dashboard.vue"),
          meta: { title: "仪表盘" },
        },
        {
          path: "knowledge",
          name: "KnowledgeList",
          component: () => import("../views/knowledge/KnowledgeList.vue"),
          meta: { title: "知识库" },
        },
        {
          path: "knowledge/:id",
          name: "KnowledgeDetail",
          component: () => import("../views/knowledge/KnowledgeDetail.vue"),
          meta: { title: "知识库详情" },
        },
        {
          path: "chat",
          name: "Chat",
          component: () => import("../views/chat/Chat.vue"),
          meta: { title: "AI 对话" },
        },
        {
          path: "chat-history",
          name: "ChatHistory",
          component: () => import("../views/chat/ChatHistory.vue"),
          meta: { title: "对话记录" },
        },
        {
          path: "profile",
          name: "Profile",
          component: () => import("../views/settings/Profile.vue"),
          meta: { title: "个人设置" },
        },
      ],
    },
  ],
});

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore();

  // F8: 已登录用户访问 /login 重定向到首页
  if (to.path === '/login' && authStore.token) {
    next('/');
    return;
  }

  if (!to.meta.public) {
    // F4: 检查 token 是否存在且未过期
    if (!authStore.token || isTokenExpired(authStore.token)) {
      authStore.logout();
      next("/login");
      return;
    }
  }

  next();
});

export default router;
