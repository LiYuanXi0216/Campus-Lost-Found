<template>
  <div class="admin-container">
    <div class="zh-card">
      <h2 style="margin-bottom: 20px; font-size: 22px;">🛡️ 管理员控制台</h2>
      <p style="color: #8590a6; margin-bottom: 20px;">欢迎进入系统后台。这里未来将展示全站数据统计和违规帖子审核列表。</p>

      <div style="display: flex; gap: 20px;">
        <div class="zh-card" style="flex: 1; background: #f8f9fa; border: 1px solid #ebebeb;">
          <h3 style="color: #8590a6; font-size: 14px;">今日活跃度</h3>
          <h2 style="font-size: 28px; color: #056de8; margin-top: 10px;">158</h2>
        </div>
        <div class="zh-card" style="flex: 1; background: #fcf4f4; border: 1px solid #fbdcdb;">
          <h3 style="color: #8590a6; font-size: 14px;">待审核违规帖子</h3>
          <h2 style="font-size: 28px; color: #f1403c; margin-top: 10px;">12</h2>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-container { padding-top: 10px; }
</style>