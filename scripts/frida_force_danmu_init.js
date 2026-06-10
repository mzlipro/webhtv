Java.perform(function () {
  const App = Java.use("com.fongmi.android.tv.App");
  const Build = Java.use("android.os.Build");
  const DexClassLoader = Java.use("dalvik.system.DexClassLoader");
  const System = Java.use("java.lang.System");
  const ctx = App.get();
  const jarPath = "/data/data/com.fongmi.android.tv/cache/jar/abe41ce8e8691ca7ec00b28ed01e7f29.jar";
  const cachePath = "/data/data/com.fongmi.android.tv/cache/jar";
  const SPOOF_ARM_ABI = false;
  let targetLoader = null;

  try {
    console.log("[force] original abis: " + Build.SUPPORTED_ABIS.value);
    if (SPOOF_ARM_ABI) {
      Build.SUPPORTED_ABIS.value = Java.array("java.lang.String", ["arm64-v8a", "armeabi-v7a"]);
      console.log("[force] patched abis: " + Build.SUPPORTED_ABIS.value);
    }
  } catch (e) {
    console.log("[force] patch abi failed: " + e);
  }

  const systemLoad = System.load.overload("java.lang.String");
  systemLoad.implementation = function (path) {
    console.log("[system-load] " + path);
    try {
      const result = systemLoad.call(this, path);
      console.log("[system-load] ok");
      return result;
    } catch (e) {
      console.log("[system-load] failed: " + e);
      throw e;
    }
  };

  const systemLoadLibrary = System.loadLibrary.overload("java.lang.String");
  systemLoadLibrary.implementation = function (name) {
    console.log("[system-loadLibrary] " + name);
    try {
      const result = systemLoadLibrary.call(this, name);
      console.log("[system-loadLibrary] ok");
      return result;
    } catch (e) {
      console.log("[system-loadLibrary] failed: " + e);
      throw e;
    }
  };

  Java.enumerateClassLoaders({
    onMatch: function (loader) {
      if (targetLoader) return;
      try {
        loader.loadClass("com.github.catvod.spider.SecureDanmu");
        targetLoader = loader;
        console.log("[force] found loader: " + loader);
      } catch (e) {
      }
    },
    onComplete: function () {
      if (!targetLoader) {
        try {
          targetLoader = DexClassLoader.$new(jarPath, cachePath, cachePath, ctx.getClassLoader());
          console.log("[force] created loader: " + targetLoader);
          targetLoader.loadClass("com.github.catvod.spider.SecureDanmu");
        } catch (e) {
          console.log("[force] target loader not available: " + e);
          return;
        }
      }

      Java.classFactory.loader = targetLoader;
      try {
        const Init = Java.use("com.github.catvod.spider.Init");
        Init.init(ctx);
        console.log("[force] Init.init(App.get()) called");
      } catch (e) {
        console.log("[force] Init.init failed: " + e);
      }

      try {
        const SecureDanmu = Java.use("com.github.catvod.spider.SecureDanmu");
        console.log("[force] SecureDanmu inner class: " + SecureDanmu.a());
        console.log("[force] SecureDanmu singleton: " + SecureDanmu.b());
      } catch (e) {
        console.log("[force] SecureDanmu check failed: " + e);
      }
    },
  });
});
