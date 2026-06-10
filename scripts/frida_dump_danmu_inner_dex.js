Java.perform(function () {
  const File = Java.use("java.io.File");
  const FileInputStream = Java.use("java.io.FileInputStream");
  const FileOutputStream = Java.use("java.io.FileOutputStream");
  const DexClassLoader = Java.use("dalvik.system.DexClassLoader");
  const Class = Java.use("java.lang.Class");

  function copyFile(src, dst) {
    const input = FileInputStream.$new(src);
    const output = FileOutputStream.$new(dst);
    const buffer = Java.array("byte", Array(8192).fill(0));
    let count = 0;
    try {
      while ((count = input.read(buffer)) > 0) output.write(buffer, 0, count);
      output.flush();
      console.log("[dump] copied " + src + " -> " + dst);
    } finally {
      input.close();
      output.close();
    }
  }

  function dumpDex(dexPath) {
    if (dexPath.indexOf("/secure_dex/inner_") < 0) return;
    const dexFile = File.$new(dexPath);
    const cacheDir = dexFile.getParentFile().getParentFile().getAbsolutePath();
    const dst = cacheDir + "/danmu_inner_" + Date.now() + ".dex";
    try {
      copyFile(dexPath, dst);
    } catch (e) {
      console.log("[dump] failed: " + e);
    }
  }

  const dexInit = DexClassLoader.$init.overload(
    "java.lang.String",
    "java.lang.String",
    "java.lang.String",
    "java.lang.ClassLoader"
  );
  dexInit.implementation = function (dexPath, optimizedDirectory, librarySearchPath, parent) {
    console.log("[dex] " + dexPath);
    dumpDex(dexPath);
    return dexInit.call(this, dexPath, optimizedDirectory, librarySearchPath, parent);
  };

  const fileDelete = File.delete.overload();
  fileDelete.implementation = function () {
    const path = this.getAbsolutePath();
    if (path.indexOf("/secure_dex/inner_") >= 0) {
      console.log("[keep] skip delete " + path);
      return false;
    }
    return fileDelete.call(this);
  };

  const getDeclaredField = Class.getDeclaredField.overload("java.lang.String");
  getDeclaredField.implementation = function (name) {
    const className = this.getName();
    if (className.indexOf("com.fongmi.android.tv") >= 0) {
      console.log("[reflect-field] " + className + " -> " + name);
    }
    return getDeclaredField.call(this, name);
  };

  const getDeclaredMethod = Class.getDeclaredMethod.overload("java.lang.String", "[Ljava.lang.Class;");
  getDeclaredMethod.implementation = function (name, types) {
    const className = this.getName();
    if (className.indexOf("com.fongmi.android.tv") >= 0) {
      console.log("[reflect-method] " + className + " -> " + name);
    }
    return getDeclaredMethod.call(this, name, types);
  };

  console.log("[ready] danmu inner dex dumper installed");
});
