<template>
    <div style="width: 100vw;height: 100vh;overflow: hidden;display: flex">
      <div class="singe-bg"></div>
      <el-container>
        <el-aside></el-aside>
        <el-main class="login-elmain">
          <el-container class="login-container">
            <el-header ></el-header>
            <el-container class="login-main">
              <el-main>
                <el-image style="width: 100%;height: 100%" fit="cover"
                          src="/UI/img.png"/>
              </el-main>
              <el-aside width="400px">
                <div class="right-card">
                  <router-view v-slot="{ Component }">
                    <transition name="el-fade-in-linear" mode="out-in">
                      <component :is="Component" style="height: 100%"/>
                    </transition>
                  </router-view>
                </div>

              </el-aside>
            </el-container>
          </el-container>
        </el-main>
        <el-aside></el-aside>
      </el-container>


        <div class="welcome-title">
            <div style="font-size: 30px;font-weight: bold">服务器监控后台</div>
            <div style="margin-top: 10px">在这里可以看到服务器状态。</div>
        </div>

    </div>
</template>

<script setup>

</script>

<style scoped>

.login-container{
  height: 50%;
}

.login-main{
  background: white;
  align-items: center;
  flex: auto;
}
.right-card {
  z-index: 1;
  background-color: var(--el-bg-color);
}
.singe-bg {
  position: fixed;
  background-image: url("/UI/bg1.png");
  left: 0;
  top:0;
  width: 100vw;
  height: 100vh;
  z-index: -1;
  background-size: cover;
}
.singe-bg:after {
  content: "";
  position: fixed;
  width: 100vw;
  height: 100vh;
  top: 0;
  left: 0;
  background: rgba(246, 246, 246, .7);
}
.welcome-title {
    position: absolute;
    bottom: 30px;
    left: 30px;
    color: white;
    text-shadow: 0 0 10px black;
}
</style>
