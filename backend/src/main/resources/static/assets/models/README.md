# three.js 模型接入说明

前端页面已默认加载 `/scripts/smart-room.js`，并按以下顺序尝试接入 3D 模型：

1. `import("/assets/models/model-adapter.js")`
2. 如果 `model-adapter.js` 导出 `mountMeetingRoomModel`，则优先使用适配器接管页面中的 three.js 场景
3. 如果 `model-adapter.js` 导出 `modelUrl`，则尝试加载对应的 `.glb/.gltf`
4. 如果以上都没有，则回退到程序化生成的降级会议室场景

## 适配器接口

```js
export async function mountMeetingRoomModel(context) {
  const { THREE, sceneGroup, camera, controls, onHotspotSelect } = context;

  // 清空默认降级场景
  sceneGroup.clear();

  // 在这里挂载你自己的模型
  // ...

  return {
    update(appState) {
      // 根据页面联动状态更新模型材质、灯光、动画
    }
  };
}
```

## 直接加载模型文件

如果只想直接加载 `glb`，可以提供：

```js
export const modelUrl = "/assets/models/meeting-room.glb";
```

推荐把模型文件放在：

- `/static/assets/models/meeting-room.glb`
- `/static/assets/models/model-adapter.js`
