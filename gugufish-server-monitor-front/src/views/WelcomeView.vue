<template>
    <div style="width: 100vw;height: 100vh;overflow: hidden;display: flex">
      <div class="singe-bg"></div>
      <el-container class="main-container">
        <el-main class="login-elmain">
          <el-container class="login-container">
            <el-container class="login-main">
              <el-main class="image-container">
                <el-image class="login-image" fit="cover"
                          src="/UI/img.png" />
              </el-main>
              <el-aside class="login-form-container">
                <div class="right-card">
                  <router-view v-slot="{ Component }" style="margin-bottom: 20px">
                    <transition name="el-fade-in-linear" mode="out-in">
                      <component :is="Component" style="height: 100%"/>
                    </transition>
                  </router-view>
                </div>

              </el-aside>
            </el-container>
          </el-container>
        </el-main>
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

.main-container {
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
}

.responsive-aside {
  width: 5% !important;
}

.login-elmain {
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
}

.login-container {
  height: min(80vh, 600px);
  width: 90%;
  max-width: 1200px;
  margin: 0 auto;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.login-main {
  background: white;
  align-items: center;
  flex: auto;
  height: 100%;
}

.image-container {
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.login-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-form-container {
  width: clamp(300px, 30%, 450px) !important;
  padding: 1rem;
}

.right-card {
  z-index: 1;
  background-color: var(--el-bg-color);
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
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
    bottom: clamp(15px, 3vh, 30px);
    left: clamp(15px, 3vw, 30px);
    color: white;
    text-shadow: 0 0 10px black;
}

@media (max-width: 768px) {
  .login-container {
    height: min(90vh, 500px);
    width: 95%;
  }
  
  .login-form-container {
    width: 100% !important;
  }
  
  .image-container {
    display: none;
  }
  
  .welcome-title {
    font-size: 0.9em;
  }
}

@media (min-width: 769px) and (max-width: 1200px) {
  .login-container {
    width: 90%;
  }
  
  .login-form-container {
    width: clamp(280px, 40%, 400px) !important;
  }
}
</style>
